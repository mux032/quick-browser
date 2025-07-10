# OpenNLP Models

This directory should contain the following OpenNLP model files:

1. `en-sent.bin` - English sentence detector model

You can download these models from the Apache OpenNLP website:
https://opennlp.apache.org/models.html

## Model Details

### en-sent.bin
- Purpose: Detects sentence boundaries in English text
- Size: ~100KB
- Download: https://opennlp.sourceforge.net/models-1.5/en-sent.bin

## Usage

These models are used by the SummarizationManager to process text for article summarization.
If the models are not available, the app will fall back to using regex-based approaches.

## Installation

1. Download the models from the links above
2. Place them in this directory (app/src/main/assets/)
3. Rebuild the app

## Fallback Mechanism

If the models are not available, the app will use the following fallback mechanisms:

1. For sentence detection: Regular expression pattern `(?<=[.!?])\\s+`