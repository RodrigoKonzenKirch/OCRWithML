package com.example.ocrwithml

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.rememberAsyncImagePainter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

private const val TAG = "AppScreens"

/**
 * Main Composable function that handles navigation between screens.
 * This is the primary OCR selector logic implemented in Compose.
 */
@Composable
fun AppScreens(currentScreen: Screen, onNavigate: (Screen) -> Unit) {
    when (currentScreen) {
        Screen.MENU -> MenuScreen(onNavigate = onNavigate)
        Screen.LIVE_OCR -> LiveOcrScreen(onBack = { onNavigate(Screen.MENU) })
        Screen.STILL_OCR -> StillOcrScreen(onBack = { onNavigate(Screen.MENU) })
    }
}

// --- 1. Menu Screen (OCR Selector) ---

@Composable
fun MenuScreen(onNavigate: (Screen) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Choose OCR Mode",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select whether you want real-time analysis or analyze a still image.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Live OCR Card
        MenuCard(
            title = "Live Camera OCR",
            description = "Real-time text extraction from the live camera feed.",
            icon = Icons.Filled.CameraAlt,
            onClick = { onNavigate(Screen.LIVE_OCR) }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Still OCR Card
        MenuCard(
            title = "Still Image OCR",
            description = "Analyze text from a photo you take or upload.",
            icon = Icons.Filled.FileUpload,
            onClick = { onNavigate(Screen.STILL_OCR) }
        )
    }
}

@Composable
fun MenuCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onClick) {
                Text("Select")
            }
        }
    }
}

// --- 2. Live Camera OCR Screen (Previous OcrScreen) ---

@Composable
fun LiveOcrScreen(onBack: () -> Unit) {
    var recognizedText by remember { mutableStateOf("Point the camera at some text...") }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar for Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Menu")
            }
            Text(
                text = "Live Camera Analysis",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Camera Preview Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            CameraPreview(
                onTextDetected = { text ->
                    recognizedText = text
                }
            )

            // Text display card on top of the camera
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),

                ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Detected Text:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = recognizedText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Composable that displays the CameraX live preview and starts the ML Kit Text Analyzer.
 */
@Composable
fun CameraPreview(onTextDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // AndroidView is used to embed a traditional Android View (PreviewView) into Compose
    AndroidView(
        factory = {
            PreviewView(context).apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                // Use the theme's background color
                setBackgroundColor(Color(0xFFFFFBFE).hashCode())
            }
        },
        update = { previewView ->
            bindCameraUseCases(
                context,
                lifecycleOwner,
                previewView,
                onTextDetected
            )
        }
    )

    // Unbind camera when the composable leaves the screen
    DisposableEffect(lifecycleOwner) {
        onDispose {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                // IMPORTANT: Unbind all use cases when leaving the screen
                cameraProvider.unbindAll()
            }, ContextCompat.getMainExecutor(context))
        }
    }
}

/**
 * Binds the CameraX Preview and ImageAnalysis use cases to the lifecycle.
 */
private fun bindCameraUseCases(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onTextDetected: (String) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    val cameraProvider = cameraProviderFuture.get()

    // 1. Preview Use Case
    val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
    }

    // 2. Image Analysis Use Case (for OCR)
    val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also {
            it.setAnalyzer(
                Executors.newSingleThreadExecutor(),
                TextAnalyzer(onTextDetected)
            )
        }

    cameraProvider.unbindAll()

    try {
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalysis
        )
    } catch (exc: Exception) {
        Log.e(TAG, "Live OCR use case binding failed", exc)
    }
}

/**
 * Analyzer class that runs the ML Kit Text Recognizer on incoming ImageProxy frames.
 */
class TextAnalyzer(private val onTextDetected: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastAnalyzedTimestamp = 0L
    private val frameIntervalMs = 500 // Analyze every 500ms (2 FPS)

    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimestamp < frameIntervalMs) {
            imageProxy.close()
            return // Skip frame analysis
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    if (visionText.text.isNotEmpty()) {
                        onTextDetected(visionText.text)
                    }
                    lastAnalyzedTimestamp = currentTimestamp
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Live Text recognition failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}

// --- 3. Still Image OCR Screen (Placeholder for future implementation) ---

@Composable
fun StillOcrScreen(onBack: () -> Unit) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var recognizedText by remember { mutableStateOf("Select an image to analyze its text.") }
    var isAnalyzing by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // Launcher for selecting an image from the device's storage
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            imageUri = uri
            recognizedText = if (uri != null) "Image selected. Press 'Analyze' to begin." else "No image selected."
            isAnalyzing = false
        }
    )

    // ML Kit analysis
    val startAnalysis = {
        if (imageUri != null) {
            isAnalyzing = true
            recognizedText = "Analyzing image..."

            try {
                // 1. Create InputImage from the file path (Uri)
                val image = InputImage.fromFilePath(context, imageUri!!)

                // 2. Start the recognition process
                textRecognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        recognizedText = if (visionText.text.isBlank()) {
                            "No text detected in the image."
                        } else {
                            visionText.text
                        }
                        isAnalyzing = false
                    }
                    .addOnFailureListener { e ->
                        recognizedText = "Analysis failed: ${e.localizedMessage ?: "Unknown error"}"
                        Log.e(TAG, "Still image OCR failed", e)
                        isAnalyzing = false
                    }
            } catch (e: Exception) {
                recognizedText = "Error loading image: ${e.localizedMessage}"
                Log.e(TAG, "Error creating InputImage from Uri", e)
                isAnalyzing = false
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Menu")
            }
            Text(
                text = "Still Image Analysis",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Image Preview Area
        if (imageUri != null) {
            Image(
                painter = rememberAsyncImagePainter(model = imageUri),
                contentDescription = "Selected image preview",
                modifier = Modifier
                    .weight(1f) // Fill available space
                    .fillMaxWidth()
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Select an image file to see a preview.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                Text("Select Image")
            }

            Button(
                onClick = startAnalysis,
                enabled = imageUri != null && !isAnalyzing
            ) {
                Text("Analyze")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

       // Result Area (Scrollable to accommodate long text)
        Text(
            text = "OCR Result:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = recognizedText,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
