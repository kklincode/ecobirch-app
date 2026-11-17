package ru.ecobirch.fa.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ecobirch.fa.app.domain.math.LeafBioindicationEngine
import ru.ecobirch.fa.app.domain.model.LeafAnalysis
import ru.ecobirch.fa.app.domain.model.Territory
import ru.ecobirch.fa.app.ui.theme.EcoColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class DailyFAPoint(
    val dateKey: String,
    val label: String,
    val weekday: String,
    val count: Int,
    val avgFA: Double?,
    val t1: Double?,
    val t2: Double?,
    val t3: Double?,
    val t4: Double?,
    val t5: Double?
)

@Composable
fun FAAnalyticsSection(
    territories: List<Territory>,
    showZakharovScale: Boolean = false,
    onLoadDemoData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTerritoryId by remember { mutableStateOf("all") }
    var chartMode by remember { mutableStateOf("fa") } // "fa" | "traits" | "volume"
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }

    val activeLeaves = remember(territories, selectedTerritoryId) {
        territories.flatMap { t ->
            if (selectedTerritoryId == "all" || t.id == selectedTerritoryId) t.leaves else emptyList()
        }
    }

    val primaryLeaves = remember(activeLeaves) {
        activeLeaves.filter { it.replicateOfLeafId.isNullOrBlank() }
    }

    val stats = remember(activeLeaves) {
        LeafBioindicationEngine.calculateTerritoryStatistics(activeLeaves)
    }

    val dailyData = remember(primaryLeaves) { buildLast7Days(primaryLeaves) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Territory Filter Chips
        if (territories.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedTerritoryId == "all",
                        onClick = { selectedTerritoryId = "all"; selectedDayIndex = null },
                        shape = RoundedCornerShape(99.dp),
                        label = { Text("Все точки", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EcoColors.Emerald600,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                items(territories) { terr ->
                    FilterChip(
                        selected = selectedTerritoryId == terr.id,
                        onClick = { selectedTerritoryId = terr.id; selectedDayIndex = null },
                        shape = RoundedCornerShape(99.dp),
                        label = {
                            Text(
                                text = terr.name.take(24) + if (terr.name.length > 24) "…" else "",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EcoColors.Emerald600,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Hero KPI Card + 2-column Secondary KPI Cards + Biostatistics Verification Card
        val rating = stats.indicativeRating
        val heroBg = if (showZakharovScale) EcoColors.scoreSoftBg(rating?.score) else EcoColors.Emerald50
        val displayMean = if (stats.treeCount >= 2) stats.treeMeanFA else stats.meanFA
        val displaySE = if (stats.treeCount >= 2) stats.treeLevelSE else stats.standardError
        val displayCi = if (stats.treeCount >= 2) stats.treeLevelCi95HalfWidth else stats.ci95HalfWidth

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = heroBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (stats.treeCount >= 2) {
                            "МЕЖДРЕВЕСНЫЙ ИНДЕКС Z"
                        } else {
                            "ИНДЕКС ОТН. АСИММЕТРИИ Z"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = EcoColors.Slate700
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (stats.primaryLeafCount > 0) "%.4f".format(displayMean) else "—",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = EcoColors.Slate900
                        )
                        if (stats.primaryLeafCount >= 2) {
                            Text(
                                text = "± ${"%.4f".format(displaySE)}",
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = EcoColors.Slate700,
                                modifier = Modifier.padding(bottom = 5.dp)
                            )
                        }
                    }
                    if (stats.primaryLeafCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (showZakharovScale && rating != null) {
                                "Ориентир Захарова: ${rating.status} (${rating.rangeLabel})"
                            } else {
                                stats.faValidationStatus
                            },
                            fontSize = 12.5.sp,
                            color = EcoColors.Slate700
                        )
                    }
                }

                if (showZakharovScale && rating != null) {
                    Surface(
                        color = EcoColors.scoreColor(rating.score),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${rating.score}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = EcoColors.scoreTextColor(rating.score)
                            )
                            Text(
                                text = "ОРИЕНТИР",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoColors.scoreTextColor(rating.score).copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiCard(
                title = "Иерархия выборки",
                value = "${stats.treeCount} дер. / ${stats.primaryLeafCount} л.",
                sub = if (stats.replicateCount > 0) "Повторов ME: ${stats.replicateCount}" else if (stats.treeCount >= 5) "≥ 5 деревьев (ок)" else "Нужно ≥ 5 деревьев",
                accent = if (stats.isHierarchicallyReliable) EcoColors.Emerald700 else EcoColors.Slate900,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "95% Дов. интервал",
                value = if (stats.primaryLeafCount >= 2) "±${"%.4f".format(displayCi)}" else "—",
                sub = if (stats.primaryLeafCount > 0) "SD ${"%.4f".format(stats.standardDeviation)}" else "Нет данных",
                accent = EcoColors.Slate900,
                modifier = Modifier.weight(1f)
            )
        }

        if (stats.primaryLeafCount > 0) {
            val daTraits = stats.traitDiagnostics.filter { it.hasDirectionalAsymmetry }.map { "Y${it.traitIndex}" }
            val asTraits = stats.traitDiagnostics.filter { it.hasAntisymmetrySuspected }.map { "Y${it.traitIndex}" }
            val sizeTraits = stats.traitDiagnostics.filter { it.hasSizeDependence }.map { "Y${it.traitIndex}" }
            val me = stats.measurementError

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Биостатистическая проверка (FA / DA / AS / ME)",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoColors.Slate900
                    )
                    Text(
                        text = "Статус: ${stats.faValidationStatus}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (stats.hasDirectionalAsymmetry || stats.hasAntisymmetry) EcoColors.Score4 else EcoColors.Emerald700
                    )
                    Text(
                        text = "• Направленная (DA): ${if (daTraits.isEmpty()) "не выявлена" else "обнаружена в ${daTraits.joinToString()}"}\n" +
                            "• Антисимметрия (AS): ${if (asTraits.isEmpty()) "не выявлена" else "подозрение в ${asTraits.joinToString()}"}\n" +
                            "• Связь с размером: ${if (sizeTraits.isEmpty()) "не выявлена" else "корреляция в ${sizeTraits.joinToString()}"}\n" +
                            "• Ошибка измерения (ME): " + if (me != null) {
                            "${"%.1f".format(me.measurementErrorPercent)}% (пар=${me.pairedLeafCount}, |ΔZ|=${"%.4f".format(me.meanAbsoluteDiffZ)})"
                        } else {
                            "нет слепых повторов"
                        },
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = EcoColors.Slate700
                    )
                }
            }
        }

        if (activeLeaves.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Пока нет замеров за 7 дней",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = EcoColors.Slate900
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Отсканируйте листья берёзы или загрузите демонстрационный набор с эталонами Захарова.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        color = EcoColors.Slate500
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onLoadDemoData,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Emerald600),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = "Загрузить демо (3 зоны × 14 листьев)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            // Pill Chart Mode Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EcoColors.SurfaceTonal)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("fa" to "Тренд FA", "traits" to "Признаки", "volume" to "Объём (n)").forEach { (id, title) ->
                    val selected = chartMode == id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Color.White else Color.Transparent)
                            .clickable { chartMode = id }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 13.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) EcoColors.Emerald800 else EcoColors.Slate700
                        )
                    }
                }
            }

            // Chart Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    when (chartMode) {
                        "fa" -> {
                            Text("Динамика индекса Z за 7 дней", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EcoColors.Slate900)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Ориентиры на графике: 0.040 и 0.054 (без учёта DA/AS)", fontSize = 12.5.sp, color = EcoColors.Slate500)
                            Spacer(modifier = Modifier.height(14.dp))
                            FATrendLineChart(
                                data = dailyData,
                                selectedIndex = selectedDayIndex,
                                onSelectDay = { selectedDayIndex = it }
                            )
                        }
                        "traits" -> {
                            Text("Признаки асимметрии Y1–Y5", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EcoColors.Slate900)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Средняя модульная асимметрия (первичные замеры)", fontSize = 12.5.sp, color = EcoColors.Slate500)
                            Spacer(modifier = Modifier.height(14.dp))
                            TraitsBreakdownBars(if (primaryLeaves.isNotEmpty()) primaryLeaves else activeLeaves)
                        }
                        else -> {
                            Text("Первичные замеры по дням", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EcoColors.Slate900)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Учитываются только первичные листья (без повторов ME)", fontSize = 12.5.sp, color = EcoColors.Slate500)
                            Spacer(modifier = Modifier.height(14.dp))
                            SampleVolumeChart(dailyData)
                        }
                    }
                }
            }

            val day = selectedDayIndex?.let { dailyData.getOrNull(it) }
            if (day != null && day.avgFA != null && chartMode == "fa") {
                val r = LeafBioindicationEngine.getEnvironmentalRating(day.avgFA)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = EcoColors.Emerald50,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${day.label} (${day.weekday})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EcoColors.Slate900)
                            val desc = if (showZakharovScale) {
                                "Листьев: ${day.count} · Ориентир ${r.score} (${r.status})"
                            } else {
                                "Первичных листьев: ${day.count}"
                            }
                            Text(desc, fontSize = 13.sp, color = EcoColors.Slate700)
                        }
                        Text(
                            text = "%.4f".format(day.avgFA),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = if (showZakharovScale) EcoColors.scoreColor(r.score) else EcoColors.Emerald700
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    sub: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 12.5.sp, color = EcoColors.Slate500, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = accent
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(sub, fontSize = 12.sp, color = EcoColors.Slate500)
        }
    }
}

@Composable
private fun FATrendLineChart(
    data: List<DailyFAPoint>,
    selectedIndex: Int?,
    onSelectDay: (Int) -> Unit
) {
    val maxVal = max(0.070, (data.mapNotNull { it.avgFA }.maxOrNull() ?: 0.060) * 1.15)
    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val stepX = size.width / max(1, data.size)
                        val idx = (offset.x / stepX).toInt().coerceIn(0, data.lastIndex)
                        onSelectDay(idx)
                    }
                }
        ) {
            val leftPad = 18f
            val rightPad = 18f
            val topPad = 18f
            val bottomPad = 18f
            val plotW = size.width - leftPad - rightPad
            val plotH = size.height - topPad - bottomPad

            fun yFor(v: Double): Float {
                val frac = (v / maxVal).coerceIn(0.0, 1.0)
                return topPad + plotH * (1f - frac.toFloat())
            }

            val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            val yNorm = yFor(0.040)
            drawLine(EcoColors.Score1.copy(alpha = 0.75f), Offset(leftPad, yNorm), Offset(size.width - rightPad, yNorm), strokeWidth = 2.5f, pathEffect = dash)
            val yCrit = yFor(0.054)
            drawLine(EcoColors.Score4.copy(alpha = 0.75f), Offset(leftPad, yCrit), Offset(size.width - rightPad, yCrit), strokeWidth = 2.5f, pathEffect = dash)

            val validPts = mutableListOf<Triple<Int, Offset, Double>>()
            val step = if (data.size > 1) plotW / (data.size - 1) else 0f
            data.forEachIndexed { idx, d ->
                val fa = d.avgFA
                if (fa != null) {
                    validPts.add(Triple(idx, Offset(leftPad + idx * step, yFor(fa)), fa))
                }
            }

            if (validPts.size >= 2) {
                val linePath = Path().apply {
                    moveTo(validPts.first().second.x, validPts.first().second.y)
                    for (i in 1 until validPts.size) {
                        val prev = validPts[i - 1].second
                        val curr = validPts[i].second
                        val ctrlX = (prev.x + curr.x) / 2f
                        cubicTo(ctrlX, prev.y, ctrlX, curr.y, curr.x, curr.y)
                    }
                }
                val areaPath = Path().apply {
                    addPath(linePath)
                    lineTo(validPts.last().second.x, topPad + plotH)
                    lineTo(validPts.first().second.x, topPad + plotH)
                    close()
                }
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        listOf(EcoColors.Emerald600.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
                drawPath(
                    path = linePath,
                    color = EcoColors.Emerald600,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
            }

            validPts.forEach { (idx, pos, fa) ->
                val r = LeafBioindicationEngine.getEnvironmentalRating(fa)
                val dotColor = EcoColors.scoreColor(r.score)
                val isSel = idx == selectedIndex
                drawCircle(Color.White, radius = if (isSel) 16f else 11f, center = pos)
                drawCircle(dotColor, radius = if (isSel) 11f else 8f, center = pos)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            data.forEachIndexed { idx, d ->
                Text(
                    text = d.label,
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (selectedIndex == idx) FontWeight.Bold else FontWeight.Medium,
                    color = if (d.avgFA != null) EcoColors.Slate700 else EcoColors.Slate400
                )
            }
        }
    }
}

@Composable
private fun TraitsBreakdownBars(leaves: List<LeafAnalysis>) {
    val n = leaves.size.coerceAtLeast(1)
    val items = listOf(
        "Y1 · Ширина половинок" to leaves.sumOf { it.traits.t1Width.yDiff } / n,
        "Y2 · Длина 2-й жилки" to leaves.sumOf { it.traits.t2VeinLen.yDiff } / n,
        "Y3 · Основания жилок" to leaves.sumOf { it.traits.t3VeinDist.yDiff } / n,
        "Y4 · Края 1–2 жилок" to leaves.sumOf { it.traits.t4EndDist.yDiff } / n,
        "Y5 · Угол 2-й жилки" to leaves.sumOf { it.traits.t5Angle.yDiff } / n
    )
    val maxVal = max(0.06, items.maxOf { it.second } * 1.15)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { (label, value) ->
            val ratio = (value / maxVal).toFloat().coerceIn(0.04f, 1f)
            val barColor = EcoColors.scoreColor(LeafBioindicationEngine.getEnvironmentalRating(value).score)
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = EcoColors.Slate900)
                    Text("%.4f".format(value), fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(EcoColors.SurfaceTonal)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .height(10.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(barColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun SampleVolumeChart(data: List<DailyFAPoint>) {
    val maxCount = max(10, data.maxOfOrNull { it.count } ?: 10)
    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(165.dp)) {
            val barSpace = size.width / max(1, data.size)
            val barW = barSpace * 0.56f
            data.forEachIndexed { idx, d ->
                val h = (d.count.toFloat() / maxCount.toFloat()) * (size.height - 16f)
                val x = idx * barSpace + (barSpace - barW) / 2f
                val y = size.height - h
                drawRoundRect(
                    color = if (d.count >= 10) EcoColors.Emerald600 else EcoColors.Score3,
                    topLeft = Offset(x, y),
                    size = Size(barW, h.coerceAtLeast(6f)),
                    cornerRadius = CornerRadius(14f, 14f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            data.forEach { d ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = d.label,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = EcoColors.Slate700
                    )
                    Text(
                        text = "${d.count}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoColors.Slate900
                    )
                }
            }
        }
    }
}

private fun buildLast7Days(leaves: List<LeafAnalysis>): List<DailyFAPoint> {
    val now = System.currentTimeMillis()
    val dayMs = 86_400_000L
    val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val labelFmt = SimpleDateFormat("dd.MM", Locale.US)
    val weekdayFmt = SimpleDateFormat("EE", Locale("ru"))

    return (6 downTo 0).map { offset ->
        val date = Date(now - offset * dayMs)
        val key = keyFmt.format(date)
        val dayLeaves = leaves.filter { it.timestamp.startsWith(key) }
        val count = dayLeaves.size
        DailyFAPoint(
            dateKey = key,
            label = if (offset == 0) "Сег." else labelFmt.format(date),
            weekday = weekdayFmt.format(date),
            count = count,
            avgFA = if (count > 0) dayLeaves.sumOf { it.zScore } / count else null,
            t1 = if (count > 0) dayLeaves.sumOf { it.traits.t1Width.yDiff } / count else null,
            t2 = if (count > 0) dayLeaves.sumOf { it.traits.t2VeinLen.yDiff } / count else null,
            t3 = if (count > 0) dayLeaves.sumOf { it.traits.t3VeinDist.yDiff } / count else null,
            t4 = if (count > 0) dayLeaves.sumOf { it.traits.t4EndDist.yDiff } / count else null,
            t5 = if (count > 0) dayLeaves.sumOf { it.traits.t5Angle.yDiff } / count else null
        )
    }
}
