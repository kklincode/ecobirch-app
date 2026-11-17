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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
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
import ru.ecobirch.fa.app.data.local.AppSettings
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
    settings: AppSettings,
    viewModel: MainViewModel,
    onOpenJournal: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current

    when (state.stage) {
        ScanStage.SELECT -> ScanSelectStage(
            state = state,
            territories = territories,
            settings = settings,
            viewModel = viewModel,
            context = context,
            onOpenSettings = onOpenSettings
        )
        ScanStage.PROCESSING -> ScanProcessingStage(state = state)
        ScanStage.ADJUST -> ScanAdjustStage(
            state = state,
            territories = territories,
            settings = settings,
            viewModel = viewModel,
            context = context
        )
        ScanStage.SAVED -> ScanSavedStage(
            state = state,
            territories = territories,
            settings = settings,
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
    settings: AppSettings,
    viewModel: MainViewModel,
    context: Context,
    onOpenSettings: () -> Unit
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Editorial Header + Settings button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Сканер листьев",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp,
                    color = EcoColors.Slate900
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Betula pendula · 12 ориентиров",
                    fontSize = 13.5.sp,
                    color = EcoColors.Slate500
                )
            }
            Surface(
                onClick = onOpenSettings,
                shape = CircleShape,
                color = EcoColors.SurfaceTonal,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Настройки и приватность",
                        tint = EcoColors.Slate700,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        if (targetTerr != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = EcoColors.Emerald50,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = EcoColors.Emerald600,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (settings.blindMeasurementMode) "СЛЕПОЙ РЕЖИМ · ТОЧКА СКРЫТА" else "АКТИВНАЯ ТОЧКА СБОРА",
                            fontSize = 11.5.sp,
                            color = EcoColors.Emerald700,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (settings.blindMeasurementMode) "Участок скрыт до сохранения" else targetTerr.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoColors.Slate900
                        )
                        val primaryCount = targetTerr.leaves.count { it.replicateOfLeafId == null }
                        val treeCount = targetTerr.leaves.filter { it.replicateOfLeafId == null }.map { it.treeId }.distinct().size
                        Text("Деревьев: $treeCount · Первичных листьев: $primaryCount", fontSize = 13.sp, color = EcoColors.Slate500)
                    }
                }
            }
        }

        // CameraX Live Viewfinder + Birch Silhouette Alignment Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3.5f)
                .clip(RoundedCornerShape(24.dp))
                .background(EcoColors.Slate950)
                .border(2.dp, EcoColors.Emerald600.copy(alpha = 0.45f), RoundedCornerShape(24.dp)),
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
                drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), 1.4f)
                drawLine(gridColor, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), 1.4f)
                drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), 1.4f)
                drawLine(gridColor, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), 1.4f)

                val leafPath = Path().apply {
                    moveTo(w * 0.50f, h * 0.80f)
                    cubicTo(w * 0.36f, h * 0.80f, w * 0.28f, h * 0.64f, w * 0.27f, h * 0.48f)
                    cubicTo(w * 0.26f, h * 0.33f, w * 0.38f, h * 0.24f, w * 0.50f, h * 0.16f)
                    cubicTo(w * 0.62f, h * 0.24f, w * 0.74f, h * 0.33f, w * 0.73f, h * 0.48f)
                    cubicTo(w * 0.72f, h * 0.64f, w * 0.64f, h * 0.80f, w * 0.50f, h * 0.80f)
                    close()
                }
                val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                drawPath(leafPath, color = Color(0xFF4ADE80).copy(alpha = 0.60f), style = Stroke(width = 3.2f, pathEffect = dash))
                drawLine(
                    color = Color(0xFF4ADE80).copy(alpha = 0.60f),
                    start = Offset(w * 0.50f, h * 0.80f),
                    end = Offset(w * 0.50f, h * 0.16f),
                    strokeWidth = 2.8f,
                    pathEffect = dash
                )
            }

            // Frosted Pill Orientation Badges
            Surface(
                color = EcoColors.Slate950.copy(alpha = 0.78f),
                shape = RoundedCornerShape(99.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            ) {
                Text(
                    text = "↑ Вершина листа",
                    color = EcoColors.Emerald100,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            Surface(
                color = EcoColors.Slate950.copy(alpha = 0.78f),
                shape = RoundedCornerShape(99.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = "↓ Черешок",
                    color = Color(0xFFFDE68A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            if (!hasCameraPermission || cameraBindFailed) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(40.dp))
                    Text(
                        text = if (!hasCameraPermission) {
                            "Разрешите доступ к камере или выберите снимок из галереи"
                        } else {
                            "Системная камера готова к съёмке"
                        },
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {
                            if (!hasCameraPermission) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            else launchSystemCamera()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Emerald600),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Text(
                            text = if (!hasCameraPermission) "Включить камеру" else "Открыть системную камеру",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Capture & Gallery Actions (56dp tactile dock)
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
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(1.25f)
                    .height(54.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Сделать снимок", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EcoColors.Emerald100,
                    contentColor = EcoColors.Emerald900
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Галерея", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        // Shooting Protocol Card (Tonal surface, readable 14sp copy)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = EcoColors.SurfaceTonal,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Протокол съёмки",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = EcoColors.Slate900
                )
                ProtocolStepRow("1", "Расправьте неповреждённый лист на белом листе А4 без теней и бликов.")
                ProtocolStepRow("2", "Держите камеру строго параллельно поверхности листа (исключая перспективу).")
                ProtocolStepRow("3", "Берите листья с укороченных побегов B. pendula (фиксируйте номер дерева).")
            }
        }

        // Training Templates Section
        Text(
            text = "Тренировка на эталонах",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = EcoColors.Slate900
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TemplateRow("Низкая асимметрия (контроль)", "Индекс Z ~0.020", "Эталон 1", EcoColors.Score1) {
                viewModel.selectTemplate("healthy")
            }
            TemplateRow("Умеренная асимметрия", "Индекс Z ~0.047", "Эталон 2", EcoColors.Score3) {
                viewModel.selectTemplate("moderate")
            }
            TemplateRow("Высокая асимметрия", "Индекс Z ~0.085", "Эталон 3", EcoColors.Score5) {
                viewModel.selectTemplate("stressed")
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun ProtocolStepRow(num: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = EcoColors.Emerald600,
            modifier = Modifier.padding(top = 2.dp).size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(num, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            text = text,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            color = EcoColors.Slate700,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TemplateRow(
    title: String,
    subtitle: String,
    scoreLabel: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = badgeColor
            ) {
                Text(
                    text = scoreLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = EcoColors.Slate900)
                Text(subtitle, fontSize = 13.sp, color = EcoColors.Slate500)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = EcoColors.Emerald600,
                modifier = Modifier.size(20.dp)
            )
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
            .background(
                Brush.verticalGradient(
                    listOf(EcoColors.Emerald900, EcoColors.Slate950)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(28.dp)
        ) {
            CircularProgressIndicator(
                color = EcoColors.Score1,
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = state.processingHint,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Автоматическое выделение контура и жилок выполняется локально",
                color = EcoColors.Emerald100.copy(alpha = 0.8f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ScanAdjustStage(
    state: ScanUiState,
    territories: List<Territory>,
    settings: AppSettings,
    viewModel: MainViewModel,
    context: Context
) {
    val isBlind = state.forceBlindMode || settings.blindMeasurementMode
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

    val availableTrees = remember(territories, state.targetTerritoryId, state.treeId, state.treeLabel) {
        val existing = state.targetTerritoryId
            ?.let { tid -> territories.find { it.id == tid } }
            ?.leaves
            ?.map { it.treeId to it.treeLabel }
            ?: emptyList()
        (existing + (state.treeId to state.treeLabel)).distinctBy { it.first }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(EcoColors.CanvasBg)
    ) {
        // Clean Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                val badgeText = when {
                    !state.replicateOfLeafId.isNullOrBlank() -> "Слепой повторный замер (ME)"
                    isBlind -> "Слепой режим · без подсказок"
                    state.detectionSource == DetectionSource.GEMINI_AI -> "Gemini AI · проверьте точки"
                    state.detectionSource == DetectionSource.LOCAL_CONTOUR -> "Локальный контур · проверьте лупой"
                    state.detectionSource == DetectionSource.TEMPLATE -> "Учебный шаблон"
                    else -> "Ручная калибровка"
                }
                Text(badgeText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EcoColors.Emerald800)
                if (state.statusMessage.isNotBlank() && !isBlind) {
                    Text(state.statusMessage, fontSize = 12.5.sp, color = EcoColors.Slate500, maxLines = 1)
                }
            }
            Surface(
                onClick = { viewModel.resetToPhotoSelect() },
                shape = RoundedCornerShape(99.dp),
                color = EcoColors.SurfaceTonal
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = EcoColors.Slate700, modifier = Modifier.size(16.dp))
                    Text("Заново", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = EcoColors.Slate900)
                }
            }
        }

        // Interactive Precision Canvas with Pinch-Zoom & 2.6x Loupe
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

        Spacer(modifier = Modifier.height(14.dp))

        // Tree ID Hierarchy + Morphometry / Blind Results Sheet
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(26.dp),
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Tree Identifier & Botanical QC (prevents pseudoreplication & wrong-species/damaged leaves)
                if (state.replicateOfLeafId.isNullOrBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Идентификатор дерева (против псевдорепликации)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoColors.Slate700
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableTrees) { (tid, tlabel) ->
                                FilterChip(
                                    selected = state.treeId == tid,
                                    onClick = { viewModel.setScanTree(tid, tlabel) },
                                    label = { Text(tlabel, fontSize = 12.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EcoColors.Emerald600,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = { viewModel.addNextScanTree() },
                                    label = { Text("+ Дерево", fontSize = 12.5.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = state.leafIntegrityVerified,
                                onCheckedChange = viewModel::setLeafIntegrityVerified
                            )
                            Text(
                                text = "B. pendula · укороченный побег, без повреждений",
                                fontSize = 12.5.sp,
                                color = EcoColors.Slate700
                            )
                        }
                    }
                }

                if (isBlind) {
                    Surface(
                        color = EcoColors.Emerald50,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = EcoColors.Emerald700)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Слепой режим замера активен",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EcoColors.Emerald900
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Индекс Z, признаки Y1–Y5 и название участка скрыты на время разметки для защиты от подтверждающего смещения (Kozlov 2025).",
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp,
                                    color = EcoColors.Slate700
                                )
                            }
                        }
                    }
                } else {
                    // Hero Z-Score Panel (Pure Morphometry or Optional Zakharov Reference)
                    Surface(
                        color = if (settings.enableZakharovScale) EcoColors.scoreSoftBg(rating.score) else EcoColors.Emerald50,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ИНДЕКС ЛИСТА Z",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    color = EcoColors.Slate700
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "%.4f".format(zScore),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = EcoColors.Slate900
                                )
                                Text(
                                    text = if (settings.enableZakharovScale) {
                                        "Ориентир Захарова: ${rating.rangeLabel}"
                                    } else {
                                        "Индекс отн. асимметрии (без баллов)"
                                    },
                                    fontSize = 12.5.sp,
                                    color = EcoColors.Slate700
                                )
                            }

                            if (settings.enableZakharovScale) {
                                Surface(
                                    color = EcoColors.scoreColor(rating.score),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Балл ${rating.score}",
                                            color = EcoColors.scoreTextColor(rating.score),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = rating.status,
                                            color = EcoColors.scoreTextColor(rating.score).copy(alpha = 0.92f),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2-Column Grid for Traits Y1..Y5 + Scale Calibration Card
                    val unit = if (state.axisReferenceLengthMm != null) "мм" else "% оси"
                    val traitCards = listOf(
                        Triple("Y1 · Ширина", "%.3f".format(traits.t1Width.yDiff), "${"%.1f".format(traits.t1Width.xLeft)} / ${"%.1f".format(traits.t1Width.xRight)} $unit"),
                        Triple("Y2 · 2-я жилка", "%.3f".format(traits.t2VeinLen.yDiff), "${"%.1f".format(traits.t2VeinLen.xLeft)} / ${"%.1f".format(traits.t2VeinLen.xRight)} $unit"),
                        Triple("Y3 · Основания", "%.3f".format(traits.t3VeinDist.yDiff), "${"%.1f".format(traits.t3VeinDist.xLeft)} / ${"%.1f".format(traits.t3VeinDist.xRight)} $unit"),
                        Triple("Y4 · Края жилок", "%.3f".format(traits.t4EndDist.yDiff), "${"%.1f".format(traits.t4EndDist.xLeft)} / ${"%.1f".format(traits.t4EndDist.xRight)} $unit"),
                        Triple("Y5 · Угол жилки", "%.3f".format(traits.t5Angle.yDiff), "${"%.0f".format(traits.t5Angle.xLeft)}° / ${"%.0f".format(traits.t5Angle.xRight)}°")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            TraitTile(traitCards[0], Modifier.weight(1f))
                            TraitTile(traitCards[1], Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            TraitTile(traitCards[2], Modifier.weight(1f))
                            TraitTile(traitCards[3], Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            TraitTile(traitCards[4], Modifier.weight(1f))
                            // 6th Slot: Scale Calibration Tile
                            Surface(
                                onClick = { showScaleDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                color = EcoColors.Emerald50,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Straighten, contentDescription = null, tint = EcoColors.Emerald700, modifier = Modifier.size(16.dp))
                                        Text("Масштаб", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = EcoColors.Emerald900)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (state.axisReferenceLengthMm != null) {
                                            "${"%.1f".format(state.axisReferenceLengthMm)} мм"
                                        } else {
                                            "% оси"
                                        },
                                        fontSize = 18.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EcoColors.Emerald800
                                    )
                                    Text(
                                        text = "Задать эталон мм",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EcoColors.Emerald700
                                    )
                                }
                            }
                        }
                    }
                }

                if (isBlind) {
                    OutlinedButton(
                        onClick = { showScaleDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.axisReferenceLengthMm != null) "Эталон: ${"%.1f".format(state.axisReferenceLengthMm)} мм" else "Калибровка масштаба (% оси)",
                            fontSize = 13.5.sp
                        )
                    }
                }

                // Primary Save Button
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
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сохранить в журнал", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
    }

    // Physical Scale Calibration Dialog
    if (showScaleDialog) {
        var mmInput by remember { mutableStateOf(state.axisReferenceLengthMm?.toString() ?: "") }
        Dialog(onDismissRequest = { showScaleDialog = false }) {
            Surface(shape = RoundedCornerShape(28.dp), color = Color.White) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Калибровка масштаба", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = EcoColors.Slate900)
                    Text(
                        text = "Для расчёта индекса Z масштаб не нужен — формула Захарова безразмерна. Если вы измерили главную жилку листа линейкой, укажите её в миллиметрах.",
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        color = EcoColors.Slate500
                    )
                    OutlinedTextField(
                        value = mmInput,
                        onValueChange = { mmInput = it },
                        label = { Text("Длина главной жилки (мм), напр. 58.5", fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                viewModel.setAxisReferenceLengthMm(null)
                                showScaleDialog = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("Сброс", fontSize = 14.sp) }
                        Button(
                            onClick = {
                                viewModel.setAxisReferenceLengthMm(mmInput.replace(',', '.').toDoubleOrNull())
                                showScaleDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Emerald600),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) { Text("Применить", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
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
            Surface(shape = RoundedCornerShape(28.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Куда сохранить замер?", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = EcoColors.Slate900)

                    if (userTerritories.isNotEmpty()) {
                        Text("Добавить к существующей точке:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EcoColors.Slate700)
                        userTerritories.take(5).forEach { terr ->
                            Surface(
                                onClick = { viewModel.savePendingLeafToExistingTerritory(terr) },
                                shape = RoundedCornerShape(16.dp),
                                color = EcoColors.SurfaceTonal,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(terr.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EcoColors.Slate900)
                                        Text("Замеров: ${terr.leaves.size}", fontSize = 12.5.sp, color = EcoColors.Slate500)
                                    }
                                    Text("Выбрать →", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EcoColors.Emerald700)
                                }
                            }
                        }
                        HorizontalDivider(color = EcoColors.Hairline)
                    }

                    if (state.exifLatitude != null && state.exifLongitude != null) {
                        Text(
                            text = "Новая точка по GPS (${"%.4f".format(state.exifLatitude)}, ${"%.4f".format(state.exifLongitude)}):",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoColors.Slate700
                        )
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Название (или авто по адресу)", fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
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
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Сохранить по GPS", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.routePendingLeafToMapPicker() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Выбрать на карте OSM", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TraitTile(data: Triple<String, String, String>, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = EcoColors.CanvasBg,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(data.first, fontSize = 12.5.sp, color = EcoColors.Slate500, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = data.second,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = EcoColors.Slate900
            )
            Text(data.third, fontSize = 12.sp, color = EcoColors.Slate500, maxLines = 1)
        }
    }
}

@Composable
private fun ScanSavedStage(
    state: ScanUiState,
    territories: List<Territory>,
    settings: AppSettings,
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = CircleShape,
            color = EcoColors.Emerald100,
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EcoColors.Emerald600, modifier = Modifier.size(44.dp))
            }
        }

        Text("Замер сохранён", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = EcoColors.Slate900)
        Text(
            text = "${state.savedTerritoryName.ifBlank { terr?.name.orEmpty() }}${if (leaf != null) " · ${leaf.treeLabel}" else ""}",
            fontSize = 15.sp,
            color = EcoColors.Slate500,
            textAlign = TextAlign.Center
        )

        if (leaf != null && rating != null) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Индекс листа Z", fontSize = 14.sp, color = EcoColors.Slate500, fontWeight = FontWeight.SemiBold)
                        Text("%.4f".format(leaf.zScore), fontSize = 26.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = EcoColors.Slate900)
                    }
                    if (settings.enableZakharovScale) {
                        Surface(
                            color = EcoColors.scoreColor(rating.score),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Ориентир Захарова: Балл ${rating.score} (${rating.rangeLabel}) · 1 лист не диагностичен",
                                color = EcoColors.scoreTextColor(rating.score),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    if (terr != null) {
                        val terrStats = LeafBioindicationEngine.calculateTerritoryStatistics(terr.leaves)
                        val progress = (terrStats.primaryLeafCount / 10f).coerceIn(0f, 1f)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { progress },
                                color = if (terrStats.isHierarchicallyReliable) EcoColors.Emerald600 else EcoColors.Score3,
                                trackColor = EcoColors.SurfaceTonal,
                                strokeCap = StrokeCap.Round,
                                modifier = Modifier.fillMaxWidth().height(8.dp)
                            )
                            Text(
                                text = "Деревьев: ${terrStats.treeCount} · Первичных листьев: ${terrStats.primaryLeafCount} · Повторов ME: ${terrStats.replicateCount}",
                                fontSize = 13.sp,
                                color = if (terrStats.isHierarchicallyReliable) EcoColors.Emerald700 else EcoColors.Slate700,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = terrStats.faValidationStatus,
                                fontSize = 12.5.sp,
                                color = EcoColors.Slate500
                            )
                        }
                    }
                }
            }
        }

        if (terr != null) {
            Button(
                onClick = { viewModel.startScanForTerritory(terr.id) },
                colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Emerald600),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Добавить ещё лист сюда", fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
            }
        }

        if (leaf != null && terr != null) {
            Button(
                onClick = { viewModel.shareLeafPassport(context, terr, leaf) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EcoColors.Emerald100,
                    contentColor = EcoColors.Emerald900
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Поделиться эко-паспортом", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onOpenJournal,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("В журнал", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = onOpenMap,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).height(48.dp)
            ) {
                Text("На карту", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        TextButton(onClick = { viewModel.resetToPhotoSelect() }) {
            Text("Новый скан на другой точке", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
