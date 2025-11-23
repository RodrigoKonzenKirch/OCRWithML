📸 Still & Live OCR Companion (Android/Compose)

This is a modern Android application built using Jetpack Compose and Google ML Kit that provides two distinct modes for Optical Character Recognition (OCR): real-time text extraction from a live camera feed, and analysis of static images selected from the device gallery.

The app is designed for speed, efficiency, and a clean, responsive user experience.

✨ Features

- Dual Mode OCR: Seamlessly switch between Live Camera Analysis (real-time text detection) and Still Image Analysis (upload/select a photo).

- Real-time Feedback: Instantly see detected text blocks while pointing your camera at documents, signs, or screens (Live Mode).

- Image Selection: Easily pick images from your device gallery for deep-dive text analysis (Still Mode).

- Built on Modern Android Stack: Utilizes Kotlin, Jetpack Compose, CameraX, and the highly optimized Google ML Kit for vision tasks.

💻 Technology Stack

- Primary Language: Kotlin

- UI Toolkit: Jetpack Compose (Modern Android UI)

- Vision/OCR: Google ML Kit Text Recognition (Latin Script)

- Camera: CameraX (Jetpack library for easy camera integration)

- Image Loading: Coil (for asynchronous image loading and caching)


🚀 How to Run Locally

Prerequisites

Android Studio (Latest version recommended)

A physical Android device or emulator running Android API 21+ with Google Play Services.

Steps:

1.Clone the Repository:

    git clone https://github.com/RodrigoKonzenKirch/OCRWithML.git

2.Open in Android Studio:

Open the cloned directory in Android Studio.

3.Build and Run:

Select your target device (emulator or physical phone) and click the green "Run" button.

⚠️ Permissions Note

This application requires the CAMERA permission to function. On first launch, the app will automatically request this permission. If denied, the app will prompt the user to enable it manually for OCR features to work.

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
