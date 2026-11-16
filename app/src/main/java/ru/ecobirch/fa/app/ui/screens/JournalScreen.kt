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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EcoColors.Emerald50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Emerald100)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, tint = EcoColors.Emerald600, modifier = Modifier.padding(10.dp))
                }
                Column {
                    Text("Журнал наблюдений", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = EcoColors.Slate900)
                    Text("Надёжная локальная БД (Room)", fontSize = 11.sp, color = EcoColors.Slate500)
                }
            }
        }

        // Action Bar: Export, Import, Demo, Clear
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    exportLauncher.launch("betula_fa_journal_$dateStr.json")
                },
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = EcoColors.Emerald600, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Экспорт", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null, tint = EcoColors.Slate700, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Импорт", fontSize = 11.sp)
            }

            if (hasDemo) {
                OutlinedButton(
                    onClick = viewModel::clearDemoTerritories,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("Убрать демо", fontSize = 11.sp)
                }
            } else {
                OutlinedButton(
                    onClick = viewModel::loadDemoTerritories,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("+ Демо", fontSize = 11.sp)
                }
            }

            if (territories.isNotEmpty()) {
                IconButton(
                    onClick = { showClearStep1 = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Очистить журнал", tint = EcoColors.Score4, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Top Segmented Switch: Analytics vs Territories
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(EcoColors.Slate200.copy(alpha = 0.75f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("chart" to "График ФА (7 дней)", "territories" to "Территории (${territories.size})").forEach { (id, label) ->
                val selected = viewMode == id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Color.White else Color.Transparent)
                        .clickable { viewMode = id }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(
                            imageVector = if (id == "chart") Icons.Default.Timeline else Icons.AutoMirrored.Filled.ListAlt,
                            contentDescription = null,
                            tint = if (selected) EcoColors.Emerald600 else EcoColors.Slate500,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) EcoColors.Emerald700 else EcoColors.Slate700
                        )
                    }
                }
            }
        }

        if (viewMode == "chart") {
            FAAnalyticsSection(
                territories = territories,
                onLoadDemoData = viewModel::loadDemoTerritories
            )
            if (territories.isNotEmpty()) {
                TextButton(
                    onClick = { viewMode = "territories" },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Перейти к списку территорий (${territories.size}) →", fontSize = 12.sp)
                }
            }
        } else if (territories.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Журнал наблюдений пуст", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "Снимите первый лист берёзы или выберите точку мониторинга на карте.",
                        fontSize = 12.sp,
                        color = EcoColors.Slate500
                    )
                    Button(
                        onClick = onNavigateToScan,
                        colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Emerald600),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Начать замер листа")
                    }
                    OutlinedButton(onClick = onNavigateToMap, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Открыть карту точек")
                    }
                }
            }
        } else {
            territories.forEach { terr ->
                val isExpanded = expandedTerritoryId == terr.id
                val stats = LeafBioindicationEngine.calculateTerritoryStatistics(terr.leaves)
                val rating = stats.indicativeRating

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isExpanded) 1.5.dp else 1.dp,
                        color = if (isExpanded) EcoColors.Emerald600 else EcoColors.Slate200
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Territory Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedTerritoryId = if (isExpanded) null else terr.id
                                    expandedLeafId = null
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (stats.sampleCount == 0) EcoColors.Slate100 else EcoColors.scoreColor(rating?.score),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    val txtColor = if (stats.sampleCount == 0) EcoColors.Slate500 else EcoColors.scoreTextColor(rating?.score)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (stats.sampleCount == 0) "—" else if (!stats.isReliable) "~${rating?.score}" else "${rating?.score}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = txtColor
                                        )
                                        Text(
                                            text = if (stats.isReliable) "БАЛЛ" else "ПРЕДВ",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = txtColor.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = terr.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = EcoColors.Slate900,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (terr.isDemo) {
                                        Surface(color = EcoColors.Slate100, shape = RoundedCornerShape(6.dp)) {
                                            Text("ДЕМО", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = EcoColors.Slate500, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Text(
                                    text = "${"%.3f".format(terr.latitude)}°, ${"%.3f".format(terr.longitude)}° · n=${stats.sampleCount}" +
                                        if (stats.sampleCount > 0) " · FA ${"%.4f".format(stats.meanFA)}" else "",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = EcoColors.Slate500
                                )
                            }

                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = EcoColors.Slate500
                            )
                        }

                        if (isExpanded) {
                            HorizontalDivider(color = EcoColors.Slate100)
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (terr.description.isNotBlank()) {
                                    Text(terr.description, fontSize = 12.sp, color = EcoColors.Slate700)
                                }

                                // Statistical summary box (audit fix: n, SE, 95% CI, reliability warning)
                                if (stats.sampleCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = EcoColors.Slate50,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Slate200),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(
                                                text = if (stats.isReliable) {
                                                    "Статус выборки: Достоверная (n = ${stats.sampleCount} ≥ 10)"
                                                } else {
                                                    "Предварительная оценка (n = ${stats.sampleCount} из 10 рекомендуемых листьев)"
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (stats.isReliable) EcoColors.Emerald700 else EcoColors.Score3
                                            )
                                            Text(
                                                text = "FA = ${"%.4f".format(stats.meanFA)} ± ${"%.4f".format(stats.standardError)} (SE) · 95% ДИ ±${"%.4f".format(stats.ci95HalfWidth)} · разброс ${"%.4f".format(stats.minZ)}–${"%.4f".format(stats.maxZ)}",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = EcoColors.Slate700
                                            )
                                        }
                                    }
                                }

                                // Quick actions row
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.startScanForTerritory(terr.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Emerald600),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("+ Замер", fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.centerMapOnTerritory(terr)
                                            onNavigateToMap()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("На карте", fontSize = 12.sp)
                                    }
                                    IconButton(onClick = { territoryToDelete = terr }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Удалить территорию", tint = EcoColors.Score4)
                                    }
                                }

                                // Leaves list inside territory
                                terr.leaves.forEach { leaf ->
                                    val leafExpanded = expandedLeafId == leaf.id
                                    val leafRating = LeafBioindicationEngine.getEnvironmentalRating(leaf.zScore)
                                    var thumb by remember(leaf.photoUrl) { mutableStateOf<Bitmap?>(null) }
                                    LaunchedEffect(leaf.photoUrl) {
                                        thumb = viewModel.getThumbnailBitmap(leaf.photoUrl)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = EcoColors.Slate50,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Slate200),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { expandedLeafId = if (leafExpanded) null else leaf.id }
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                if (thumb != null) {
                                                    Image(
                                                        bitmap = thumb!!.asImageBitmap(),
                                                        contentDescription = leaf.photoName,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                                    )
                                                } else {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = EcoColors.Emerald50,
                                                        modifier = Modifier.size(40.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text("${leafRating.score}", fontWeight = FontWeight.Bold, color = EcoColors.scoreColor(leafRating.score))
                                                        }
                                                    }
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(leaf.photoName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                                    Text(
                                                        text = "Z = ${"%.4f".format(leaf.zScore)} · Балл ${leafRating.score}",
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = EcoColors.Slate700
                                                    )
                                                }

                                                IconButton(onClick = { passportTarget = terr to leaf }, modifier = Modifier.size(32.dp)) {
                                                    Icon(Icons.Default.Share, contentDescription = "Эко-паспорт", tint = EcoColors.Emerald600, modifier = Modifier.size(16.dp))
                                                }
                                            }

                                            if (leafExpanded) {
                                                HorizontalDivider(color = EcoColors.Slate200)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Y1:${"%.3f".format(leaf.traits.t1Width.yDiff)} " +
                                                            "Y2:${"%.3f".format(leaf.traits.t2VeinLen.yDiff)} " +
                                                            "Y3:${"%.3f".format(leaf.traits.t3VeinDist.yDiff)} " +
                                                            "Y4:${"%.3f".format(leaf.traits.t4EndDist.yDiff)} " +
                                                            "Y5:${"%.3f".format(leaf.traits.t5Angle.yDiff)}",
                                                        fontSize = 9.5.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = EcoColors.Slate700
                                                    )
                                                    TextButton(onClick = { viewModel.deleteLeaf(leaf.id) }) {
                                                        Text("Удалить", fontSize = 11.sp, color = EcoColors.Score4)
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
    }

    // Eco-Passport Dialog
    passportTarget?.let { (terr, leaf) ->
        EcoPassportDialog(
            territory = terr,
            leaf = leaf,
            onShare = { viewModel.shareLeafPassport(context, terr, leaf) },
            onDismiss = { passportTarget = null }
        )
    }

    // Confirm Delete Territory Dialog
    territoryToDelete?.let { terr ->
        AlertDialog(
            onDismissRequest = { territoryToDelete = null },
            title = { Text("Удалить территорию?") },
            text = { Text("Точка «${terr.name}» и все её замеры (${terr.leaves.size} шт.) будут удалены.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTerritory(terr.id)
                        territoryToDelete = null
                    }
                ) { Text("Удалить", color = EcoColors.Score4) }
            },
            dismissButton = {
                TextButton(onClick = { territoryToDelete = null }) { Text("Отмена") }
            }
        )
    }

    // Safe Two-Step Clear Journal Confirmation (with Export offer)
    if (showClearStep1) {
        AlertDialog(
            onDismissRequest = { showClearStep1 = false },
            title = { Text("Очистить весь журнал?") },
            text = { Text("Рекомендуется сначала сохранить резервную копию данных в JSON. Все территории и фотографии листьев будут удалены.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearStep1 = false
                    showClearStep2 = true
                }) { Text("Продолжить", color = EcoColors.Score4) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        exportLauncher.launch("betula_backup_$dateStr.json")
                    }) { Text("Экспорт JSON") }
                    TextButton(onClick = { showClearStep1 = false }) { Text("Отмена") }
                }
            }
        )
    }

    if (showClearStep2) {
        AlertDialog(
            onDismissRequest = { showClearStep2 = false },
            title = { Text("Подтвердите удаление") },
            text = { Text("Это действие необратимо удалит все записи из локальной базы данных.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllJournalData()
                        showClearStep2 = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Score4)
                ) { Text("Удалить всё") }
            },
            dismissButton = {
                TextButton(onClick = { showClearStep2 = false }) { Text("Отмена") }
            }
        )
    }
}
