package ru.ecobirch.fa.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import ru.ecobirch.fa.app.domain.math.LeafBioindicationEngine
import ru.ecobirch.fa.app.domain.model.Territory
import ru.ecobirch.fa.app.ui.state.MainViewModel
import ru.ecobirch.fa.app.ui.state.MapUiState
import ru.ecobirch.fa.app.ui.theme.EcoColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapScreen(
    state: MapUiState,
    territories: List<Territory>,
    showZakharovScale: Boolean = false,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.centerMapOnUserGps(granted)
    }

    val mapView = androidx.compose.runtime.remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(12.0)
            controller.setCenter(GeoPoint(state.centerLat, state.centerLng))
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    LaunchedEffect(state.centerTrigger) {
        if (state.centerTrigger > 0) {
            mapView.controller.animateTo(GeoPoint(state.centerLat, state.centerLng))
            if (mapView.zoomLevelDouble < 13.0) {
                mapView.controller.setZoom(14.0)
            }
        }
    }

    LaunchedEffect(territories, state.pendingPinLat, state.pendingPinLng, state.isAddMode, state.selectedTerritoryId, showZakharovScale) {
        mapView.overlays.clear()

        mapView.overlays.add(
            MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    viewModel.onMapTapped(p.latitude, p.longitude)
                    return true
                }

                override fun longPressHelper(p: GeoPoint): Boolean {
                    if (!state.isAddMode) viewModel.toggleMapAddMode()
                    viewModel.onMapTapped(p.latitude, p.longitude)
                    return true
                }
            })
        )

        territories.forEach { terr ->
            val stats = LeafBioindicationEngine.calculateTerritoryStatistics(terr.leaves)
            val color = when {
                stats.primaryLeafCount == 0 -> EcoColors.Slate400
                showZakharovScale -> EcoColors.scoreColor(stats.indicativeRating?.score)
                else -> EcoColors.Emerald600
            }
            val label = when {
                stats.primaryLeafCount == 0 -> "0"
                !stats.isReliable -> "n${stats.primaryLeafCount}"
                showZakharovScale -> "${stats.indicativeRating?.score ?: "—"}"
                else -> "${stats.treeCount}д"
            }
            val marker = Marker(mapView).apply {
                position = GeoPoint(terr.latitude, terr.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = terr.name
                icon = createMarkerDrawable(
                    context = context,
                    fillColor = color.toArgb(),
                    badgeText = label,
                    isSelected = terr.id == state.selectedTerritoryId
                )
                setOnMarkerClickListener { _, _ ->
                    viewModel.selectMapTerritory(terr.id)
                    true
                }
            }
            mapView.overlays.add(marker)
        }

        val pLat = state.pendingPinLat
        val pLng = state.pendingPinLng
        if (pLat != null && pLng != null) {
            val pendingMarker = Marker(mapView).apply {
                position = GeoPoint(pLat, pLng)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = createMarkerDrawable(
                    context = context,
                    fillColor = EcoColors.Emerald600.toArgb(),
                    badgeText = "+",
                    isSelected = true
                )
            }
            mapView.overlays.add(pendingMarker)
        }

        mapView.invalidate()
    }

    val selectedTerritory = territories.find { it.id == state.selectedTerritoryId }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Top Floating Search Bar & Results
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 6.dp,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::updateMapSearchQuery,
                        placeholder = { Text("Поиск адреса, парка или 55.75, 37.61", fontSize = 14.sp, color = EcoColors.Slate400) },
                        textStyle = TextStyle(fontSize = 15.sp, color = EcoColors.Slate900),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.performMapSearch() }),
                        modifier = Modifier.weight(1f)
                    )
                    if (state.isSearching) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp).size(22.dp), strokeWidth = 2.5.dp)
                    } else {
                        IconButton(onClick = viewModel::performMapSearch) {
                            Icon(Icons.Default.Search, contentDescription = "Найти", tint = EcoColors.Emerald600, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            if (state.searchResults.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 8.dp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyColumn(modifier = Modifier.height(210.dp)) {
                        items(state.searchResults) { place ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectGeocodeResult(place) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(place.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EcoColors.Slate900)
                                if (place.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(place.description, fontSize = 13.sp, color = EcoColors.Slate500, maxLines = 1)
                                }
                            }
                            HorizontalDivider(color = EcoColors.Slate100)
                        }
                    }
                }
            }

            if (state.isAddMode) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EcoColors.Emerald900.copy(alpha = 0.94f),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (state.pendingLeafToAttach != null) {
                                "Нажмите на карту, чтобы привязать скан листа"
                            } else {
                                "Нажмите на карту, чтобы поставить новую точку"
                            },
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = viewModel::toggleMapAddMode, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Отмена", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // Right-side Map Controls (GPS + Add Point)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            SmallFloatingActionButton(
                onClick = {
                    val hasLoc = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasLoc) {
                        viewModel.centerMapOnUserGps(true)
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = Color.White,
                contentColor = EcoColors.Slate900,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Моё местоположение", modifier = Modifier.size(22.dp))
            }

            FloatingActionButton(
                onClick = viewModel::toggleMapAddMode,
                shape = RoundedCornerShape(18.dp),
                containerColor = if (state.isAddMode) EcoColors.Score4 else EcoColors.Emerald600,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = if (state.isAddMode) Icons.Default.Close else Icons.Default.AddLocationAlt,
                    contentDescription = "Добавить точку",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Bottom Area: Pending Pin Card, Selected Territory Card, or Crisp Legend
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.isAddMode && state.pendingPinLat != null) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 8.dp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Новая территория", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = EcoColors.Slate900)
                        OutlinedTextField(
                            value = state.pendingAddressName,
                            onValueChange = { viewModel.updatePendingTerritoryFields(it, state.pendingAddressDesc) },
                            label = { Text("Название точки", fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = state.pendingAddressDesc,
                            onValueChange = { viewModel.updatePendingTerritoryFields(state.pendingAddressName, it) },
                            label = { Text("Адрес / комментарий", fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = viewModel::confirmCreateTerritoryFromMap,
                            colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Emerald600),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text(
                                text = if (state.pendingLeafToAttach != null) "Сохранить лист в эту точку" else "Создать точку",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else if (selectedTerritory != null) {
                val stats = LeafBioindicationEngine.calculateTerritoryStatistics(selectedTerritory.leaves)
                val rating = stats.indicativeRating

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 8.dp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(selectedTerritory.name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = EcoColors.Slate900)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${"%.4f".format(selectedTerritory.latitude)}°, ${"%.4f".format(selectedTerritory.longitude)}° · Замеров: ${stats.sampleCount}",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = EcoColors.Slate500
                                )
                            }
                            IconButton(onClick = { viewModel.selectMapTerritory(null) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = EcoColors.Slate500)
                            }
                        }

                        if (rating != null) {
                            Surface(
                                color = if (showZakharovScale) EcoColors.scoreColor(rating.score) else EcoColors.Emerald50,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val shownZ = if (stats.treeCount >= 2) stats.treeMeanFA else stats.meanFA
                                val shownSE = if (stats.treeCount >= 2) stats.treeLevelSE else stats.standardError
                                val seStr = if (stats.primaryLeafCount >= 2) " ± ${"%.4f".format(shownSE)}" else ""
                                val msg = if (showZakharovScale) {
                                    "Z = ${"%.4f".format(shownZ)}$seStr · Ориентир ${rating.score} (${stats.faValidationStatus})"
                                } else {
                                    "Z = ${"%.4f".format(shownZ)}$seStr · ${stats.treeCount} дер. / ${stats.primaryLeafCount} л. (${stats.faValidationStatus})"
                                }
                                val txtColor = if (showZakharovScale) EcoColors.scoreTextColor(rating.score) else EcoColors.Emerald900
                                Text(
                                    text = msg,
                                    color = txtColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        } else {
                            Text("В этой точке ещё нет замеров листьев.", fontSize = 14.sp, color = EcoColors.Slate500)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { viewModel.startScanForTerritory(selectedTerritory.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Emerald600),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Сканировать", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { viewModel.openTerritoryInJournal(selectedTerritory.id) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("В журнал", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                // Readable Legend: Pure Morphometry vs Optional Zakharov Reference
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.96f),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (showZakharovScale) {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LegendPill("I <0.040", EcoColors.Score1)
                            LegendPill("II .040–.044", EcoColors.Score2)
                            LegendPill("III .045–.049", EcoColors.Score3)
                            LegendPill("IV .050–.054", EcoColors.Score4)
                            LegendPill("V >.054", EcoColors.Score5)
                        }
                    } else {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LegendPill("Морфометрия (баллы отключены)", EcoColors.Emerald600)
                            LegendPill("Нет замеров", EcoColors.Slate400)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendPill(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = EcoColors.Slate700
        )
    }
}

private fun createMarkerDrawable(
    context: Context,
    fillColor: Int,
    badgeText: String,
    isSelected: Boolean
): BitmapDrawable {
    val sizePx = if (isSelected) 104 else 84
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f
    val radius = sizePx * 0.36f

    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, radius + (if (isSelected) 9f else 6f), haloPaint)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, radius, fillPaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (fillColor == EcoColors.Score2.toArgb()) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        textSize = sizePx * 0.30f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(badgeText, cx, textY, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}
