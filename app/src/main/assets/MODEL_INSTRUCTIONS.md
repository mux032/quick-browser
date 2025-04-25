# Article Summarization Model Instructions

## Model Requirements
- TensorFlow Lite model for text summarization
- Size less than 100MB
- Optimized for mobile devices
- Capable of extractive or abstractive summarization

## Recommended Models
1. **MobileBERT** - A compressed version of BERT optimized for mobile devices
   - Size: ~25MB
   - Good for extractive summarization
   - [GitHub Repository](https://github.com/google-research/google-research/tree/master/mobilebert)

2. **DistilBART** - A distilled version of BART for summarization
   - Size: ~60MB
   - Good for abstractive summarization
   - [Hugging Face Model](https://huggingface.co/sshleifer/distilbart-cnn-12-6)

3. **BART-mini** - A smaller version of BART
   - Size: ~40MB
   - Good for abstractive summarization
   - [Hugging Face Model](https://huggingface.co/facebook/bart-large-cnn)

4. **TinyBERT** - A smaller and faster version of BERT
   - Size: ~15MB
   - Good for extractive summarization
   - [GitHub Repository](https://github.com/huawei-noah/Pretrained-Language-Model/tree/master/TinyBERT)

## How to Add the Model

1. Convert your chosen model to TensorFlow Lite format using the TensorFlow Lite Converter
   ```python
   import tensorflow as tf
   
   # Load your model
   model = tf.keras.models.load_model('your_model.h5')
   
   # Convert the model
   converter = tf.lite.TFLiteConverter.from_keras_model(model)
   tflite_model = converter.convert()
   
   # Save the model
   with open('summarization_model.tflite', 'wb') as f:
     f.write(tflite_model)
   ```

2. For Hugging Face models, you can use the optimum library:
   ```bash
   pip install optimum[exporters]
   python -m optimum.exporters.tflite --model distilbart-cnn-12-6 --output summarization_model
   ```

3. Place the converted model file (`summarization_model.tflite`) in the app's assets directory.

4. Update the `ModelDownloader.kt` file with the URL to your hosted model if you want to download it at runtime.

## Implementation Notes

- The current implementation uses a placeholder extractive summarization approach
- Replace the placeholder with actual model inference in the `runModelInference` method in `SummarizationManager.kt`
- The model is loaded on demand and unloaded after use to minimize memory usage
- Consider implementing a caching mechanism for frequently summarized pages

## Testing Your Model

1. Test with various article types (news, blogs, technical articles)
2. Measure summarization quality
3. Monitor memory usage during summarization
4. Check summarization speed on different device tiers