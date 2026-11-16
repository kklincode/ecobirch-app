package ru.ecobirch.fa.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ecobirch.fa.app.domain.math.LeafBioindicationEngine
import ru.ecobirch.fa.app.domain.model.LeafAnalysis
import ru.ecobirch.fa.app.domain.model.Territory
import ru.ecobirch.fa.app.ui.theme.EcoColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
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

    val stats = remember(activeLeaves) {
        LeafBioindicationEngine.calculateTerritoryStatistics(activeLeaves)
    }

    val dailyData = remember(activeLeaves) { buildLast7Days(activeLeaves) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Territory Filter Chips
        if (territories.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedTerritoryId == "all",
                        onClick = { selectedTerritoryId = "all"; selectedDayIndex = null },
                        label = { Text("Все территории", fontSize = 11.sp) },
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
                        label = {
                            Text(
                                text = terr.name.take(24) + if (terr.name.length > 24) "…" else "",
                                fontSize = 11.sp
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

        // KPI Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiCard(
                title = "Среднее FA ± SE",
                value = if (stats.sampleCount > 0) "${"%.4f".format(stats.meanFA)}" else "—",
                sub = if (stats.sampleCount >= 2) "± ${"%.4f".format(stats.standardError)}" else "Замеров: ${stats.sampleCount}",
                accent = EcoColors.scoreColor(stats.indicativeRating?.score),
                modifier = Modifier.weight(1.2f)
            )
            KpiCard(
                title = "Выборка (n)",
                value = "${stats.sampleCount}",
                sub = if (stats.isReliable) "Надёжная (n ≥ 10)" else if (stats.sampleCount > 0) "Предв. (n < 10)" else "Нет замеров",
                accent = if (stats.isReliable) EcoColors.Emerald600 else EcoColors.Score3,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "95% ДИ",
                value = if (stats.sampleCount >= 2) "±${"%.3f".format(stats.ci95HalfWidth)}" else "—",
                sub = if (stats.sampleCount > 0) "SD ${"%.4f".format(stats.standardDeviation)}" else "Нет данных",
                accent = EcoColors.Slate700,
                modifier = Modifier.weight(1f)
            )
        }

        if (activeLeaves.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Нет данных за последние 7 дней",
                        fontWeight = FontWeight.Bold,
                        color = EcoColors.Slate900
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Отсканируйте листья берёзы или загрузите демонстрационный набор с эталонами Захарова.",
                        fontSize = 12.sp,
                        color = EcoColors.Slate500
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        onClick = onLoadDemoData,
                        shape = RoundedCornerShape(12.dp),
                        color = EcoColors.Emerald50,
                        border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Emerald600)
                    ) {
                        Text(
                            text = "Загрузить демо-данные (3 зоны × 14 листьев)",
                            color = EcoColors.Emerald900,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }
                }
            }
        } else {
            // Chart Mode Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(EcoColors.Slate200.copy(alpha = 0.7f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("fa" to "Тренд FA", "traits" to "Признаки Y1–Y5", "volume" to "Объём (n)").forEach { (id, title) ->
                    val selected = chartMode == id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selected) Color.White else Color.Transparent)
                            .clickable { chartMode = id }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) EcoColors.Emerald700 else EcoColors.Slate700
                        )
                    }
                }
            }

            // Chart Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    when (chartMode) {
                        "fa" -> {
                            Text("Динамика интегрального FA (7 дней)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Пунктир: 0.040 (Норма I) и 0.054 (Критический порог V)", fontSize = 10.sp, color = EcoColors.Slate500)
                            Spacer(modifier = Modifier.height(10.dp))
                            FATrendLineChart(
                                data = dailyData,
                                selectedIndex = selectedDayIndex,
                                onSelectDay = { selectedDayIndex = it }
                            )
                        }
                        "traits" -> {
                            Text("Средние коэффициенты по признакам Y1–Y5", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Вклад каждого морфологического признака в итоговый балл", fontSize = 10.sp, color = EcoColors.Slate500)
                            Spacer(modifier = Modifier.height(10.dp))
                            TraitsBreakdownBars(activeLeaves)
                        }
                        else -> {
                            Text("Количество замеров по дням", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Для достоверного балла территории рекомендуется n ≥ 10", fontSize = 10.sp, color = EcoColors.Slate500)
                            Spacer(modifier = Modifier.height(10.dp))
                            SampleVolumeChart(dailyData)
                        }
                    }
                }
            }

            // Selected Day Details Card
            val day = selectedDayIndex?.let { dailyData.getOrNull(it) }
            if (day != null && day.avgFA != null && chartMode == "fa") {
                val r = LeafBioindicationEngine.getEnvironmentalRating(day.avgFA)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = EcoColors.Emerald50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Emerald100),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${day.label} (${day.weekday})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EcoColors.Slate900)
                            Text("Листьев: ${day.count} · Балл ${r.score} (${r.status})", fontSize = 11.sp, color = EcoColors.Slate700)
                        }
                        Text(
                            text = "%.4f".format(day.avgFA),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = EcoColors.scoreColor(r.score)
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
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Slate200)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontSize = 10.sp, color = EcoColors.Slate500, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = accent
            )
            Text(sub, fontSize = 10.sp, color = EcoColors.Slate500, maxLines = 1)
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
                .height(175.dp)
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val stepX = size.width / max(1, data.size)
                        val idx = (offset.x / stepX).toInt().coerceIn(0, data.lastIndex)
                        onSelectDay(idx)
                    }
                }
        ) {
            val leftPad = 14f
            val rightPad = 14f
            val topPad = 14f
            val bottomPad = 14f
            val plotW = size.width - leftPad - rightPad
            val plotH = size.height - topPad - bottomPad

            fun yFor(v: Double): Float {
                val frac = (v / maxVal).coerceIn(0.0, 1.0)
                return topPad + plotH * (1f - frac.toFloat())
            }

            // Threshold lines: 0.040 (Norm) & 0.054 (Critical)
            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            val yNorm = yFor(0.040)
            drawLine(EcoColors.Score1.copy(alpha = 0.7f), Offset(leftPad, yNorm), Offset(size.width - rightPad, yNorm), strokeWidth = 2f, pathEffect = dash)
            val yCrit = yFor(0.054)
            drawLine(EcoColors.Score4.copy(alpha = 0.7f), Offset(leftPad, yCrit), Offset(size.width - rightPad, yCrit), strokeWidth = 2f, pathEffect = dash)

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
                        lineTo(validPts[i].second.x, validPts[i].second.y)
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
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
            }

            validPts.forEach { (idx, pos, fa) ->
                val r = LeafBioindicationEngine.getEnvironmentalRating(fa)
                val dotColor = EcoColors.scoreColor(r.score)
                val isSel = idx == selectedIndex
                drawCircle(Color.White, radius = if (isSel) 14f else 10f, center = pos)
                drawCircle(dotColor, radius = if (isSel) 10f else 7f, center = pos)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            data.forEachIndexed { idx, d ->
                Text(
                    text = d.label,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (selectedIndex == idx) FontWeight.Bold else FontWeight.Normal,
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
        "Y1 Ширина" to leaves.sumOf { it.traits.t1Width.yDiff } / n,
        "Y2 2-я жилка" to leaves.sumOf { it.traits.t2VeinLen.yDiff } / n,
        "Y3 Осн. жилок" to leaves.sumOf { it.traits.t3VeinDist.yDiff } / n,
        "Y4 Кон. жилок" to leaves.sumOf { it.traits.t4EndDist.yDiff } / n,
        "Y5 Угол" to leaves.sumOf { it.traits.t5Angle.yDiff } / n
    )
    val maxVal = max(0.06, items.maxOf { it.second } * 1.15)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (label, value) ->
            val ratio = (value / maxVal).toFloat().coerceIn(0.03f, 1f)
            val barColor = EcoColors.scoreColor(LeafBioindicationEngine.getEnvironmentalRating(value).score)
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = EcoColors.Slate700)
                    Text("%.4f".format(value), fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(EcoColors.Slate100)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .height(8.dp)
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
        Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            val barSpace = size.width / max(1, data.size)
            val barW = barSpace * 0.54f
            data.forEachIndexed { idx, d ->
                val h = (d.count.toFloat() / maxCount.toFloat()) * (size.height - 16f)
                val x = idx * barSpace + (barSpace - barW) / 2f
                val y = size.height - h
                drawRoundRect(
                    color = if (d.count >= 10) EcoColors.Emerald600 else EcoColors.Score3,
                    topLeft = Offset(x, y),
                    size = Size(barW, h.coerceAtLeast(4f)),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            data.forEach { d ->
                Text(
                    text = "${d.label}\n(${d.count})",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = EcoColors.Slate700
                )
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
            label = if (offset == 0) "Сегодня" else labelFmt.format(date),
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
