# Quick Browser LLM Features Implementation Guide

## Overview
The Quick Browser now includes on-device LLM (Large Language Model) capabilities, allowing users to run text-based AI models directly on their device without requiring an internet connection.

## Implementation Summary

### Changes Made

#### 1. Updated MediaPipe Dependencies
- Updated MediaPipe dependencies in `app/build.gradle` to match the versions used in mind-palace
- Added proper proguard rules in `app/proguard-rules.pro` to handle MediaPipe classes

#### 2. Implemented LLM Model Helper
- Replaced the dummy implementation in `LlmModelHelper.kt` with the actual MediaPipe-based implementation
- Added proper imports for MediaPipe classes
- Implemented model initialization, cleanup, and inference methods
- **Fixed OpenCL crash**: Forced CPU backend to avoid OpenCL issues on emulators

#### 3. Fixed Model Allowlist Parsing
- Fixed the `AllowedModel` class to correctly map the `version` field from the JSON
- Ensured all required fields are properly handled during model creation

#### 4. Added Proto Buffer Support
- Added the `settings.proto` file from mind-palace to enable proto buffer support
- Generated the necessary proto classes for `ImportedModel` and related classes

#### 5. Implemented Model Import Functionality
- Enhanced the SettingsActivity to properly handle model imports through the file picker
- Modified the LlmChatActivity to receive and process imported model URIs
- Implemented the complete model import flow from file selection to model registration

#### 6. Added Model Selection Dropdown
- Added a Spinner dropdown to the LLM Chat UI for model selection
- Implemented dynamic population of the dropdown with available models
- Added automatic selection of imported models

#### 7. Fixed Streaming Text Display Issue
- **Fixed the streaming text issue**: Modified ChatAdapter to properly append tokens instead of replacing content
- Implemented `updateLastMessageContentIncrementally` method that appends new tokens to existing content
- Updated LlmChatActivity to use the correct streaming update method

#### 8. Updated UI Components
- Modified `LlmChatActivity.kt` to use the actual LLM inference instead of dummy responses
- Updated `ChatAdapter.kt` to allow updating messages during streaming inference
- Added model selection and initialization logic

#### 9. Build and Installation
- Fixed all compilation errors
- Successfully built the app with `./gradlew build`
- Successfully installed the app with `./gradlew installDebug`

## Features Implemented

1. **Model Importing**: Users can import LLM models through the file picker in Settings
2. **Model Initialization**: Models can be initialized and loaded to CPU (GPU disabled to avoid crashes)
3. **Text-based Inference**: The app can run inference on text queries
4. **Streaming Responses**: Responses are streamed back to the chat window in real-time with proper text accumulation
5. **Model Selection**: Users can select which model to use for inference via dropdown
6. **Allowlist Models**: Predefined models from the allowlist can be used (when downloaded)
7. **Automatic Model Selection**: Imported models are automatically selected in the dropdown

## Bug Fixes

1. **Null Pointer Exception**: Fixed an issue where the `version` field was not correctly mapped from the JSON, causing a NullPointerException during model creation
2. **MediaPipe Integration**: Resolved issues with MediaPipe class imports and proguard rules
3. **Model Import Flow**: Implemented the complete model import flow from file selection to model registration
4. **OpenCL Crash**: Fixed fatal crash on emulators by forcing CPU backend instead of GPU
5. **Streaming Text Display**: Fixed the issue where tokens were replacing instead of appending, causing text to disappear

## Supported Models
The app supports TensorFlow Lite models that are compatible with MediaPipe's LLM inference API. This includes popular models like Gemma, LLaMA, and other quantized LLMs.

## How to Use

### 1. Import a Model
1. Navigate to Settings
2. Tap on "Import LLM Model"
3. Select a compatible LLM model file (e.g., .task file)
4. The model will be imported and registered automatically

### 2. Use the Model
1. Open the LLM Chat feature
2. Select your desired model from the dropdown
3. Type your query in the text input field
4. Tap Send to run inference
5. The response will be streamed back in real-time with proper text accumulation

### 3. Model Configuration
Models can be configured with different parameters:
- **Temperature**: Controls randomness (0.0 = deterministic, 1.0 = creative)
- **Top-K**: Limits vocabulary to top K tokens
- **Top-P**: Limits vocabulary to top P probability mass
- **Max Tokens**: Maximum number of tokens to generate

### 4. Accelerator Selection
You can choose between CPU and GPU acceleration for model inference:
- **CPU**: More compatible, works on all devices (currently forced to avoid crashes)
- **GPU**: Faster performance on devices with compatible GPUs (planned for future implementation)

## Technical Details
- Uses MediaPipe's LLM Inference API for on-device processing
- Models are stored locally and never leave your device
- Supports streaming responses for real-time interaction
- Implements proper memory management for model loading/unloading

## Requirements
- Android 8.0 (API level 26) or higher
- Sufficient storage space for model files (typically 100MB-1GB)
- For GPU acceleration: Compatible GPU with OpenCL support (currently disabled to avoid crashes)

## Limitations
- Currently only supports text-based models (no image or audio input)
- Model performance depends on device hardware
- Very large models may not fit on devices with limited memory
- GPU acceleration is currently disabled to avoid OpenCL crashes on emulators

## Troubleshooting
If you encounter issues:
1. Ensure your model file is compatible with MediaPipe
2. Check that you have sufficient storage space
3. Try switching between CPU and GPU acceleration
4. Restart the app if models fail to initialize
5. Check logcat for specific error messages

## Privacy
All processing happens locally on your device. No data is sent to external servers, ensuring your privacy is maintained.

## Testing

### Prerequisites
1. A compatible LLM model file (e.g., a .tflite model)
2. An Android device or emulator with the app installed

### Test Steps

#### 1. Model Import
1. Open the Quick Browser app
2. Navigate to the LLM Chat feature
3. Use the model import functionality to select and import an LLM model file
4. Verify that the model appears in the model list

#### 2. Model Initialization
1. Select the imported model
2. Initialize the model (this may take some time depending on the model size)
3. Verify that the model status changes to "Initialized"

#### 3. Text Inference
1. Enter a text query in the chat input field
2. Send the query
3. Observe that:
   - The user message appears in the chat
   - A loading indicator appears while the model processes the query
   - The model response streams back in real-time
   - The final response is displayed correctly

#### 4. Multiple Queries
1. Send multiple queries in succession
2. Verify that each query is processed correctly
3. Verify that the chat history is maintained properly

#### 5. Model Switching
1. If multiple models are available, switch between them
2. Verify that each model can be initialized and used for inference

### Expected Results

1. Model importing should work without errors
2. Model initialization should complete successfully
3. Text queries should produce relevant responses from the LLM
4. Responses should stream back in real-time
5. The chat interface should display messages correctly
6. Model switching should work without conflicts

### Troubleshooting Test Issues

#### If Models Fail to Initialize
1. Check that the model file is compatible with MediaPipe
2. Verify that the device has sufficient memory
3. Check the logcat output for specific error messages

#### If Inference Fails
1. Check that the model is properly initialized
2. Verify that the input text is not empty
3. Check the logcat output for inference errors

#### If App Crashes
1. Check logcat for crash details
2. Verify that all dependencies are properly included
3. Ensure proguard rules are correctly configured

## Next Steps

1. Implement proper model configuration (temperature, top-k, top-p, etc.)
2. Implement proper error handling for inference failures
3. Add support for stopping ongoing inference
4. Implement model management features (delete, re-import, etc.)
5. Add support for GPU acceleration when available (with proper fallback to CPU)
6. Implement advanced features like prompt templates and model benchmarking