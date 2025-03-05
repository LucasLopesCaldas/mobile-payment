package com.lupus.mobilepayment.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun QrCodeView(text: String, size: Int = 256, modifier: Modifier = Modifier) {
    Box(modifier) {
        var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }

        LaunchedEffect(text) {
            qrCodeBitmap = QrCode(text).bitmap()
        }

        qrCodeBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code",
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.None,
                modifier = Modifier
                    .size(size.dp)
                    .align(Alignment.Center)
                    .clip(shape = RoundedCornerShape(16.dp))
            )
        } ?: run {
            Box(modifier = Modifier.size(size.dp))
        }
    }
}

@Composable
fun QrCodeReader(onQrCodeScanned: (String?) -> Unit) {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraAvailable by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = decodeBitmapFromUri(context, it)
            bitmap?.let { bmp ->
                decodeQrFromBitmap(bmp, onQrCodeScanned)
            }
        }
    }

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                cameraAvailable = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
            }, ContextCompat.getMainExecutor(context))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission && cameraAvailable) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().apply {
                        surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                        processImageProxy(imageProxy, onQrCodeScanned)
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            ctx as LifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )
                    } catch (e: Exception) {
                        println("Error binding use cases")
                    }

                    previewView
                },
                onRelease = {
                    cameraProviderFuture.get().unbindAll()
                }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (!hasCameraPermission) "Camera permission required"
                    else "No camera found",
                    fontSize = 18.sp
                )
            }
        }

        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Load QR from Image")
        }
    }
}


private fun decodeBitmapFromUri(context: Context, uri: android.net.Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun decodeQrFromBitmap(bitmap: Bitmap, onQrCodeScanned: (String?) -> Unit) {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val source: LuminanceSource = RGBLuminanceSource(width, height, pixels)
    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

    try {
        val result = QRCodeReader().decode(binaryBitmap)
        if (result.text.isNotEmpty()) {
            Log.i("QrCodeReader", "QR Code detected: ${result.text}")
            onQrCodeScanned(result.text)
        }
    } catch (e: Exception) {
        Log.e("QrCodeReader", "Error decoding QR Code", e)
        onQrCodeScanned(null)
    }
}

class QrCode(private val text: String, val size: Int = 0) {
    fun bitmap(): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun processImageProxy(imageProxy: ImageProxy, onQrCodeScanned: (String) -> Unit) {
    val buffer = imageProxy.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    val width = imageProxy.width
    val height = imageProxy.height
    val pixels = IntArray(width * height)

    for (i in pixels.indices) {
        val y = bytes[i].toInt() and 0xFF
        pixels[i] = -0x1000000 or (y shl 16) or (y shl 8) or y
    }

    val source: LuminanceSource = RGBLuminanceSource(width, height, pixels)
    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

    try {
        val result = QRCodeReader().decode(binaryBitmap)
        if (result.text.isNotEmpty()) {
            Log.i("QrCodeReader", "QR Code detected: ${result.text}")
            onQrCodeScanned(result.text)
        }
    } catch (_: Exception) {
        //
    } finally {
        imageProxy.close()
    }
}
