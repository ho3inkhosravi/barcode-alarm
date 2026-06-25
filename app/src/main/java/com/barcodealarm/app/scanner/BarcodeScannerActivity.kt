package com.barcodealarm.app.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import kotlin.math.abs

class BarcodeScannerActivity : ComponentActivity() {

    private var expectedBarcode: String = ""
    private var onResultCallback: ((Boolean, String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        expectedBarcode = intent.getStringExtra("expected_barcode") ?: ""
        val mode = intent.getStringExtra("mode") ?: "scan"

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
            return
        }

        setContent {
            BarcodeScannerScreen(
                mode = mode,
                expectedBarcode = expectedBarcode,
                onBarcodeScanned = { barcode ->
                    val resultIntent = Intent().apply {
                        putExtra("barcode_value", barcode)
                        putExtra("match", barcode == expectedBarcode)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                },
                onBack = { finish() }
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recreate()
            } else {
                Toast.makeText(this, "دسترسی به دوربین نیاز است", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}

@Composable
fun BarcodeScannerScreen(
    mode: String,
    expectedBarcode: String,
    onBarcodeScanned: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var barcodeDetected by remember { mutableStateOf<String?>(null) }
    var hasScanned by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    val executor = Executors.newSingleThreadExecutor()

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            if (!hasScanned) {
                                processImage(imageProxy) { barcode ->
                                    if (barcode != null) {
                                        hasScanned = true
                                        barcodeDetected = barcode
                                        onBarcodeScanned(barcode)
                                    }
                                }
                            } else {
                                imageProxy.close()
                            }
                        }

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                (ctx as androidx.lifecycle.LifecycleOwner),
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            }
        )

        // Overlay UI
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
                }
                Text(
                    text = if (mode == "alarm") "اسکن بارکد برای قطع آلارم" else "اسکن بارکد محصول",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Scan frame
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(3.dp, Color.White, RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (expectedBarcode.isNotEmpty()) {
                        Text(
                            text = "بارکد مورد انتظار: $expectedBarcode",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    } else {
                        Text(
                            text = "بارکد را در این کادر قرار دهید",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Flash effect on detection
        if (barcodeDetected != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    }
}

private fun processImage(imageProxy: ImageProxy, onResult: (String?) -> Unit) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    val barcode = barcodes[0].rawValue
                    onResult(barcode)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { onResult(null) }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}
