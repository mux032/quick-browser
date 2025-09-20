package com.quick.browser.llm

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "LlmModelManagerViewModel"

data class ModelInitializationStatus(
  val status: ModelInitializationStatusType,
  var error: String = "",
)

enum class ModelInitializationStatusType {
  NOT_INITIALIZED,
  INITIALIZING,
  INITIALIZED,
  ERROR,
}

data class LlmModelManagerUiState(
  /** A list of models available in the application. */
  val models: List<Model>,

  /** A map that tracks the download status of each model, indexed by model name. */
  val modelDownloadStatus: Map<String, ModelDownloadStatus>,

  /** A map that tracks the initialization status of each model, indexed by model name. */
  val modelInitializationStatus: Map<String, ModelInitializationStatus>,

  /** Whether the app is loading and processing the model allowlist. */
  val loadingModelAllowlist: Boolean = true,

  /** The error message when loading the model allowlist. */
  val loadingModelAllowlistError: String = "",

  /** The currently selected model. */
  val selectedModel: Model = EMPTY_MODEL,

  /** The history of text inputs entered by the user. */
  val textInputHistory: List<String> = listOf(),
  val configValuesUpdateTrigger: Long = 0L,
) {
  fun isModelInitialized(model: Model): Boolean {
    return modelInitializationStatus[model.name]?.status ==
      ModelInitializationStatusType.INITIALIZED
  }

  fun isModelInitializing(model: Model): Boolean {
    return modelInitializationStatus[model.name]?.status ==
      ModelInitializationStatusType.INITIALIZING
  }
}

@HiltViewModel
class LlmModelManagerViewModel
@Inject
constructor(
  @ApplicationContext private val context: Context,
) : ViewModel() {
  private val externalFilesDir = context.getExternalFilesDir(null)
  private val _uiState = MutableStateFlow(createEmptyUiState())
  val uiState = _uiState.asStateFlow()

  fun selectModel(model: Model) {
    _uiState.update { _uiState.value.copy(selectedModel = model) }
  }

  fun initializeModel(context: Context, model: Model, force: Boolean = false) {
    viewModelScope.launch(Dispatchers.Default) {
      // Skip if initialized already.
      if (
        !force &&
          uiState.value.modelInitializationStatus[model.name]?.status ==
            ModelInitializationStatusType.INITIALIZED
      ) {
        Log.d(TAG, "Model '${model.name}' has been initialized. Skipping.")
        return@launch
      }

      // Skip if initialization is in progress.
      if (model.initializing) {
        model.cleanUpAfterInit = false
        Log.d(TAG, "Model '${model.name}' is being initialized. Skipping.")
        return@launch
      }

      // Clean up.
      cleanupModel(context = context, model = model)

      // Start initialization.
      Log.d(TAG, "Initializing model '${model.name}'...")
      model.initializing = true

      val onDone: (error: String) -> Unit = { error ->
        model.initializing = false
        if (model.instance != null) {
          Log.d(TAG, "Model '${model.name}' initialized successfully")
          updateModelInitializationStatus(
            model = model,
            status = ModelInitializationStatusType.INITIALIZED,
          )
          if (model.cleanUpAfterInit) {
            Log.d(TAG, "Model '${model.name}' needs cleaning up after init.")
            cleanupModel(context = context, model = model)
          }
        } else if (error.isNotEmpty()) {
          Log.d(TAG, "Model '${model.name}' failed to initialize")
          updateModelInitializationStatus(
            model = model,
            status = ModelInitializationStatusType.ERROR,
            error = error,
          )
        }
      }

      // Call the model initialization function.
      LlmModelHelper.initialize(
        context = context,
        model = model,
        supportImage = model.llmSupportImage,
        supportAudio = model.llmSupportAudio,
        onDone = onDone,
      )
    }
  }

  fun cleanupModel(context: Context, model: Model) {
    if (model.instance != null) {
      model.cleanUpAfterInit = false
      Log.d(TAG, "Cleaning up model '${model.name}'...")
      val onDone: () -> Unit = {
        model.instance = null
        model.initializing = false
        updateModelInitializationStatus(
          model = model,
          status = ModelInitializationStatusType.NOT_INITIALIZED,
        )
        Log.d(TAG, "Clean up model '${model.name}' done")
      }
      LlmModelHelper.cleanUp(model = model, onDone = onDone)
    } else {
      // When model is being initialized and we are trying to clean it up at same time, we mark it
      // to clean up and it will be cleaned up after initialization is done.
      if (model.initializing) {
        Log.d(
          TAG,
          "Model '${model.name}' is still initializing.. Will clean up after it is done initializing",
        )
        model.cleanUpAfterInit = true
      }
    }
  }

  fun loadModelAllowlist() {
    _uiState.update {
      uiState.value.copy(loadingModelAllowlist = true, loadingModelAllowlistError = "")
    }

    viewModelScope.launch(Dispatchers.IO) {
      try {
        // Load model allowlist json.
        var modelAllowlist: ModelAllowlist? = null

        // Try to read from assets first.
        Log.d(TAG, "Loading model allowlist from assets.")
        modelAllowlist = readModelAllowlistFromAssets()

        if (modelAllowlist == null) {
          _uiState.update {
            uiState.value.copy(loadingModelAllowlistError = "Failed to load model list")
          }
          return@launch
        }

        Log.d(TAG, "Allowlist: $modelAllowlist")

        // Convert models in the allowlist.
        val models = mutableListOf<Model>()
        for (allowedModel in modelAllowlist.models) {
          if (allowedModel.disabled == true) {
            continue
          }

          val model = allowedModel.toModel()
          models.add(model)
        }

        // Process all models.
        for (model in models) {
          model.preProcess()
        }

        // Load imported models and add them to the list
        val importedModels = loadImportedModels()
        val allModels = models + importedModels

        // Update UI state.
        _uiState.update { createUiState().copy(loadingModelAllowlist = false, models = allModels) }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  private fun loadImportedModels(): List<Model> {
    val importedModels = mutableListOf<Model>()
    try {
      val importsDir = File(externalFilesDir, IMPORTS_DIR)
      if (importsDir.exists()) {
        val importedFiles = importsDir.listFiles()
        if (importedFiles != null) {
          for (file in importedFiles) {
            if (file.isFile) {
              val model = Model(
                name = file.name,
                url = "",
                sizeInBytes = file.length(),
                downloadFileName = "$IMPORTS_DIR/${file.name}",
                imported = true,
                configs = createLlmChatConfigs(),
                showBenchmarkButton = false,
                showRunAgainButton = false,
              )
              model.preProcess()
              importedModels.add(model)
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to load imported models", e)
    }
    return importedModels
  }

  fun clearLoadModelAllowlistError() {
    _uiState.update {
      createUiState()
        .copy(loadingModelAllowlist = false, loadingModelAllowlistError = "")
    }
  }

  fun importModelFromUri(uri: Uri, onDone: (Model?, String?) -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        // Get file name and size
        val fileName = getFileNameFromUri(context, uri) ?: "imported_model.task"
        val fileSize = getFileSizeFromUri(context, uri)

        // Create imports directory if it doesn't exist
        val importsDir = File(externalFilesDir, IMPORTS_DIR)
        if (!importsDir.exists()) {
          importsDir.mkdirs()
        }

        // Copy file to imports directory
        val outputFile = File(importsDir, fileName)
        val inputStream = context.contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(outputFile)
        
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()

        // Create a model for the imported file
        val model = Model(
          name = fileName,
          url = "",
          sizeInBytes = fileSize,
          downloadFileName = "$IMPORTS_DIR/$fileName",
          imported = true,
          configs = createLlmChatConfigs(),
          showBenchmarkButton = false,
          showRunAgainButton = false,
        )
        model.preProcess()

        // Add to UI state
        val currentModels = _uiState.value.models.toMutableList()
        currentModels.add(model)
        
        _uiState.update { 
          _uiState.value.copy(models = currentModels) 
        }

        onDone(model, null)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to import model", e)
        onDone(null, e.message)
      }
    }
  }

  private fun readModelAllowlistFromAssets(): ModelAllowlist? {
    try {
      Log.d(TAG, "Reading model allowlist from assets")
      val inputStream = context.assets.open("model_allowlist.json")
      val content = inputStream.bufferedReader().use { it.readText() }
      Log.d(TAG, "Model allowlist content from assets: $content")

      val gson = Gson()
      return gson.fromJson(content, ModelAllowlist::class.java)
    } catch (e: Exception) {
      Log.e(TAG, "failed to read model allowlist from assets", e)
      return null
    }
  }

  private fun createEmptyUiState(): LlmModelManagerUiState {
    return LlmModelManagerUiState(
      models = listOf(),
      modelDownloadStatus = mapOf(),
      modelInitializationStatus = mapOf(),
    )
  }

  private fun createUiState(): LlmModelManagerUiState {
    val modelDownloadStatus: MutableMap<String, ModelDownloadStatus> = mutableMapOf()
    val modelInstances: MutableMap<String, ModelInitializationStatus> = mutableMapOf()
    val models = mutableListOf<Model>()

    // Load imported models.
    val importsDir = File(externalFilesDir, IMPORTS_DIR)
    if (importsDir.exists()) {
      val importedFiles = importsDir.listFiles()
      if (importedFiles != null) {
        for (file in importedFiles) {
          if (file.isFile) {
            val model = Model(
              name = file.name,
              url = "",
              sizeInBytes = file.length(),
              downloadFileName = "$IMPORTS_DIR/${file.name}",
              imported = true,
              configs = createLlmChatConfigs(),
              showBenchmarkButton = false,
              showRunAgainButton = false,
            )
            model.preProcess()
            models.add(model)

            // Update status.
            modelDownloadStatus[model.name] =
              ModelDownloadStatus(
                status = ModelDownloadStatusType.SUCCEEDED,
                receivedBytes = file.length(),
                totalBytes = file.length(),
              )
          }
        }
      }
    }

    Log.d(TAG, "model download status: $modelDownloadStatus")
    return LlmModelManagerUiState(
      models = models,
      modelDownloadStatus = modelDownloadStatus,
      modelInitializationStatus = modelInstances,
    )
  }

  private fun updateModelInitializationStatus(
    model: Model,
    status: ModelInitializationStatusType,
    error: String = "",
  ) {
    val curModelInstance = uiState.value.modelInitializationStatus.toMutableMap()
    curModelInstance[model.name] = ModelInitializationStatus(status = status, error = error)
    val newUiState = uiState.value.copy(modelInitializationStatus = curModelInstance)
    _uiState.update { newUiState }
  }

  private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    if (uri.scheme == "content") {
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
          if (nameIndex != -1) {
            return cursor.getString(nameIndex)
          }
        }
      }
    } else if (uri.scheme == "file") {
      return uri.lastPathSegment
    }
    return null
  }

  private fun getFileSizeFromUri(context: Context, uri: Uri): Long {
    if (uri.scheme == "content") {
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
          if (sizeIndex != -1) {
            return cursor.getLong(sizeIndex)
          }
        }
      }
    }
    return 0L
  }
}