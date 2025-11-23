📸 Still & Live OCR Companion (Android/Compose)

This is a modern Android application built using Jetpack Compose and Google ML Kit that provides two distinct modes for Optical Character Recognition (OCR): real-time text extraction from a live camera feed, and analysis of static images selected from the device gallery.

✨ Features

- Dual Mode OCR: Seamlessly switch between Live Camera Analysis (real-time text detection) and Still Image Analysis (upload/select a photo).

- Real-time Feedback: Instantly see detected text blocks while pointing your camera at documents, signs, or screens (Live Mode).

- Image Selection: Easily pick images from your device gallery for deep-dive text analysis (Still Mode).

💻 Technology Stack

- Primary Language: Kotlin

- UI Toolkit: Jetpack Compose (Modern Android UI)

- Vision/OCR: Google ML Kit Text Recognition (Latin Script)

- Camera: CameraX (Jetpack library for easy camera integration)

- Image Loading: Coil (for asynchronous image loading and caching)


🚀 How to Run Locally

Setup

1. Prerequisites: Android Studio and an Android API 21+ device/emulator are required.

2. Clone: git clone https://github.com/RodrigoKonzenKirch/OCRWithML.git

3. Run: Open the project in Android Studio and click the 'Run' button.

4. Permissions: This application requires the CAMERA permission to perform live OCR.

⚙️ Core Implementation Details

1. Live OCR (LiveOcrScreen)

The live analysis is handled using the following components:

- CameraPreview Composable: Embeds a PreviewView using AndroidView.

- CameraX: Binds Preview and ImageAnalysis use cases to the component's lifecycle.

- TextAnalyzer Class: This custom ImageAnalysis.Analyzer is executed on a background thread. It converts incoming ImageProxy frames into InputImage objects and passes them to the TextRecognizer.

- Throttling: To maintain performance, the analyzer processes frames at a limited rate (e.g., 2 FPS), skipping intermediate frames.

2. Still OCR (StillOcrScreen)

The still image analysis follows a clear, sequential process:

- Image Selection: Uses rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) to open the device gallery and retrieve the image Uri.

- Analysis Trigger: The "Analyze" button calls the startAnalysis function.

- ML Kit Integration: The image Uri is converted into an InputImage using InputImage.fromFilePath(context, imageUri).

- Asynchronous Processing: The textRecognizer.process(image) call runs asynchronously.

- Result Display: The extracted text is displayed in a dedicated, scrollable text area upon completion.

🤝 Contributing

We welcome contributions! If you find a bug or have an idea for an enhancement, please open an issue or submit a pull request.

Fork the repository.

Create a new feature branch (git checkout -b feature/AmazingFeature).

Commit your changes (git commit -m 'Add AmazingFeature').

Push to the branch (git push origin feature/AmazingFeature).

Open a Pull Request.

📝 License

This project is licensed under the MIT License - see the LICENSE.md file for details.
