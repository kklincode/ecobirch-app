package ru.ecobirch.fa.app.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ecobirch.fa.app.domain.math.LeafBioindicationEngine
import ru.ecobirch.fa.app.domain.model.LeafAnalysis
import ru.ecobirch.fa.app.domain.model.Territory
import ru.ecobirch.fa.app.ui.components.EcoPassportDialog
import ru.ecobirch.fa.app.ui.components.FAAnalyticsSection
import ru.ecobirch.fa.app.ui.state.MainViewModel
import ru.ecobirch.fa.app.ui.theme.EcoColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JournalScreen(
    territories: List<Territory>,
    focusedTerritoryId: String?,
    showZakharovScale: Boolean = false,
    viewModel: MainViewModel,
    onNavigateToMap: () -> Unit,
    onNavigateToScan: () -> Unit
) {
    val context = LocalContext.current
    var viewMode by remember { mutableStateOf("chart") } // "chart" | "territories"
    var expandedTerritoryId by remember { mutableStateOf<String?>(null) }
    var expandedLeafId by remember { mutableStateOf<String?>(null) }
    var passportTarget by remember { mutableStateOf<Pair<Territory, LeafAnalysis>?>(null) }
    var territoryToDelete by remember { mutableStateOf<Territory?>(null) }
    var showClearStep1 by remember { mutableStateOf(false) }
    var showClearStep2 by remember { mutableStateOf(false) }

    LaunchedEffect(focusedTerritoryId) {
        if (!focusedTerritoryId.isNullOrBlank()) {
            viewMode = "territories"
            expandedTerritoryId = focusedTerritoryId
            viewModel.consumeFocusedJournalTerritory()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportJournalToUri(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importJournalFromUri(uri)
    }

    val hasDemo = territories.any { it.isDemo }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Editorial Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Журнал наблюдений",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp,
                    color = EcoColors.Slate900
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Локальное хранилище Room · JSON резерв",
                    fontSize = 13.5.sp,
                    color = EcoColors.Slate500
                )
            }
            if (territories.isNotEmpty()) {
                Surface(
                    onClick = { showClearStep1 = true },
                    shape = CircleShape,
                    color = EcoColors.Score4Bg,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Очистить журнал",
                            tint = EcoColors.Score4,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }

        // Scrollable Tonal Action Pills (Export, Import, Demo — never cramped!)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            item {
                ActionPill(
                    icon = Icons.Default.Download,
                    label = "Экспорт JSON",
                    tint = EcoColors.Emerald700,
                    bg = EcoColors.Emerald50
                ) {
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    exportLauncher.launch("betula_fa_journal_$dateStr.json")
                }
            }
            item {
                ActionPill(
                    icon = Icons.Default.Upload,
                    label = "Импорт JSON",
                    tint = EcoColors.Slate700,
                    bg = Color.White
                ) {
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                }
            }
            item {
                ActionPill(
                    icon = Icons.Default.Science,
                    label = if (hasDemo) "Убрать демо" else "+ Демо-данные",
                    tint = EcoColors.Slate700,
                    bg = Color.White
                ) {
                    if (hasDemo) viewModel.clearDemoTerritories() else viewModel.loadDemoTerritories()
                }
            }
        }

        // Pill Segmented Switch: Analytics vs Territories
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EcoColors.SurfaceTonal)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("chart" to "Аналитика", "territories" to "Точки (${territories.size})").forEach { (id, label) ->
                val selected = viewMode == id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) Color.White else Color.Transparent)
                        .clickable { viewMode = id }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = if (id == "chart") Icons.Default.Timeline else Icons.AutoMirrored.Filled.ListAlt,
                            contentDescription = null,
                            tint = if (selected) EcoColors.Emerald700 else EcoColors.Slate500,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) EcoColors.Emerald800 else EcoColors.Slate700
                        )
                    }
                }
            }
        }

        if (viewMode == "chart") {
            FAAnalyticsSection(
                territories = territories,
                showZakharovScale = showZakharovScale,
                onLoadDemoData = viewModel::loadDemoTerritories
            )
            if (territories.isNotEmpty()) {
                TextButton(
                    onClick = { viewMode = "territories" },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Перейти к списку территорий (${territories.size}) →",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else if (territories.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Журнал наблюдений пуст", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EcoColors.Slate900)
                    Text(
                        text = "Снимите первый лист берёзы или выберите точку мониторинга на карте.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        color = EcoColors.Slate500
                    )
                    Button(
                        onClick = onNavigateToScan,
                        colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Emerald600),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Начать замер листа", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onNavigateToMap,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Открыть карту точек", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            territories.forEach { terr ->
                val isExpanded = expandedTerritoryId == terr.id
                val stats = LeafBioindicationEngine.calculateTerritoryStatistics(terr.leaves)
                val rating = stats.indicativeRating

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = if (isExpanded) 3.dp else 1.dp,
                    border = if (isExpanded) androidx.compose.foundation.BorderStroke(1.5.dp, EcoColors.Emerald600) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedTerritoryId = if (isExpanded) null else terr.id
                                    expandedLeafId = null
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = when {
                                    stats.primaryLeafCount == 0 -> EcoColors.SurfaceTonal
                                    showZakharovScale -> EcoColors.scoreColor(rating?.score)
                                    else -> EcoColors.Emerald100
                                },
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    val txtColor = when {
                                        stats.primaryLeafCount == 0 -> EcoColors.Slate500
                                        showZakharovScale -> EcoColors.scoreTextColor(rating?.score)
                                        else -> EcoColors.Emerald900
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = when {
                                                stats.primaryLeafCount == 0 -> "—"
                                                showZakharovScale -> if (!stats.isReliable) "~${rating?.score}" else "${rating?.score}"
                                                else -> "${stats.treeCount}д"
                                            },
                                            fontWeight = FontWeight.Black,
                                            fontSize = if (showZakharovScale) 19.sp else 16.sp,
                                            color = txtColor
                                        )
                                        Text(
                                            text = when {
                                                stats.primaryLeafCount == 0 -> "ПУСТО"
                                                showZakharovScale -> if (stats.isReliable) "ОРИЕНТ" else "ПРЕДВ"
                                                else -> "n=${stats.primaryLeafCount}"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = txtColor.copy(alpha = 0.88f)
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = terr.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = EcoColors.Slate900,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (terr.isDemo) {
                                        Surface(color = EcoColors.SurfaceTonal, shape = RoundedCornerShape(8.dp)) {
                                            Text(
                                                text = "ДЕМО",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EcoColors.Slate700,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                val shownZ = if (stats.treeCount >= 2) stats.treeMeanFA else stats.meanFA
                                Text(
                                    text = "${"%.3f".format(terr.latitude)}°, ${"%.3f".format(terr.longitude)}° · дер=${stats.treeCount}, n=${stats.primaryLeafCount}" +
                                        if (stats.primaryLeafCount > 0) " · Z ${"%.4f".format(shownZ)}" else "",
                                    fontSize = 12.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = EcoColors.Slate500
                                )
                            }

                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = EcoColors.Slate500,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        if (isExpanded) {
                            HorizontalDivider(color = EcoColors.Hairline)
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (terr.description.isNotBlank()) {
                                    Text(terr.description, fontSize = 13.5.sp, lineHeight = 19.sp, color = EcoColors.Slate700)
                                }

                                if (stats.primaryLeafCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = EcoColors.CanvasBg,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = if (stats.isHierarchicallyReliable) {
                                                    "Статус выборки: Достоверная (${stats.treeCount} дер., n = ${stats.primaryLeafCount} ≥ 10)"
                                                } else if (stats.isReliable) {
                                                    "Выборка n = ${stats.primaryLeafCount} ≥ 10 · Деревьев: ${stats.treeCount} (рек. ≥ 5)"
                                                } else {
                                                    "Предварительная выборка (n = ${stats.primaryLeafCount} из 10 · Деревьев: ${stats.treeCount})"
                                                },
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (stats.isHierarchicallyReliable) EcoColors.Emerald700 else EcoColors.Score3
                                            )
                                            Text(
                                                text = if (stats.treeCount >= 2) {
                                                    "Междревес. Z = ${"%.4f".format(stats.treeMeanFA)} ± ${"%.4f".format(stats.treeLevelSE)} (SE) · 95% ДИ ±${"%.4f".format(stats.treeLevelCi95HalfWidth)}"
                                                } else {
                                                    "Z = ${"%.4f".format(stats.meanFA)} ± ${"%.4f".format(stats.standardError)} (SE) · 95% ДИ ±${"%.4f".format(stats.ci95HalfWidth)}"
                                                },
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = EcoColors.Slate700
                                            )
                                            val meStr = stats.measurementError?.let {
                                                " · ME=${"%.1f".format(it.measurementErrorPercent)}% (пар=${it.pairedLeafCount})"
                                            } ?: " · ME: нет повторов"
                                            Text(
                                                text = "Проверка: ${stats.faValidationStatus}$meStr",
                                                fontSize = 12.sp,
                                                color = if (stats.hasDirectionalAsymmetry || stats.hasAntisymmetry) EcoColors.Score4 else EcoColors.Slate700
                                            )
                                        }
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.startScanForTerritory(terr.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Emerald600),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f).height(46.dp)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(17.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("+ Замер", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.centerMapOnTerritory(terr)
                                            onNavigateToMap()
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f).height(46.dp)
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(17.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("На карте", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Surface(
                                        onClick = { territoryToDelete = terr },
                                        shape = RoundedCornerShape(14.dp),
                                        color = EcoColors.Score4Bg,
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Delete, contentDescription = "Удалить территорию", tint = EcoColors.Score4, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }

                                terr.leaves.forEach { leaf ->
                                    val leafExpanded = expandedLeafId == leaf.id
                                    val leafRating = LeafBioindicationEngine.getEnvironmentalRating(leaf.zScore)
                                    var thumb by remember(leaf.photoUrl) { mutableStateOf<Bitmap?>(null) }
                                    LaunchedEffect(leaf.photoUrl) {
                                        thumb = viewModel.getThumbnailBitmap(leaf.photoUrl)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = EcoColors.CanvasBg,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { expandedLeafId = if (leafExpanded) null else leaf.id }
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                if (thumb != null) {
                                                    Image(
                                                        bitmap = thumb!!.asImageBitmap(),
                                                        contentDescription = leaf.photoName,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                                                    )
                                                } else {
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = EcoColors.scoreSoftBg(leafRating.score),
                                                        modifier = Modifier.size(48.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = "${leafRating.score}",
                                                                fontSize = 18.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = EcoColors.scoreColor(leafRating.score)
                                                            )
                                                        }
                                                    }
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(leaf.photoName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = EcoColors.Slate900, maxLines = 1)
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    val meta = buildString {
                                                        append("Z = ${"%.4f".format(leaf.zScore)} · ${leaf.treeLabel}")
                                                        if (!leaf.replicateOfLeafId.isNullOrBlank()) append(" · Повтор ME")
                                                        else if (leaf.isBlindMeasurement) append(" · Слепой")
                                                        if (showZakharovScale) append(" · Ор.${leafRating.score}")
                                                    }
                                                    Text(
                                                        text = meta,
                                                        fontSize = 12.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = EcoColors.Slate700
                                                    )
                                                }

                                                IconButton(onClick = { passportTarget = terr to leaf }, modifier = Modifier.size(38.dp)) {
                                                    Icon(Icons.Default.Share, contentDescription = "Эко-паспорт", tint = EcoColors.Emerald600, modifier = Modifier.size(20.dp))
                                                }
                                            }

                                            if (leafExpanded) {
                                                HorizontalDivider(color = EcoColors.Hairline)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Y1:${"%.3f".format(leaf.traits.t1Width.yDiff)}  " +
                                                            "Y2:${"%.3f".format(leaf.traits.t2VeinLen.yDiff)}  " +
                                                            "Y3:${"%.3f".format(leaf.traits.t3VeinDist.yDiff)}\n" +
                                                            "Y4:${"%.3f".format(leaf.traits.t4EndDist.yDiff)}  " +
                                                            "Y5:${"%.3f".format(leaf.traits.t5Angle.yDiff)}",
                                                        fontSize = 12.sp,
                                                        lineHeight = 18.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = EcoColors.Slate700,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    if (leaf.replicateOfLeafId.isNullOrBlank()) {
                                                        TextButton(onClick = { viewModel.startBlindReplicateMeasurement(terr.id, leaf) }) {
                                                            Text("Повтор (ME)", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = EcoColors.Emerald700)
                                                        }
                                                    }
                                                    TextButton(onClick = { viewModel.deleteLeaf(leaf.id) }) {
                                                        Text("Удалить", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = EcoColors.Score4)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }

    passportTarget?.let { (terr, leaf) ->
        EcoPassportDialog(
            territory = terr,
            leaf = leaf,
            showZakharovScale = showZakharovScale,
            onShare = { viewModel.shareLeafPassport(context, terr, leaf) },
            onDismiss = { passportTarget = null }
        )
    }

    territoryToDelete?.let { terr ->
        AlertDialog(
            onDismissRequest = { territoryToDelete = null },
            title = { Text("Удалить территорию?", fontSize = 19.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Точка «${terr.name}» и все её замеры (${terr.leaves.size} шт.) будут удалены.", fontSize = 14.5.sp, lineHeight = 20.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTerritory(terr.id)
                        territoryToDelete = null
                    }
                ) { Text("Удалить", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EcoColors.Score4) }
            },
            dismissButton = {
                TextButton(onClick = { territoryToDelete = null }) { Text("Отмена", fontSize = 14.sp) }
            }
        )
    }

    if (showClearStep1) {
        AlertDialog(
            onDismissRequest = { showClearStep1 = false },
            title = { Text("Очистить весь журнал?", fontSize = 19.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Рекомендуется сначала сохранить резервную копию данных в JSON. Все территории и фотографии листьев будут удалены.",
                    fontSize = 14.5.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearStep1 = false
                    showClearStep2 = true
                }) { Text("Продолжить", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EcoColors.Score4) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        exportLauncher.launch("betula_backup_$dateStr.json")
                    }) { Text("Экспорт JSON", fontSize = 14.sp) }
                    TextButton(onClick = { showClearStep1 = false }) { Text("Отмена", fontSize = 14.sp) }
                }
            }
        )
    }

    if (showClearStep2) {
        AlertDialog(
            onDismissRequest = { showClearStep2 = false },
            title = { Text("Подтвердите удаление", fontSize = 19.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Это действие необратимо удалит все записи из локальной базы данных.",
                    fontSize = 14.5.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllJournalData()
                        showClearStep2 = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Score4)
                ) { Text("Удалить всё", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showClearStep2 = false }) { Text("Отмена", fontSize = 14.sp) }
            }
        )
    }
}

@Composable
private fun ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    bg: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(99.dp),
        color = bg,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = tint)
        }
    }
}
