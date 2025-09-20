package com.quick.browser.llm

import com.google.gson.annotations.SerializedName

data class DefaultConfig(
  @SerializedName("topK") val topK: Int?,
  @SerializedName("topP") val topP: Float?,
  @SerializedName("temperature") val temperature: Float?,
  @SerializedName("accelerators") val accelerators: String?,
  @SerializedName("maxTokens") val maxTokens: Int?,
)

/** A model in the model allowlist. */
data class AllowedModel(
  val name: String,
  val modelId: String,
  val modelFile: String,
  val description: String,
  val sizeInBytes: Long,
  val version: String,
  val defaultConfig: DefaultConfig,
  val taskTypes: List<String>,
  val disabled: Boolean? = null,
  val llmSupportImage: Boolean? = null,
  val llmSupportAudio: Boolean? = null,
  val minDeviceMemoryInGb: Int? = null,
  val bestForTaskTypes: List<String>? = null,
  val localModelFilePathOverride: String? = null,
) {
  fun toModel(): Model {
    // Construct HF download url.
    val downloadUrl = "https://huggingface.co/$modelId/resolve/main/$modelFile?download=true"

    // Config.
    val isLlmModel =
      taskTypes.contains(BuiltInTaskId.LLM_CHAT) ||
        taskTypes.contains(BuiltInTaskId.LLM_PROMPT_LAB)
    var configs: List<Config> = listOf()
    if (isLlmModel) {
      val defaultTopK: Int = defaultConfig.topK ?: DEFAULT_TOPK
      val defaultTopP: Float = defaultConfig.topP ?: DEFAULT_TOPP
      val defaultTemperature: Float = defaultConfig.temperature ?: DEFAULT_TEMPERATURE
      val defaultMaxToken = defaultConfig.maxTokens ?: 1024
      var accelerators: List<Accelerator> = DEFAULT_ACCELERATORS
      if (defaultConfig.accelerators != null) {
        val items = defaultConfig.accelerators.split(",")
        accelerators = mutableListOf()
        for (item in items) {
          if (item == "cpu") {
            accelerators.add(Accelerator.CPU)
          } else if (item == "gpu") {
            accelerators.add(Accelerator.GPU)
          }
        }
      }
      configs =
        createLlmChatConfigs(
          defaultTopK = defaultTopK,
          defaultTopP = defaultTopP,
          defaultTemperature = defaultTemperature,
          defaultMaxToken = defaultMaxToken,
          accelerators = accelerators,
        )
    }

    // Misc.
    var showBenchmarkButton = true
    var showRunAgainButton = true
    if (isLlmModel) {
      showBenchmarkButton = false
      showRunAgainButton = false
    }

    return Model(
      name = name,
      version = version,
      info = description,
      url = downloadUrl,
      sizeInBytes = sizeInBytes,
      minDeviceMemoryInGb = minDeviceMemoryInGb,
      configs = configs,
      downloadFileName = modelFile,
      showBenchmarkButton = showBenchmarkButton,
      showRunAgainButton = showRunAgainButton,
      learnMoreUrl = "https://huggingface.co/${modelId}",
      llmSupportImage = llmSupportImage == true,
      llmSupportAudio = llmSupportAudio == true,
      bestForTaskIds = bestForTaskTypes ?: listOf(),
      localModelFilePathOverride = localModelFilePathOverride ?: "",
    )
  }

  override fun toString(): String {
    return "$modelId/$modelFile"
  }
}

/** The model allowlist. */
data class ModelAllowlist(val models: List<AllowedModel>)