# Article Summarization Feature

## Overview
This feature adds AI-powered article summarization to the Bubble Browser. It uses a small language model (less than 100MB) to summarize web articles into 5-10 bullet points.

## Implementation Details

### UI Components
1. **GenAI Icon**: Added to the WebView toolbar next to the reader mode icon
2. **Summary Dialog**: Displays the summarized bullet points with options to share

### Backend Components
1. **SummarizationManager**: Handles loading/unloading the ML model and performing summarization
2. **ModelDownloader**: Downloads the ML model when needed and manages its lifecycle
3. **SummaryDialog**: UI component for displaying the summarized content

### Key Features
1. **On-demand Model Loading**: The ML model is only loaded when needed and unloaded after use to conserve memory
2. **Extractive Summarization**: The current implementation uses a simple extractive approach that can be replaced with a proper ML model
3. **Bullet Point Format**: Results are presented as 5-10 easy-to-read bullet points
4. **Sharing**: Users can share the summarized content

## How It Works
1. User clicks the GenAI icon in the toolbar
2. The app extracts the HTML content from the current webpage
3. The SummarizationManager loads the ML model (if not already loaded)
4. The content is processed and summarized
5. Results are displayed in a dialog as bullet points
6. The model is unloaded after use to free up memory

## Model Requirements
- TensorFlow Lite model for text summarization
- Size less than 100MB
- Optimized for mobile devices
- Capable of extractive or abstractive summarization

## Recommended Models
1. **MobileBERT** (~25MB) - Good for extractive summarization
2. **DistilBART** (~60MB) - Good for abstractive summarization
3. **BART-mini** (~40MB) - Good for abstractive summarization
4. **TinyBERT** (~15MB) - Good for extractive summarization

## Next Steps
1. Implement or acquire a proper ML model for summarization
2. Fine-tune the model for better performance on news articles
3. Add caching for frequently summarized pages
4. Implement offline summarization for saved pages
5. Add user feedback mechanism to improve summarization quality