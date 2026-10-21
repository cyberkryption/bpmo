package com.cyberkryption.vitals.ui.capture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyberkryption.vitals.R
import com.cyberkryption.vitals.core.Field
import com.cyberkryption.vitals.core.ReadingType
import com.cyberkryption.vitals.extraction.FrameReader
import com.cyberkryption.vitals.extraction.GuideFrame
import com.cyberkryption.vitals.ui.components.IconAction
import com.cyberkryption.vitals.ui.components.SegmentedColors
import com.cyberkryption.vitals.ui.components.Segmented
import com.cyberkryption.vitals.ui.findActivity
import com.cyberkryption.vitals.ui.theme.CaptureColors
import com.cyberkryption.vitals.ui.theme.VitalsType
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/** Always dark: camera preview, a guide frame for the display, live detection and the shutter. */
@Composable
fun CaptureScreen(
    onClose: () -> Unit,
    onGallery: () -> Unit,
    onCaptured: (sessionId: String) -> Unit,
    viewModel: CaptureViewModel = viewModel(factory = CaptureViewModel.Factory),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var asked by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        asked = true
    }
    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }
    LightSystemBars()

    val fg = CaptureColors.foreground
    val shutterLabel = stringResource(R.string.capture_shutter)
    var shutter: (() -> Unit)? by remember { mutableStateOf(null) }
    val guide = remember { GuideTracker() }

    Box(Modifier.fillMaxSize().background(CaptureColors.background)) {
        if (granted) {
            CameraPreview(
                viewModel = viewModel,
                torch = state.torch,
                guide = guide,
                onCaptured = onCaptured,
                registerShutter = { shutter = it },
            )
        }

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconAction(R.drawable.ic_x, stringResource(R.string.action_close), onClose, tint = fg)
                Spacer(Modifier.weight(1f))
                IconAction(
                    R.drawable.ic_zap,
                    stringResource(if (state.torch) R.string.capture_torch_off else R.string.capture_torch_on),
                    viewModel::toggleTorch,
                    iconSize = 22.dp,
                    tint = if (state.torch) CaptureColors.guide else fg,
                    enabled = granted,
                )
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                Segmented(
                    options = listOf(
                        ReadingType.BP to stringResource(R.string.capture_mode_bp),
                        ReadingType.OXIMETER to stringResource(R.string.type_oximeter),
                    ),
                    selected = state.mode,
                    onSelect = viewModel::setMode,
                    colors = SegmentedColors(
                        track = Color.White.copy(alpha = 0.12f),
                        selected = CaptureColors.guide,
                        selectedText = CaptureColors.onGuide,
                        text = fg,
                    ),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .widthIn(max = 300.dp)
                        .fillMaxWidth()
                        .aspectRatio(if (state.mode == ReadingType.BP) 3f / 4f else 16f / 9f)
                        .border(2.5.dp, CaptureColors.guide, RoundedCornerShape(28.dp))
                        .onGloballyPositioned { guide.guideAt(it.boundsInRoot()) },
                )
                if (!granted) {
                    PermissionPrompt(asked = asked, onRequest = { permission.launch(Manifest.permission.CAMERA) })
                } else {
                    DetectionChip(state)
                    Text(
                        stringResource(if (state.failed) R.string.capture_failed else R.string.capture_hint),
                        style = VitalsType.control,
                        color = fg.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(CaptureColors.background.copy(alpha = 0.85f))
                    .navigationBarsPadding()
                    .padding(start = 28.dp, top = 20.dp, end = 28.dp, bottom = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable(role = Role.Button, onClick = onGallery),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.ic_image), stringResource(R.string.capture_gallery), tint = fg, modifier = Modifier.size(22.dp))
                    }
                }
                Box(
                    Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .border(3.dp, fg, CircleShape)
                        .clickable(enabled = granted && !state.capturing, role = Role.Button, onClick = { shutter?.invoke() })
                        .semantics { contentDescription = shutterLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(62.dp).clip(CircleShape).background(CaptureColors.guide.copy(alpha = if (state.capturing) 0.5f else 1f)))
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    val autoState = stringResource(if (state.autoCapture) R.string.capture_auto_on else R.string.capture_auto_off)
                    Text(
                        stringResource(if (state.autoCapture) R.string.capture_auto else R.string.capture_manual),
                        style = VitalsType.control,
                        color = if (state.autoCapture) CaptureColors.onGuide else fg,
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(if (state.autoCapture) CaptureColors.guide else Color.White.copy(alpha = 0.12f))
                            .clickable(role = Role.Switch, onClick = viewModel::toggleAuto)
                            .semantics { stateDescription = autoState }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

/** Where the guide frame sits over the camera preview, kept for the analysis thread. */
private class GuideTracker {
    val frame = AtomicReference<GuideFrame?>(null)
    private var preview: Rect? = null
    private var guide: Rect? = null

    fun previewAt(bounds: Rect) {
        preview = bounds
        publish()
    }

    fun guideAt(bounds: Rect) {
        guide = bounds
        publish()
    }

    private fun publish() {
        val p = preview ?: return
        val g = guide ?: return
        val inPreview = com.cyberkryption.vitals.core.Box(g.left - p.left, g.top - p.top, g.right - p.left, g.bottom - p.top)
        frame.set(GuideFrame(inPreview, p.width, p.height))
    }
}

@Composable
private fun CameraPreview(
    viewModel: CaptureViewModel,
    torch: Boolean,
    guide: GuideTracker,
    onCaptured: (String) -> Unit,
    registerShutter: (() -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCaptured by rememberUpdatedState(onCaptured)
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS)
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            // Preview and analysis share a 4:3 frame, so the guide frame on screen maps onto the
            // analysed frame; 1280 × 960 gives the digits enough pixels.
            previewResolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()
            imageAnalysisResolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    ResolutionStrategy(Size(1280, 960), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER),
                )
                .build()
        }
    }

    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }

    DisposableEffect(controller) {
        // Reading a frame takes tens of milliseconds, so it runs off the main thread; results come back to it.
        val analysis = Executors.newSingleThreadExecutor()
        val main = ContextCompat.getMainExecutor(context)
        val reader = FrameReader(guide.frame, { viewModel.state.value.mode }) { boxes ->
            main.execute { viewModel.onFrame(boxes) }
        }
        controller.setImageAnalysisAnalyzer(analysis, reader)
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            analysis.execute { reader.close() }
            analysis.shutdown()
        }
    }

    LaunchedEffect(torch) { controller.enableTorch(torch) }

    val takePicture: () -> Unit = {
        val file = viewModel.newPhotoFile()
        controller.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    currentOnCaptured(viewModel.onCaptured(file))
                }

                override fun onError(exception: ImageCaptureException) {
                    viewModel.onCaptureFailed(file)
                }
            },
        )
    }
    LaunchedEffect(Unit) {
        registerShutter(takePicture)
        viewModel.autoCaptureRequests.collect { if (!viewModel.state.value.capturing) takePicture() }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                this.controller = controller
            }
        },
        modifier = Modifier.fillMaxSize().onGloballyPositioned { guide.previewAt(it.boundsInRoot()) },
    )
}

@Composable
private fun DetectionChip(state: CaptureUiState) {
    val fg = CaptureColors.foreground
    val found = state.detected ?: return
    val d = found.detections
    val values = when (found.type) {
        ReadingType.BP -> "${d[Field.SYSTOLIC]?.value} / ${d[Field.DIASTOLIC]?.value} · ${d[Field.PULSE]?.value}"
        ReadingType.OXIMETER -> "${d[Field.SPO2]?.value}% · ${d[Field.PULSE]?.value}"
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(CaptureColors.background.copy(alpha = 0.75f))
            .border(1.dp, CaptureColors.guide.copy(alpha = 0.5f), RoundedCornerShape(percent = 50))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(CaptureColors.detected))
        Text(stringResource(R.string.capture_found, values), style = VitalsType.control.copy(fontFeatureSettings = "tnum"), color = fg)
    }
}

@Composable
private fun PermissionPrompt(asked: Boolean, onRequest: () -> Unit) {
    val context = LocalContext.current
    val fg = CaptureColors.foreground
    val activity = context.findActivity()
    val blocked = asked && activity != null &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.capture_permission), style = VitalsType.control, color = fg.copy(alpha = 0.75f), textAlign = TextAlign.Center)
        Box(
            Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(CaptureColors.guide)
                .clickable(role = Role.Button) {
                    if (blocked) {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                        )
                    } else {
                        onRequest()
                    }
                }
                .padding(horizontal = 22.dp, vertical = 12.dp),
        ) {
            Text(
                stringResource(if (blocked) R.string.capture_open_settings else R.string.capture_allow),
                style = VitalsType.button,
                color = CaptureColors.onGuide,
            )
        }
    }
}

/** Light status and navigation bar icons over the dark camera screen, restored on exit. */
@Composable
private fun LightSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val lightStatus = controller?.isAppearanceLightStatusBars
        val lightNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (controller != null) {
                lightStatus?.let { controller.isAppearanceLightStatusBars = it }
                lightNav?.let { controller.isAppearanceLightNavigationBars = it }
            }
        }
    }
}
