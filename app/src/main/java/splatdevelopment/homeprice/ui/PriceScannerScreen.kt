package splatdevelopment.homeprice.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import splatdevelopment.homeprice.R
import splatdevelopment.homeprice.analyzer.PriceAnalyzer
import splatdevelopment.homeprice.analyzer.PriceTagDetector
import splatdevelopment.homeprice.domain.ConverterController
import splatdevelopment.homeprice.domain.DetectedPrice
import splatdevelopment.homeprice.domain.ScanMode

@Composable
fun PriceScannerScreen(
    controller: ConverterController,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val state by controller.state.collectAsState()
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    val previewView = remember { PreviewView(context) }
    // Still image shown while a price is selected, so highlights stay aligned with what the user tapped
    var frozenFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    // In preview pixels, which match this screen's Box because the preview fills it
    var aimBox by remember { mutableStateOf<Rect?>(null) }
    // Trial: tags found by the on-device detector, shown for evaluation only
    var tagDetections by remember { mutableStateOf<PriceTagDetector.Result?>(null) }

    DisposableEffect(Unit) {
        onDispose { controller.resumeScanning() }
    }

    // Unfreeze whenever the selection is cleared (Scan Again, or switching modes)
    LaunchedEffect(state.selectedPriceId) {
        if (state.selectedPriceId == null) frozenFrame = null
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .onSizeChanged { aimBox = aimBoxRect(it.toSize()) }
        ) {
            if (hasCameraPermission) {
                CameraPreview(
                    previewView = previewView,
                    onFrameAnalyzed = { prices, detections ->
                        // Keep the tags that were on screen when the user froze the frame
                        if (state.selectedPriceId == null) tagDetections = detections
                        val box = aimBox ?: return@CameraPreview
                        controller.updateDetectedPrices(
                            PriceAnalyzer.selectPrices(
                                prices = prices,
                                aimBox = box,
                                tags = detections?.tags.orEmpty(),
                                largestPerTag = state.scanMode == ScanMode.Auto,
                            )
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                frozenFrame?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                ScannerOverlay(
                    state = state,
                    onSelectPrice = { id ->
                        if (frozenFrame == null) frozenFrame = previewView.bitmap?.asImageBitmap()
                        controller.selectPrice(id)
                    },
                    onScanModeChange = controller::setScanMode,
                    onResumeScanning = controller::resumeScanning,
                    tagDetections = tagDetections,
                )
            } else {
                Text(
                    text = stringResource(R.string.scanner_camera_permission),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                if (state.selectedPriceId != null) {
                    FilledTonalButton(onClick = controller::resumeScanning) {
                        Text(stringResource(R.string.scanner_scan_again))
                    }
                }
                Button(onClick = onClose) {
                    Text(stringResource(R.string.scanner_back))
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    previewView: PreviewView,
    onFrameAnalyzed: (prices: List<DetectedPrice>, tags: PriceTagDetector.Result?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnFrameAnalyzed by rememberUpdatedState(onFrameAnalyzed)

    DisposableEffect(lifecycleOwner) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val tagDetector = PriceTagDetector(context)
        val executor = ContextCompat.getMainExecutor(context)
        val cameraController = LifecycleCameraController(context)

        cameraController.setImageAnalysisAnalyzer(
            executor,
            // View-referenced coordinates put OCR boxes in PreviewView pixels, accounting for crop and rotation
            HalfRateAnalyzer(
                MlKitAnalyzer(listOf(recognizer, tagDetector), ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED, executor) { result ->
                    result.getThrowable(recognizer)?.let { Log.e("PriceScanner", "Text recognition failed", it) }
                    result.getThrowable(tagDetector)?.let { Log.e("PriceScanner", "Tag detection failed", it) }
                    val text = result.getValue(recognizer) ?: return@MlKitAnalyzer
                    currentOnFrameAnalyzed(PriceAnalyzer.detectPrices(text), result.getValue(tagDetector))
                }
            ),
        )
        cameraController.bindToLifecycle(lifecycleOwner)
        previewView.controller = cameraController

        onDispose {
            cameraController.unbind()
            recognizer.close()
            tagDetector.close()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/**
 * Halves how often [delegate] runs by idling for as long as each analysis took.
 *
 * With KEEP_ONLY_LATEST backpressure, the first frame after an analysis arrives right when
 * the previous one finished, so the time since it started is how long OCR took.
 */
private class HalfRateAnalyzer(private val delegate: ImageAnalysis.Analyzer) : ImageAnalysis.Analyzer {
    private var analysisStartedAt = 0L
    private var resumeAt = 0L
    private var isAnalyzing = false

    override fun analyze(image: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (isAnalyzing) {
            isAnalyzing = false
            resumeAt = now + (now - analysisStartedAt)
        }
        if (now < resumeAt) {
            image.close()
            return
        }
        isAnalyzing = true
        analysisStartedAt = now
        delegate.analyze(image)
    }

    // Forward these so MlKitAnalyzer still gets view-referenced coordinates
    override fun getDefaultTargetResolution(): Size? = delegate.defaultTargetResolution
    override fun getTargetCoordinateSystem(): Int = delegate.targetCoordinateSystem
    override fun updateTransform(matrix: Matrix?) = delegate.updateTransform(matrix)
}
