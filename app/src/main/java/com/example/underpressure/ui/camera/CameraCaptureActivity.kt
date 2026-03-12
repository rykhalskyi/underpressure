package com.example.underpressure.ui.camera

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.underpressure.R
import com.example.underpressure.ocr.BloodPressureOcrManager
import com.example.underpressure.ocr.OcrParser
import com.example.underpressure.ocr.OcrResult
import com.example.underpressure.ui.theme.UnderPressureTheme
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraCaptureActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private val ocrManager = BloodPressureOcrManager()
    private val ocrParser = OcrParser()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            UnderPressureTheme {
                CameraScreen(
                    onClose = { finish() }
                )
            }
        }
    }

    @Composable
    private fun CameraScreen(
        onClose: () -> Unit
    ) {
        var isProcessing by remember { mutableStateOf(false) }
        var debugText by remember { mutableStateOf<String?>(null) }
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (exc: Exception) {
                            Log.e("CameraCapture", "Use case binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Visual Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    val strokeWidth = 4.dp.toPx()
                    val cornerLength = 40.dp.toPx()
                    val color = Color.White.copy(alpha = 0.5f)
                    
                    // Top-left
                    drawLine(color, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)
                    drawLine(color, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth)
                    
                    // Top-right
                    drawLine(color, Offset(size.width, 0f), Offset(size.width - cornerLength, 0f), strokeWidth)
                    drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerLength), strokeWidth)
                    
                    // Bottom-left
                    drawLine(color, Offset(0f, size.height), Offset(cornerLength, size.height), strokeWidth)
                    drawLine(color, Offset(0f, size.height), Offset(0f, size.height - cornerLength), strokeWidth)
                    
                    // Bottom-right
                    drawLine(color, Offset(size.width, size.height), Offset(size.width - cornerLength, size.height), strokeWidth)
                    drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - cornerLength), strokeWidth)
                }
                
                Text(
                    text = "Align screen inside frame",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                )
            }

            if (isProcessing) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.ocr_processing),
                        modifier = Modifier.padding(top = 80.dp),
                        color = Color.White
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 32.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = onClose) {
                            Text(stringResource(R.string.button_cancel))
                        }
                        Button(onClick = {
                            isProcessing = true
                            takePhoto { result ->
                                isProcessing = false
                                if (result != null) {
                                    setResultAndFinish(result.toFormattedString())
                                } else {
                                    Toast.makeText(context, R.string.ocr_error_failed, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Text(stringResource(R.string.camera_capture_button))
                        }
                    }
                }
            }

            // Debug Dialog (simplified as we now get OcrResult)
            debugText?.let { text ->
                AlertDialog(
                    onDismissRequest = { debugText = null },
                    title = { Text("OCR Debug Info") },
                    text = {
                        Column {
                            Text("Raw text recognized by ML Kit:", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(8.dp))
                            val scrollState = rememberScrollState()
                            Text(
                                text = text.ifBlank { "[No text detected]" },
                                modifier = Modifier
                                    .heightIn(max = 300.dp)
                                    .verticalScroll(scrollState)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(8.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { debugText = null }) { Text("Try Again") }
                    }
                )
            }
        }
    }

    private fun takePhoto(onResult: (OcrResult?) -> Unit) {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()
                    if (bitmap != null) {
                        lifecycleScope.launch {
                            val result = ocrManager.recognize(bitmap)
                            onResult(result)
                        }
                    } else {
                        onResult(null)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraCapture", "Photo capture failed: ${exception.message}", exception)
                    onResult(null)
                }
            }
        )
    }

    private fun imageProxyToBitmap(image: androidx.camera.core.ImageProxy): Bitmap? {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun setResultAndFinish(ocrResult: String?) {
        if (ocrResult != null) {
            val intent = Intent().apply {
                putExtra("ocr_result", ocrResult)
            }
            setResult(RESULT_OK, intent)
        } else {
            setResult(RESULT_CANCELED)
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
