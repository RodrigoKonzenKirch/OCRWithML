package com.example.ocrwithml

import android.content.Context
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

// Tag for logging
private const val TAG = "RealTimeOcr"

@Composable
fun OcrScreen() {
    var recognizedText by remember { mutableStateOf("Point the camera at some text...") }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Camera Preview Area
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
                setBackgroundColor(Color.Black.hashCode())
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
    val executor = ContextCompat.getMainExecutor(context)
    val cameraProvider = cameraProviderFuture.get()

    // 1. Preview Use Case
    val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
    }

    // 2. Image Analysis Use Case (for OCR)
    val imageAnalysis = ImageAnalysis.Builder()
        // Use STRATEGY_KEEP_ONLY_LATEST to avoid backpressure from the slow ML model processing
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also {
            it.setAnalyzer(
                // We use a dedicated single-threaded executor for image analysis
                Executors.newSingleThreadExecutor(),
                TextAnalyzer(onTextDetected)
            )
        }

    // Unbind all existing use cases before binding new ones
    cameraProvider.unbindAll()

    try {
        // Bind the selected use cases to the lifecycle
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalysis
        )
    } catch (exc: Exception) {
        Log.e(TAG, "Use case binding failed", exc)
    }
}

/**
 * Analyzer class that runs the ML Kit Text Recognizer on incoming ImageProxy frames.
 */
class TextAnalyzer(private val onTextDetected: (String) -> Unit) : ImageAnalysis.Analyzer {

    // Initialize ML Kit Recognizer
    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Simple rate limiter to prevent analyzing every single frame (improves performance)
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
            // Create InputImage from the CameraX ImageProxy
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // Pass the recognized text back to the Composable
                    if (visionText.text.isNotEmpty()) {
                        onTextDetected(visionText.text)
                    }
                    lastAnalyzedTimestamp = currentTimestamp
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Text recognition failed", e)
                }
                .addOnCompleteListener {
                    // CRITICAL: Close the ImageProxy to release the buffer for the next frame
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}