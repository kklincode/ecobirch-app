package ru.ecobirch.fa.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ru.ecobirch.fa.app.domain.math.LeafBioindicationEngine
import ru.ecobirch.fa.app.domain.model.LeafAnalysis
import ru.ecobirch.fa.app.domain.model.Territory
import ru.ecobirch.fa.app.ui.theme.EcoColors

@Composable
fun EcoPassportDialog(
    territory: Territory,
    leaf: LeafAnalysis,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val rating = LeafBioindicationEngine.getEnvironmentalRating(leaf.zScore)
    val unit = if (leaf.axisReferenceLengthMm != null) "мм" else "% оси"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Эко-паспорт листа",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = EcoColors.Slate900
                )
                Text(
                    text = territory.name,
                    fontSize = 12.sp,
                    color = EcoColors.Slate500
                )
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(EcoColors.scoreColor(rating.score))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "БАЛЛ ${rating.score} · ${rating.status}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = EcoColors.scoreTextColor(rating.score)
                            )
                            Text(
                                text = "Шкала Захарова: ${rating.rangeLabel}",
                                fontSize = 11.sp,
                                color = EcoColors.scoreTextColor(rating.score).copy(alpha = 0.88f)
                            )
                        }
                        Text(
                            text = "Z = ${"%.4f".format(leaf.zScore)}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = EcoColors.scoreTextColor(rating.score)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Морфометрия (Y1–Y5):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EcoColors.Slate700)
                Spacer(modifier = Modifier.height(6.dp))

                val rows = listOf(
                    Triple("Y1 Ширина половинок", "${"%.1f".format(leaf.traits.t1Width.xLeft)} / ${"%.1f".format(leaf.traits.t1Width.xRight)} $unit", leaf.traits.t1Width.yDiff),
                    Triple("Y2 Длина 2-й жилки", "${"%.1f".format(leaf.traits.t2VeinLen.xLeft)} / ${"%.1f".format(leaf.traits.t2VeinLen.xRight)} $unit", leaf.traits.t2VeinLen.yDiff),
                    Triple("Y3 Осн. 1–2 жилок", "${"%.1f".format(leaf.traits.t3VeinDist.xLeft)} / ${"%.1f".format(leaf.traits.t3VeinDist.xRight)} $unit", leaf.traits.t3VeinDist.yDiff),
                    Triple("Y4 Края 1–2 жилок", "${"%.1f".format(leaf.traits.t4EndDist.xLeft)} / ${"%.1f".format(leaf.traits.t4EndDist.xRight)} $unit", leaf.traits.t4EndDist.yDiff),
                    Triple("Y5 Угол 2-й жилки", "${"%.1f".format(leaf.traits.t5Angle.xLeft)}° / ${"%.1f".format(leaf.traits.t5Angle.xRight)}°", leaf.traits.t5Angle.yDiff)
                )

                rows.forEachIndexed { idx, (label, dims, y) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = EcoColors.Slate900)
                            Text("Л/П: $dims", fontSize = 10.sp, color = EcoColors.Slate500)
                        }
                        Text(
                            text = "%.4f".format(y),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = EcoColors.Slate900
                        )
                    }
                    if (idx < rows.lastIndex) HorizontalDivider(color = EcoColors.Slate100)
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Координаты: ${"%.5f".format(territory.latitude)}, ${"%.5f".format(territory.longitude)}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = EcoColors.Slate500
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Закрыть")
                    }
                    Button(
                        onClick = onShare,
                        colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Emerald600),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text("Поделиться")
                    }
                }
            }
        }
    }
}
