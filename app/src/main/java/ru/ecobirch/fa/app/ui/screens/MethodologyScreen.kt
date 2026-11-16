package ru.ecobirch.fa.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ecobirch.fa.app.data.local.AppSettings
import ru.ecobirch.fa.app.domain.math.LeafBioindicationEngine
import ru.ecobirch.fa.app.ui.components.SettingsPrivacyDialog
import ru.ecobirch.fa.app.ui.state.MainViewModel
import ru.ecobirch.fa.app.ui.theme.EcoColors

@Composable
fun MethodologyScreen(
    settings: AppSettings,
    lastCrashLog: String?,
    viewModel: MainViewModel
) {
    var activeTab by remember { mutableStateOf("theory") } // theory | traits | scale | birch
    var selectedTrait by remember { mutableIntStateOf(1) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with Settings/Privacy button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EcoColors.Emerald50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Emerald100)
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = EcoColors.Emerald600, modifier = Modifier.padding(10.dp))
                }
                Column {
                    Text("Методика оценки", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = EcoColors.Slate900)
                    Text("В.М. Захаров · Betula pendula", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = EcoColors.Slate500)
                }
            }
            IconButton(onClick = { showSettingsDialog = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Настройки и приватность", tint = EcoColors.Slate700)
            }
        }

        // 4 Sub-tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(EcoColors.Slate200.copy(alpha = 0.7f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "theory" to "Суть",
                "traits" to "Признаки",
                "scale" to "Шкала",
                "birch" to "Берёза"
            ).forEach { (id, title) ->
                val selected = activeTab == id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selected) Color.White else Color.Transparent)
                        .clickable { activeTab = id }
                        .padding(vertical = 8.dp),
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

        when (activeTab) {
            "theory" -> TheoryTab()
            "traits" -> TraitsTab(selectedTrait = selectedTrait, onSelectTrait = { selectedTrait = it })
            "scale" -> ScaleTab()
            "birch" -> BirchTab()
        }
    }

    if (showSettingsDialog) {
        SettingsPrivacyDialog(
            settings = settings,
            lastCrashLog = lastCrashLog,
            onAcceptPrivacy = viewModel::setPrivacyAccepted,
            onSaveGeminiConfig = viewModel::updateGeminiConfig,
            onSetIncludeDemoExport = viewModel::setIncludeDemoInExport,
            onDismissCrashLog = viewModel::dismissCrashLog,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun TheoryTab() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoCard(
            title = "1. Цель биоиндикации",
            body = "Метод основан на измерении флуктуирующей асимметрии (FA) — мелких ненаправленных отклонений от билатеральной симметрии листовой пластины берёзы повислой (Betula pendula Roth.). Антропогенный стресс (выхлопы, тяжёлые металлы, смог) нарушает стабильность развития растения, увеличивая интегральный показатель FA."
        )
        InfoCard(
            title = "2. Правила сбора листьев",
            body = "• Сбор ведётся с нижней части кроны деревьев на высоте 1,5–2 м.\n" +
                "• Берут листья только с укороченных побегов, без механических повреждений и вредителей.\n" +
                "• Для достоверной оценки территории по методике Захарова необходимо собрать не менее 10 листьев с 10 деревьев (в идеале 100 листьев — по 10 с каждого из 10 деревьев). Выборка n < 10 в приложении маркируется как предварительная."
        )
        InfoCard(
            title = "3. Нормированная формула",
            body = "Для каждого из 5 признаков рассчитывается относительная разница: Y_i = |X_лi - X_пi| / (X_лi + X_пi). Это полностью устраняет влияние абсолютного размера листа — сравнивать можно как крупные, так и мелкие листья без искажения результата."
        )
    }
}

@Composable
private fun TraitsTab(selectedTrait: Int, onSelectTrait: (Int) -> Unit) {
    val traitDescriptions = listOf(
        1 to ("Признак 1: Ширина половинок" to "Перпендикуляр от центральной жилки до точки максимальной ширины левой и правой половин пластины."),
        2 to ("Признак 2: Длина 2-й жилки" to "Длина второй от основания листа боковой жилки второго порядка от места ответвления до зубчатого края."),
        3 to ("Признак 3: Расстояние между основаниями" to "Шаг вдоль главной жилки между точками отхождения 1-й и 2-й боковых жилок."),
        4 to ("Признак 4: Расстояние между краями жилок" to "Прямое расстояние по краю листовой пластины между окончаниями 1-й и 2-й боковых жилок."),
        5 to ("Признак 5: Угол 2-й боковой жилки" to "Угол (в градусах) между направлением центральной главной жилки (основание → вершина) и 2-й боковой жилкой.")
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Interactive Vector Leaf Diagram
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.15f)
                .clip(RoundedCornerShape(18.dp))
                .background(EcoColors.Slate950)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                fun p(px: Float, py: Float) = Offset(w * px / 100f, h * py / 100f)

                val outline = Path().apply {
                    moveTo(p(50f, 88f).x, p(50f, 88f).y)
                    cubicTo(p(35f, 88f).x, p(35f, 88f).y, p(16f, 70f).x, p(16f, 70f).y, p(15f, 52f).x, p(15f, 52f).y)
                    cubicTo(p(14f, 34f).x, p(14f, 34f).y, p(32f, 20f).x, p(32f, 20f).y, p(50f, 10f).x, p(50f, 10f).y)
                    cubicTo(p(68f, 20f).x, p(68f, 20f).y, p(86f, 34f).x, p(86f, 34f).y, p(85f, 52f).x, p(85f, 52f).y)
                    cubicTo(p(84f, 70f).x, p(84f, 70f).y, p(65f, 88f).x, p(65f, 88f).y, p(50f, 88f).x, p(50f, 88f).y)
                    close()
                }
                drawPath(outline, Color(0xFF052E16))
                drawPath(outline, Color(0xFF22C55E), style = Stroke(width = 3f))

                // Main midrib & lateral veins
                drawLine(Color(0xFF4ADE80), p(50f, 88f), p(50f, 10f), strokeWidth = 4f)
                drawLine(Color(0xFF4ADE80).copy(alpha = 0.65f), p(50f, 70f), p(22f, 52f), strokeWidth = 3f)
                drawLine(Color(0xFF4ADE80).copy(alpha = 0.65f), p(50f, 70f), p(78f, 52f), strokeWidth = 3f)
                drawLine(Color(0xFF4ADE80).copy(alpha = 0.75f), p(50f, 50f), p(26f, 32f), strokeWidth = 3f)
                drawLine(Color(0xFF4ADE80).copy(alpha = 0.75f), p(50f, 50f), p(74f, 32f), strokeWidth = 3f)

                val highlight = Color(0xFFFACC15)
                val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                when (selectedTrait) {
                    1 -> {
                        drawLine(highlight, p(50f, 54f), p(15f, 54f), strokeWidth = 5f, pathEffect = dash)
                        drawLine(highlight, p(50f, 54f), p(85f, 54f), strokeWidth = 5f, pathEffect = dash)
                    }
                    2 -> {
                        drawLine(highlight, p(50f, 50f), p(26f, 32f), strokeWidth = 6f)
                        drawLine(highlight, p(50f, 50f), p(74f, 32f), strokeWidth = 6f)
                    }
                    3 -> {
                        drawLine(highlight, p(50f, 70f), p(50f, 50f), strokeWidth = 7f)
                        drawCircle(highlight, 9f, p(50f, 70f))
                        drawCircle(highlight, 9f, p(50f, 50f))
                    }
                    4 -> {
                        drawLine(highlight, p(22f, 52f), p(26f, 32f), strokeWidth = 5f, pathEffect = dash)
                        drawLine(highlight, p(78f, 52f), p(74f, 32f), strokeWidth = 5f, pathEffect = dash)
                    }
                    5 -> {
                        drawLine(highlight, p(50f, 50f), p(50f, 22f), strokeWidth = 5f)
                        drawLine(highlight, p(50f, 50f), p(26f, 32f), strokeWidth = 5f)
                        drawLine(highlight, p(50f, 50f), p(74f, 32f), strokeWidth = 5f)
                    }
                }
            }
            Text(
                text = "Признак $selectedTrait",
                color = EcoColors.Emerald100,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(EcoColors.Slate900, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        traitDescriptions.forEach { (id, pair) ->
            val isSel = selectedTrait == id
            Surface(
                onClick = { onSelectTrait(id) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSel) EcoColors.Emerald50 else Color.White,
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSel) 1.5.dp else 1.dp,
                    color = if (isSel) EcoColors.Emerald600 else EcoColors.Slate200
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(pair.first, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EcoColors.Slate900)
                    Text(pair.second, fontSize = 11.sp, color = EcoColors.Slate700)
                }
            }
        }
    }
}

@Composable
private fun ScaleTab() {
    val scores = listOf(0.030, 0.042, 0.047, 0.052, 0.060).map {
        LeafBioindicationEngine.getEnvironmentalRating(it)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoCard(
            title = "Классическая 5-балльная шкала Захарова (Betula pendula)",
            body = "Интегральный показатель флуктуирующей асимметрии (FA) вычисляется как среднее арифметическое показателей Z всех собранных листьев на территории (рекомендуется n ≥ 10 листьев)."
        )

        scores.forEach { r ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = EcoColors.scoreColor(r.score),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${r.score}",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = EcoColors.scoreTextColor(r.score)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(r.status, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EcoColors.Slate900)
                            Text(
                                text = r.rangeLabel,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = EcoColors.Slate700
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(r.description, fontSize = 11.sp, color = EcoColors.Slate500)
                    }
                }
            }
        }
    }
}

@Composable
private fun BirchTab() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoCard(
            title = "Как отличить берёзу повислую (Betula pendula)",
            body = "• Поникшие тонкие молодые ветви («плакучая» крона).\n" +
                "• Молодые побеги голые, красно-бурые, покрыты мелкими смолистыми желёзками («бородавками»).\n" +
                "• Листья треугольно-яйцевидные или ромбические, с заострённой верхушкой, дважды пильчатым краем и широким клиновидным основанием."
        )
        InfoCard(
            title = "Почему нельзя брать берёзу пушистую (B. pubescens)",
            body = "Листья берёзы пушистой (с мягким опушением на молодых побегах и округлым основанием пластинки) имеют иную норму генетической изменчивости и не сопоставимы со шкалой Захарова для Betula pendula."
        )
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EcoColors.Slate900)
            Text(body, fontSize = 12.sp, color = EcoColors.Slate700, lineHeight = 18.sp)
        }
    }
}
