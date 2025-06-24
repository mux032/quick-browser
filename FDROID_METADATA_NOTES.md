# Notes for F-Droid Metadata File

This document outlines specific configurations and information needed when creating the F-Droid metadata file for the QB Browser application.

## General Information

*   **License:** GPLv3 (A `LICENSE` file has been added to the repository root). F-Droid metadata should specify `GPL-3.0-or-later`.
*   **Source Code:** The metadata should point to the application's source code repository.
*   **Build System:** Standard Gradle build. No special pre-build steps seem necessary beyond what's standard for Android.

## Key Build & Configuration Points

1.  **TensorFlow Lite Native Libraries (`libtensorflowlite_jni.so`):**
    *   These files are **not** included directly in the source repository but are pulled in as part of a Gradle dependency.
    *   The exact dependency providing these was not explicitly declared in `app/build.gradle` (e.g., no direct `implementation 'org.tensorflow:tensorflow-lite:...'`). This might be a transitive dependency.
    *   **Action for F-Droid Maintainer:**
        *   The F-Droid build recipe needs to ensure that these `.so` files are either built from source by F-Droid's TensorFlow Lite recipe or that F-Droid provides compatible versions.
        *   The `packagingOptions` in `app/build.gradle` include `pickFirst` directives for these `.so` files. These are intended to resolve conflicts if multiple dependencies provide them. These can likely remain, but F-Droid maintainers may have specific guidance if their build system handles this differently.
        *   It's important to identify the version of TensorFlow Lite being used implicitly to match it with F-Droid's available versions. Running `./gradlew :app:dependencies` would help identify this.

2.  **OpenNLP Sentence Model (`en-sent.bin`):**
    *   This model is used by the `SummarizationManager` for sentence detection.
    *   The file `en-sent.bin` has been **removed** from `app/src/main/assets/`.
    *   **Action for F-Droid Maintainer:**
        *   The F-Droid build recipe **must** download `en-sent.bin` and place it into the `app/src/main/assets/` directory before the Gradle build.
        *   Download URL: `https://opennlp.sourceforge.net/models-1.5/en-sent.bin`
        *   The application code (`SummarizationManager.kt`) loads this file directly from assets.
        *   *Note:* `ModelDownloader.kt` also contains logic to download this file to external storage or copy it from assets. This functionality becomes somewhat redundant if F-Droid ensures the asset is present but is not harmful.

3.  **Summarization Model (`summarization_model.tflite`):**
    *   The file `summarization_model.tflite` (and its accompanying `MODEL_INSTRUCTIONS.md` and `README.txt`) have been **removed** from `app/src/main/assets/`.
    *   **Reason:** The application's current summarization logic (`SummarizationManager.kt`) **does not use TensorFlow Lite or this model.** It uses OpenNLP and a custom heuristic algorithm.
    *   **Action for F-Droid Maintainer:** No specific action needed for a TFLite summarization model, as it's not implemented.

4.  **Ad Blocking (`AdBlocker.kt`, `AdBlockUpdateService.kt`):**
    *   The `app/src/main/assets/hosts.txt` file has been **removed** as it was unused.
    *   Ad blocking rules are initialized with a small, hardcoded default list in `AdBlocker.kt`.
    *   If ad blocking is enabled by the user (checked via `settingsManager.isAdBlockEnabled()`), the `AdBlockUpdateService` will periodically download updated rules from:
        *   `EASYLIST_URL = "https://easylist.to/easylist/easylist.txt"`
    *   EasyList is a reputable, community-maintained ad-blocking list.
    *   **Action for F-Droid Maintainer:** This behavior is generally acceptable. Ensure the app description mentions the ad-blocking feature and its reliance on EasyList if active.

5.  **Permissions:**
    *   `SYSTEM_ALERT_WINDOW`:
        *   **Justification:** This permission is used for the "Bubble" feature, which allows the browser to display floating bubbles over other apps. This is a core UI feature of the application.
    *   `usesCleartextTraffic="true"` (in `AndroidManifest.xml`):
        *   **Justification:** As a web browser, the application must be able to access HTTP URLs in addition to HTTPS. While HTTPS is preferred and likely defaulted to for user-entered URLs where appropriate, the browser cannot restrict itself to HTTPS only for all web content.

6.  **No Known Proprietary Dependencies or Anti-Features:**
    *   Based on the review, the app does not seem to include proprietary SDKs for analytics, ads (other than the ad blocker itself), or other "anti-features" as defined by F-Droid, once the prebuilt models were removed/addressed.

## Summary of Files to be Handled by F-Droid Build Recipe

*   **`en-sent.bin`**: Download from `https://opennlp.sourceforge.net/models-1.5/en-sent.bin` and place in `app/src/main/assets/`.
*   **`libtensorflowlite_jni.so` (for various ABIs):** Ensure these are provided by F-Droid's build system or built from source, matching the version implicitly used by the app.

This information should help in creating an accurate F-Droid metadata file.
The app developers might want to refactor `ModelDownloader.kt` in the future to avoid redundancy if `en-sent.bin` is always provided in assets by the build system.Tool output for `create_file_with_block`:
