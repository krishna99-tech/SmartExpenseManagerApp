package com.expensemanager.ui.components

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.expensemanager.R
import com.expensemanager.ui.theme.DsSpacing
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun LiveBillScanner(
    onClose: () -> Unit,
    onTextDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val processing = remember { AtomicBoolean(false) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    var lastPreview by remember { mutableStateOf("") }
    var lastEmitMs by remember { mutableLongStateOf(0L) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(Unit) {
        onDispose {
            recognizer.close()
            cameraExecutor.shutdown()
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                    .padding(DsSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.live_scan_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, contentDescription = null)
                }
            }

            if (!hasPermission) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(DsSpacing.lg),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(stringResource(R.string.camera_permission_required))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.padding(top = DsSpacing.md),
                    ) {
                        Text(stringResource(R.string.grant_permission))
                    }
                }
            } else {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.weight(1f),
                    update = { view ->
                        bindCamera(
                            activity = context as FragmentActivity,
                            lifecycleOwner = lifecycleOwner,
                            previewView = view,
                            cameraExecutor = cameraExecutor,
                            recognizer = recognizer,
                            processing = processing,
                            onText = { text ->
                                lastPreview = text
                                val now = System.currentTimeMillis()
                                if (now - lastEmitMs > 1200) {
                                    lastEmitMs = now
                                    onTextDetected(text)
                                }
                            },
                        )
                    },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        .padding(DsSpacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (lastPreview.isBlank()) {
                            stringResource(R.string.live_scan_hint)
                        } else {
                            lastPreview.take(48)
                        },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = {
                        if (lastPreview.isNotBlank()) onTextDetected(lastPreview)
                    }) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                        Text(
                            text = stringResource(R.string.apply_scan),
                            modifier = Modifier.padding(start = DsSpacing.xs),
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun bindCamera(
    activity: FragmentActivity,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    cameraExecutor: java.util.concurrent.ExecutorService,
    recognizer: com.google.mlkit.vision.text.TextRecognizer,
    processing: AtomicBoolean,
    onText: (String) -> Unit,
) {
    val providerFuture = ProcessCameraProvider.getInstance(activity)
    providerFuture.addListener({
        val cameraProvider = providerFuture.get()
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                return@setAnalyzer
            }
            if (!processing.compareAndSet(false, true)) {
                imageProxy.close()
                return@setAnalyzer
            }
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val txt = result.text.trim()
                    if (txt.isNotBlank()) onText(txt)
                }
                .addOnCompleteListener {
                    processing.set(false)
                    imageProxy.close()
                }
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analysis)
    }, ContextCompat.getMainExecutor(activity))
}
