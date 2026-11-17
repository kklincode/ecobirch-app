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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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
import ru.ecobirch.fa.app.domain.math.LeafBioindicationEngine
import ru.ecobirch.fa.app.ui.theme.EcoColors

@Composable
fun MethodologyScreen(
    onOpenSettings: () -> Unit
) {
    var activeTab by remember { mutableStateOf("theory") } // theory | traits | scale | birch
    var selectedTrait by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Editorial Header with prominent Settings button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Методика оценки",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp,
                    color = EcoColors.Slate900
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "В.М. Захаров · Betula pendula",
                    fontSize = 13.5.sp,
                    color = EcoColors.Slate500
                )
            }
            Surface(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(99.dp),
                color = EcoColors.SurfaceTonal
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Настройки",
                        tint = EcoColors.Slate700,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Настройки",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcoColors.Slate900
                    )
                }
            }
        }

        // Modern Pill Segmented Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EcoColors.SurfaceTonal)
                .padding(4.dp),
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) Color.White else Color.Transparent)
                        .clickable { activeTab = id }
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

        when (activeTab) {
            "theory" -> TheoryTab()
            "traits" -> TraitsTab(selectedTrait = selectedTrait, onSelectTrait = { selectedTrait = it })
            "scale" -> ScaleTab()
            "birch" -> BirchTab()
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun TheoryTab() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoCard(
            title = "1. Цифровая морфометрия и FA",
            body = "Приложение измеряет 12 биологических ориентиров листа берёзы повислой (Betula pendula Roth.) и рассчитывает интегральный индекс относительной асимметрии Z по 5 парным признакам.\n\n" +
                "Важно: сам по себе индекс Z ≠ «уровень загрязнения воздуха» и ≠ статистически доказанная флуктуирующая асимметрия (FA). Прежде чем говорить о FA, система проверяет каждый признак на направленную асимметрию (DA), антисимметрию (AS), размерную зависимость и ошибку измерения (ME)."
        )
        InfoCard(
            title = "2. Иерархический сбор и слепые замеры",
            body = "• Иерархия «Участок → Дерево → Лист»: листья с одного дерева коррелированы. Для исключения псевдорепликации фиксируйте treeId минимум по 5–10 деревьям на участке (укороченные побеги, высота 1,5–2 м, без повреждений).\n" +
                "• Слепой режим (Kozlov 2015, 2025): знание происхождения образца вызывает подтверждающее смещение (confirmation bias). Включите «Слепой режим измерений» в Настройках и выполняйте повторные слепые замеры (ME) в Журнале."
        )
        InfoCard(
            title = "3. Формулы: знаковая и модульная",
            body = "Для каждого признака сохраняются исходные значения X_л, X_п, знаковая относительная асимметрия A_s = (X_л − X_п)/(X_л + X_п) (для проверки DA и AS) и модульный индекс Y_i = |A_s|. Индекс листа: Z = (Y1 + Y2 + Y3 + Y4 + Y5)/5."
        )
        InfoCard(
            title = "4. Ограничения метода и литература",
            body = "Мета-анализ Májeková et al. (2024, Functional Ecology, DOI: 10.1111/1365-2435.14564) показал отсутствие связи со стрессом в 53% пар «вид × стресс». Adamovich & Olkova (2024) выявили высокие значения (0.064–0.076) на участках без превышения ПДК в связи с кислотностью и составом почв, а Erofeeva & Yakimov (2020) — смену типов асимметрии (DA/AS) у B. pendula.\n\n" +
                "База: Palmer & Strobeck (1986); Захаров и др. (2000); Kozlov (2015, 2017, 2025); Graham (2021, DOI: 10.3390/sym13010009)."
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.18f)
                .clip(RoundedCornerShape(22.dp))
                .background(EcoColors.Slate950)
                .padding(18.dp),
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
                drawPath(outline, Color(0xFF22C55E), style = Stroke(width = 3.2f))

                drawLine(Color(0xFF4ADE80), p(50f, 88f), p(50f, 10f), strokeWidth = 4f)
                drawLine(Color(0xFF4ADE80).copy(alpha = 0.65f), p(50f, 70f), p(22f, 52f), strokeWidth = 3f)
                drawLine(Color(0xFF4ADE80).copy(alpha = 0.65f), p(50f, 70f), p(78f, 52f), strokeWidth = 3f)
                drawLine(Color(0xFF4ADE80).copy(alpha = 0.75f), p(50f, 50f), p(26f, 32f), strokeWidth = 3f)
                drawLine(Color(0xFF4ADE80).copy(alpha = 0.75f), p(50f, 50f), p(74f, 32f), strokeWidth = 3f)

                val highlight = Color(0xFFFACC15)
                val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                when (selectedTrait) {
                    1 -> {
                        drawLine(highlight, p(50f, 54f), p(15f, 54f), strokeWidth = 5.5f, pathEffect = dash)
                        drawLine(highlight, p(50f, 54f), p(85f, 54f), strokeWidth = 5.5f, pathEffect = dash)
                    }
                    2 -> {
                        drawLine(highlight, p(50f, 50f), p(26f, 32f), strokeWidth = 6f)
                        drawLine(highlight, p(50f, 50f), p(74f, 32f), strokeWidth = 6f)
                    }
                    3 -> {
                        drawLine(highlight, p(50f, 70f), p(50f, 50f), strokeWidth = 7f)
                        drawCircle(highlight, 10f, p(50f, 70f))
                        drawCircle(highlight, 10f, p(50f, 50f))
                    }
                    4 -> {
                        drawLine(highlight, p(22f, 52f), p(26f, 32f), strokeWidth = 5.5f, pathEffect = dash)
                        drawLine(highlight, p(78f, 52f), p(74f, 32f), strokeWidth = 5.5f, pathEffect = dash)
                    }
                    5 -> {
                        drawLine(highlight, p(50f, 50f), p(50f, 22f), strokeWidth = 5.5f)
                        drawLine(highlight, p(50f, 50f), p(26f, 32f), strokeWidth = 5.5f)
                        drawLine(highlight, p(50f, 50f), p(74f, 32f), strokeWidth = 5.5f)
                    }
                }
            }
            Surface(
                color = EcoColors.Emerald900,
                shape = RoundedCornerShape(99.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = "Признак $selectedTrait",
                    color = EcoColors.Emerald100,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }

        traitDescriptions.forEach { (id, pair) ->
            val isSel = selectedTrait == id
            Surface(
                onClick = { onSelectTrait(id) },
                shape = RoundedCornerShape(18.dp),
                color = if (isSel) EcoColors.Emerald50 else Color.White,
                shadowElevation = if (isSel) 0.dp else 1.dp,
                border = if (isSel) androidx.compose.foundation.BorderStroke(1.5.dp, EcoColors.Emerald600) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(pair.first, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EcoColors.Slate900)
                    Text(pair.second, fontSize = 13.5.sp, lineHeight = 19.sp, color = EcoColors.Slate700)
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoCard(
            title = "Справочная 5-балльная шкала Захарова (Betula pendula)",
            body = "Пороги В.М. Захарова и др. (2000) приведены как региональный справочный ориентир и по умолчанию отключены в настройках (режим чистой морфометрии). Показатели <0.040 .. >0.054 не являются универсальным доказательством загрязнения воздуха без локального контроля и проверки DA/AS/ME (Májeková et al. 2024; Adamovich & Olkova 2024)."
        )

        scores.forEach { r ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        color = EcoColors.scoreColor(r.score),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${r.score}",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = EcoColors.scoreTextColor(r.score)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(r.status, fontWeight = FontWeight.Bold, fontSize = 15.5.sp, color = EcoColors.Slate900)
                            Surface(
                                color = EcoColors.SurfaceTonal,
                                shape = RoundedCornerShape(99.dp)
                            ) {
                                Text(
                                    text = r.rangeLabel,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = EcoColors.Slate700,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(r.description, fontSize = 13.5.sp, lineHeight = 19.sp, color = EcoColors.Slate500)
                    }
                }
            }
        }
    }
}

@Composable
private fun BirchTab() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = EcoColors.Slate900)
            Text(body, fontSize = 14.5.sp, color = EcoColors.Slate700, lineHeight = 22.sp)
        }
    }
}
