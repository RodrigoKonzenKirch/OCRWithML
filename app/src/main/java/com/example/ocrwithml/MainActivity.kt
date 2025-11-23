package com.example.ocrwithml

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ocrwithml.ui.theme.OCRWithMLTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OCRWithMLTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PermissionRequester(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequester(modifier: Modifier) {
    // Note: The manifest must contain <uses-permission android:name="android.permission.CAMERA" />
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var currentScreen by remember { mutableStateOf(Screen.MENU) }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            // Only launch the request if permission is not granted and we don't need to show a rationale (i.e., first time).
            if (!cameraPermissionState.status.shouldShowRationale) {
                cameraPermissionState.launchPermissionRequest()
            }
        }
    }

    if (cameraPermissionState.status.isGranted) {
        // Permission granted, show the OCR screen with the camera view
        AppScreens(currentScreen) { screen -> currentScreen = screen }
    } else {
        // Request permission on first launch or show explanation
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val statusText = if (cameraPermissionState.status.shouldShowRationale) {
                "Camera permission is required for OCR. Please enable it manually in your device settings."
            } else {
                "Waiting for camera permission..."
            }
            Text(
                statusText,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}