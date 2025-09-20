package com.quick.browser.llm

import androidx.compose.ui.unit.dp

/**
 * LLM-related constants, enums, and configuration values
 */
enum class Accelerator(val label: String) {
  CPU(label = "CPU"),
  GPU(label = "GPU"),
}

// Default values for LLM models.
const val DEFAULT_MAX_TOKEN = 1024
const val DEFAULT_TOPK = 40
const val DEFAULT_TOPP = 0.9f
const val DEFAULT_TEMPERATURE = 1.0f
val DEFAULT_ACCELERATORS = listOf(Accelerator.GPU)

// Keys used to send/receive data to Work.
const val KEY_MODEL_URL = "KEY_MODEL_URL"
const val KEY_MODEL_NAME = "KEY_MODEL_NAME"
const val KEY_MODEL_COMMIT_HASH = "KEY_MODEL_COMMIT_HASH"
const val KEY_MODEL_DOWNLOAD_MODEL_DIR = "KEY_MODEL_DOWNLOAD_MODEL_DIR"
const val KEY_MODEL_DOWNLOAD_FILE_NAME = "KEY_MODEL_DOWNLOAD_FILE_NAME"
const val KEY_MODEL_TOTAL_BYTES = "KEY_MODEL_TOTAL_BYTES"
const val KEY_MODEL_DOWNLOAD_RECEIVED_BYTES = "KEY_MODEL_DOWNLOAD_RECEIVED_BYTES"
const val KEY_MODEL_DOWNLOAD_RATE = "KEY_MODEL_DOWNLOAD_RATE"
const val KEY_MODEL_DOWNLOAD_REMAINING_MS = "KEY_MODEL_DOWNLOAD_REMAINING_SECONDS"
const val KEY_MODEL_DOWNLOAD_ERROR_MESSAGE = "KEY_MODEL_DOWNLOAD_ERROR_MESSAGE"
const val KEY_MODEL_DOWNLOAD_ACCESS_TOKEN = "KEY_MODEL_DOWNLOAD_ACCESS_TOKEN"
const val KEY_MODEL_EXTRA_DATA_URLS = "KEY_MODEL_EXTRA_DATA_URLS"
const val KEY_MODEL_EXTRA_DATA_DOWNLOAD_FILE_NAMES = "KEY_MODEL_EXTRA_DATA_DOWNLOAD_FILE_NAMES"
const val KEY_MODEL_IS_ZIP = "KEY_MODEL_IS_ZIP"
const val KEY_MODEL_UNZIPPED_DIR = "KEY_MODEL_UNZIPPED_DIR"
const val KEY_MODEL_START_UNZIPPING = "KEY_MODEL_START_UNZIPPING"

// Max number of images allowed in a "ask image" session.
const val MAX_IMAGE_COUNT = 10

// The size the icon shown under each of the model names in the model list screen.
val MODEL_INFO_ICON_SIZE = 18.dp

// The extension of the tmp download files.
const val TMP_FILE_EXT = "gallerytmp"

// Import directory
const val IMPORTS_DIR = "__imports"

// LLM Model Import Service constants
const val LLM_IMPORT_SERVICE_CHANNEL_ID = "llm_import_service_channel"
const val LLM_IMPORT_SERVICE_NOTIFICATION_ID = 1001

// Built-in task IDs
object BuiltInTaskId {
  const val LLM_CHAT = "llm_chat"
  const val LLM_PROMPT_LAB = "llm_prompt_lab"
}