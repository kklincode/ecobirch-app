package ru.ecobirch.fa.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import ru.ecobirch.fa.app.EcoBirchApplication
import ru.ecobirch.fa.app.domain.math.LeafBioindicationEngine
import ru.ecobirch.fa.app.domain.model.DetectionSource
import ru.ecobirch.fa.app.domain.model.Territory
import ru.ecobirch.fa.app.ui.components.LeafCalibrationCanvas
import ru.ecobirch.fa.app.ui.state.MainViewModel
import ru.ecobirch.fa.app.ui.state.ScanStage
import ru.ecobirch.fa.app.ui.state.ScanUiState
import ru.ecobirch.fa.app.ui.theme.EcoColors
import java.io.File

@Composable
fun ScanScreen(
    state: ScanUiState,
    territories: List<Territory>,
    viewModel: MainViewModel,
    onOpenJournal: () -> Unit,
    onOpenMap: () -> Unit
) {
    val context = LocalContext.current

    when (state.stage) {
        ScanStage.SELECT -> ScanSelectStage(
            state = state,
            territories = territories,
            viewModel = viewModel,
            context = context
        )
        ScanStage.PROCESSING -> ScanProcessingStage(state = state)
        ScanStage.ADJUST -> ScanAdjustStage(
            state = state,
            territories = territories,
            viewModel = viewModel,
            context = context
        )
        ScanStage.SAVED -> ScanSavedStage(
            state = state,
            territories = territories,
            viewModel = viewModel,
            context = context,
            onOpenJournal = onOpenJournal,
            onOpenMap = onOpenMap
        )
    }
}

@Composable
private fun ScanSelectStage(
    state: ScanUiState,
    territories: List<Territory>,
    viewModel: MainViewModel,
    context: Context
) {
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraBindFailed by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.handlePhotoUri(uri, "Галерея_${System.currentTimeMillis() % 10000}.jpg")
        }
    }

    var systemCaptureUri by remember { mutableStateOf<Uri?>(null) }
    val systemCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = systemCaptureUri
        if (success && uri != null) {
            viewModel.handlePhotoUri(uri, "Камера_${System.currentTimeMillis() % 10000}.jpg")
        }
    }

    fun launchSystemCamera() {
        val app = context.applicationContext as EcoBirchApplication
        val tmpFile = app.photoStorageManager.createTempCaptureFile()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tmpFile)
        systemCaptureUri = uri
        systemCameraLauncher.launch(uri)
    }

    val targetTerr = territories.find { it.id == state.targetTerritoryId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EcoColors.Emerald50,
                border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Emerald100)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = EcoColors.Emerald600,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Сканер листьев берёзы", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = EcoColors.Slate900)
                Text("Betula pendula · 12 морфологических точек", fontSize = 11.sp, color = EcoColors.Slate500)
            }
        }

        if (targetTerr != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EcoColors.Emerald50,
                border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Emerald600),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Активная точка сбора:", fontSize = 10.sp, color = EcoColors.Emerald700, fontWeight = FontWeight.Bold)
                        Text(targetTerr.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EcoColors.Slate900)
                        Text("Текущая выборка: n = ${targetTerr.leaves.size}", fontSize = 10.sp, color = EcoColors.Slate500)
                    }
                }
            }
        }

        // CameraX Live Viewfinder + Birch Silhouette Alignment Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3.4f)
                .clip(RoundedCornerShape(20.dp))
                .background(EcoColors.Slate950)
                .border(2.dp, EcoColors.Emerald600.copy(alpha = 0.45f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission && !cameraBindFailed) {
                CameraXPreviewBox(
                    onImageCaptureReady = { imageCapture = it },
                    onBindError = { cameraBindFailed = true }
                )
            }

            // Alignment Grid & Dotted Leaf Silhouette Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val gridColor = Color.White.copy(alpha = 0.14f)
                drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), 1.2f)
                drawLine(gridColor, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), 1.2f)
                drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), 1.2f)
                drawLine(gridColor, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), 1.2f)

                val leafPath = Path().apply {
                    moveTo(w * 0.50f, h * 0.80f)
                    cubicTo(w * 0.36f, h * 0.80f, w * 0.28f, h * 0.64f, w * 0.27f, h * 0.48f)
                    cubicTo(w * 0.26f, h * 0.33f, w * 0.38f, h * 0.24f, w * 0.50f, h * 0.16f)
                    cubicTo(w * 0.62f, h * 0.24f, w * 0.74f, h * 0.33f, w * 0.73f, h * 0.48f)
                    cubicTo(w * 0.72f, h * 0.64f, w * 0.64f, h * 0.80f, w * 0.50f, h * 0.80f)
                    close()
                }
                val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                drawPath(leafPath, color = Color(0xFF4ADE80).copy(alpha = 0.55f), style = Stroke(width = 3f, pathEffect = dash))
                drawLine(
                    color = Color(0xFF4ADE80).copy(alpha = 0.55f),
                    start = Offset(w * 0.50f, h * 0.80f),
                    end = Offset(w * 0.50f, h * 0.16f),
                    strokeWidth = 2.5f,
                    pathEffect = dash
                )
            }

            // Orientation Labels
            Text(
                text = "↑ Вершина листа",
                color = EcoColors.Emerald100,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .background(EcoColors.Slate950.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Text(
                text = "↓ Черешок",
                color = Color(0xFFFDE68A),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .background(EcoColors.Slate950.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )

            if (!hasCameraPermission || cameraBindFailed) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(34.dp))
                    Text(
                        text = if (!hasCameraPermission) "Разрешите доступ к камере или выберите фото из галереи" else "Системная камера готова к съёмке",
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {
                            if (!hasCameraPermission) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            else launchSystemCamera()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Emerald600)
                    ) {
                        Text(if (!hasCameraPermission) "Включить камеру" else "Открыть системную камеру", fontSize = 12.sp)
                    }
                }
            }
        }

        // Capture & Gallery Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val cap = imageCapture
                    if (hasCameraPermission && !cameraBindFailed && cap != null) {
                        val app = context.applicationContext as EcoBirchApplication
                        val outFile: File = app.photoStorageManager.createTempCaptureFile()
                        val opts = ImageCapture.OutputFileOptions.Builder(outFile).build()
                        cap.takePicture(
                            opts,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    viewModel.handlePhotoUri(Uri.fromFile(outFile), outFile.name)
                                }
                                override fun onError(exception: ImageCaptureException) {
                                    launchSystemCamera()
                                }
                            }
                        )
                    } else if (hasCameraPermission) {
                        launchSystemCamera()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Emerald600),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1.2f).height(48.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Сделать снимок", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = EcoColors.Emerald700, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Галерея", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EcoColors.Slate900)
            }
        }

        // Shooting Protocol Card (Audit scientific requirement)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = EcoColors.Emerald50,
            border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Emerald100),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EcoColors.Emerald600, modifier = Modifier.size(16.dp))
                    Text("Научный протокол съёмки", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EcoColors.Emerald900)
                }
                Text("• Расправьте лист на белом листе бумаги А4 (без теней и бликов).", fontSize = 11.sp, color = EcoColors.Slate700)
                Text("• Держите камеру строго параллельно поверхности, чтобы избежать перспективных искажений.", fontSize = 11.sp, color = EcoColors.Slate700)
                Text("• Собирайте листья с укороченных побегов нижней части кроны Betula pendula (≥ 10 листьев на точку).", fontSize = 11.sp, color = EcoColors.Slate700)
            }
        }

        // Training Templates Section
        Text("Учебные шаблоны калибровки", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EcoColors.Slate900)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TemplateRow("Здоровый лесопарк (FA ~0.020 · Балл I)", EcoColors.Score1) { viewModel.selectTemplate("healthy") }
            TemplateRow("Городской сквер (FA ~0.047 · Балл III)", EcoColors.Score3) { viewModel.selectTemplate("moderate") }
            TemplateRow("Загруженная трасса (FA ~0.085 · Балл V)", EcoColors.Score5) { viewModel.selectTemplate("stressed") }
        }
    }
}

@Composable
private fun TemplateRow(title: String, badgeColor: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(badgeColor))
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = EcoColors.Slate900)
            }
            Text("Открыть →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EcoColors.Emerald600)
        }
    }
}

@Composable
private fun CameraXPreviewBox(
    onImageCaptureReady: (ImageCapture) -> Unit,
    onBindError: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        cameraProviderFuture.addListener({
            runCatching {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture
                )
                onImageCaptureReady(capture)
            }.onFailure {
                onBindError()
            }
        }, executor)

        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ScanProcessingStage(state: ScanUiState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoColors.Slate950),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            CircularProgressIndicator(color = EcoColors.Emerald600)
            Text(
                text = state.processingHint,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Автоматическое выделение контура и жилок выполняется локально",
                color = EcoColors.Slate400,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ScanAdjustStage(
    state: ScanUiState,
    territories: List<Territory>,
    viewModel: MainViewModel,
    context: Context
) {
    val traits = remember(state.points, state.aspectRatio, state.axisReferenceLengthMm) {
        LeafBioindicationEngine.calculateLeafTraits(
            points = state.points,
            aspectRatio = state.aspectRatio,
            axisReferenceLengthMm = state.axisReferenceLengthMm
        )
    }
    val zScore = remember(traits) { LeafBioindicationEngine.calculateZScore(traits) }
    val rating = remember(zScore) { LeafBioindicationEngine.getEnvironmentalRating(zScore) }

    var showScaleDialog by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.initiateSaveMeasurement(hasLocationPermission = granted)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(EcoColors.Slate50)
    ) {
        // Top status bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val badgeText = when (state.detectionSource) {
                    DetectionSource.GEMINI_AI -> "Gemini AI + ручная доводка"
                    DetectionSource.LOCAL_CONTOUR -> "Локальный контур · проверьте лупой"
                    DetectionSource.TEMPLATE -> "Учебный шаблон"
                    DetectionSource.MANUAL_DEFAULT -> "Ручная калибровка"
                }
                Text(badgeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EcoColors.Emerald700)
                if (state.statusMessage.isNotBlank()) {
                    Text(state.statusMessage, fontSize = 10.sp, color = EcoColors.Slate500, maxLines = 1)
                }
            }
            TextButton(onClick = { viewModel.resetToPhotoSelect() }) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Заново", fontSize = 12.sp)
            }
        }

        // Interactive Precision Canvas with Pinch-Zoom & 2.5x Loupe
        LeafCalibrationCanvas(
            photoBitmap = state.photoBitmap,
            isTemplate = state.isTemplate,
            aspectRatio = state.aspectRatio,
            points = state.points,
            activeLandmark = state.activeLandmark,
            onSelectLandmark = viewModel::setActiveLandmark,
            onMoveLandmark = viewModel::updateLandmark,
            onNudgeActive = viewModel::nudgeActiveLandmark
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Live Z-Score & Traits Card
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Slate200),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ПОКАЗАТЕЛЬ ЛИСТА Z", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EcoColors.Slate500)
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "%.4f".format(zScore),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = EcoColors.Slate900
                            )
                            Text("(${rating.rangeLabel})", fontSize = 11.sp, color = EcoColors.Slate500, modifier = Modifier.padding(bottom = 3.dp))
                        }
                    }

                    Surface(
                        color = EcoColors.scoreColor(rating.score),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Балл ${rating.score} · ${rating.status}",
                            color = EcoColors.scoreTextColor(rating.score),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }

                // Traits Y1..Y5 grid
                val unit = if (state.axisReferenceLengthMm != null) "мм" else "% оси"
                val traitList = listOf(
                    Triple("Y1 Шир.", traits.t1Width.yDiff, "${"%.1f".format(traits.t1Width.xLeft)}/${"%.1f".format(traits.t1Width.xRight)}"),
                    Triple("Y2 Жил.2", traits.t2VeinLen.yDiff, "${"%.1f".format(traits.t2VeinLen.xLeft)}/${"%.1f".format(traits.t2VeinLen.xRight)}"),
                    Triple("Y3 Осн.", traits.t3VeinDist.yDiff, "${"%.1f".format(traits.t3VeinDist.xLeft)}/${"%.1f".format(traits.t3VeinDist.xRight)}"),
                    Triple("Y4 Края", traits.t4EndDist.yDiff, "${"%.1f".format(traits.t4EndDist.xLeft)}/${"%.1f".format(traits.t4EndDist.xRight)}"),
                    Triple("Y5 Угол", traits.t5Angle.yDiff, "${"%.0f".format(traits.t5Angle.xLeft)}°/${"%.0f".format(traits.t5Angle.xRight)}°")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    traitList.forEach { (label, valY, rawDims) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EcoColors.Slate50,
                            border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Slate200),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(label, fontSize = 9.sp, color = EcoColors.Slate500, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "%.3f".format(valY),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EcoColors.Slate900
                                )
                                Text(rawDims, fontSize = 8.sp, color = EcoColors.Slate400, maxLines = 1)
                            }
                        }
                    }
                }

                // Scale Calibration Bar (Honest % vs Calibrated mm)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.axisReferenceLengthMm != null) {
                            "Единицы: мм (ось = ${"%.1f".format(state.axisReferenceLengthMm)} мм)"
                        } else {
                            "Единицы: $unit (безразмерная нормировка по длине листа)"
                        },
                        fontSize = 10.sp,
                        color = EcoColors.Slate500,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showScaleDialog = true }) {
                        Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Эталон мм", fontSize = 11.sp)
                    }
                }

                // Save Button
                Button(
                    onClick = {
                        val hasLoc = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasLoc) {
                            viewModel.initiateSaveMeasurement(hasLocationPermission = true)
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    enabled = !state.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Emerald600),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сохранить в журнал", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    // Physical Scale Calibration Dialog
    if (showScaleDialog) {
        var mmInput by remember { mutableStateOf(state.axisReferenceLengthMm?.toString() ?: "") }
        Dialog(onDismissRequest = { showScaleDialog = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Калибровка масштаба (необязательно)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "Для расчёта Z-индекса масштаб не нужен — формула Захарова безразмерна. Если вы измерили длину центральной жилки листа линейкой, укажите её в мм для отображения X1–X4 в миллиметрах.",
                        fontSize = 12.sp,
                        color = EcoColors.Slate500
                    )
                    OutlinedTextField(
                        value = mmInput,
                        onValueChange = { mmInput = it },
                        label = { Text("Длина главной жилки (мм), напр. 58.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                viewModel.setAxisReferenceLengthMm(null)
                                showScaleDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Сброс (% оси)") }
                        Button(
                            onClick = {
                                viewModel.setAxisReferenceLengthMm(mmInput.replace(',', '.').toDoubleOrNull())
                                showScaleDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Emerald600),
                            modifier = Modifier.weight(1f)
                        ) { Text("Применить") }
                    }
                }
            }
        }
    }

    // Territory / Location Chooser Dialog when saving
    if (state.showLocationChooserDialog) {
        var customName by remember { mutableStateOf("") }
        val userTerritories = territories.filterNot { it.isDemo }

        Dialog(onDismissRequest = { viewModel.dismissLocationChooser() }) {
            Surface(shape = RoundedCornerShape(22.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Куда сохранить замер?", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)

                    if (userTerritories.isNotEmpty()) {
                        Text("Добавить к существующей территории:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EcoColors.Slate700)
                        userTerritories.take(5).forEach { terr ->
                            Surface(
                                onClick = { viewModel.savePendingLeafToExistingTerritory(terr) },
                                shape = RoundedCornerShape(12.dp),
                                color = EcoColors.Slate50,
                                border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(terr.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Замеров: ${terr.leaves.size}", fontSize = 10.sp, color = EcoColors.Slate500)
                                    }
                                    Text("Выбрать →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EcoColors.Emerald600)
                                }
                            }
                        }
                        HorizontalDivider(color = EcoColors.Slate200)
                    }

                    if (state.exifLatitude != null && state.exifLongitude != null) {
                        Text("Создать новую точку по GPS (${"%.4f".format(state.exifLatitude)}, ${"%.4f".format(state.exifLongitude)}):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Название (или авто по адресу)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                viewModel.savePendingLeafAtNewCoordinates(
                                    state.exifLatitude,
                                    state.exifLongitude,
                                    customName
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Emerald600),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Сохранить в новую GPS-точку")
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.routePendingLeafToMapPicker() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Выбрать точку на карте OpenStreetMap")
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanSavedStage(
    state: ScanUiState,
    territories: List<Territory>,
    viewModel: MainViewModel,
    context: Context,
    onOpenJournal: () -> Unit,
    onOpenMap: () -> Unit
) {
    val leaf = state.savedLeaf
    val terr = territories.find { it.id == state.targetTerritoryId } ?: territories.firstOrNull()
    val rating = leaf?.let { LeafBioindicationEngine.getEnvironmentalRating(it.zScore) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            shape = CircleShape,
            color = EcoColors.Emerald50,
            modifier = Modifier.size(68.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EcoColors.Emerald600, modifier = Modifier.size(40.dp))
            }
        }

        Text("Замер сохранён в журнал", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = EcoColors.Slate900)
        Text(
            text = state.savedTerritoryName.ifBlank { terr?.name.orEmpty() },
            fontSize = 13.sp,
            color = EcoColors.Slate500,
            textAlign = TextAlign.Center
        )

        if (leaf != null && rating != null) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Показатель листа Z:", fontSize = 12.sp, color = EcoColors.Slate500)
                        Text("%.4f".format(leaf.zScore), fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                    Surface(
                        color = EcoColors.scoreColor(rating.score),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Балл ${rating.score} · ${rating.status} (${rating.rangeLabel})",
                            color = EcoColors.scoreTextColor(rating.score),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    if (terr != null) {
                        val n = terr.leaves.size
                        Text(
                            text = if (n >= 10) {
                                "Выборка по территории: n = $n (достаточно для оценки по Захарову)"
                            } else {
                                "Выборка по территории: n = $n из 10 рекомендуемых листьев"
                            },
                            fontSize = 11.sp,
                            color = if (n >= 10) EcoColors.Emerald700 else EcoColors.Score3,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        if (terr != null) {
            Button(
                onClick = { viewModel.startScanForTerritory(terr.id) },
                colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Emerald600),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Добавить ещё лист на эту же точку", fontWeight = FontWeight.Bold)
            }
        }

        if (leaf != null && terr != null) {
            OutlinedButton(
                onClick = { viewModel.shareLeafPassport(context, terr, leaf) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Поделиться эко-паспортом")
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onOpenJournal, modifier = Modifier.weight(1f)) {
                Text("В журнал")
            }
            OutlinedButton(onClick = onOpenMap, modifier = Modifier.weight(1f)) {
                Text("На карту")
            }
        }

        TextButton(onClick = { viewModel.resetToPhotoSelect() }) {
            Text("Новый скан на другой точке")
        }
    }
}
