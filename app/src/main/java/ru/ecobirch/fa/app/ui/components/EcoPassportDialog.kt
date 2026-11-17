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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    showZakharovScale: Boolean,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val rating = LeafBioindicationEngine.getEnvironmentalRating(leaf.zScore)
    val unit = if (leaf.axisReferenceLengthMm != null) "мм" else "% оси"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Морфометрический паспорт листа",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = EcoColors.Slate900
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${territory.name} · ${leaf.treeLabel}",
                    fontSize = 13.5.sp,
                    color = EcoColors.Slate500
                )
                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (showZakharovScale) EcoColors.scoreColor(rating.score) else EcoColors.Emerald50
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val titleTxt = if (showZakharovScale) {
                                "ОРИЕНТИР ${rating.score} · ${rating.status}"
                            } else {
                                "ИНДЕКС АСИММЕТРИИ Z"
                            }
                            val subTxt = if (showZakharovScale) {
                                "Шкала Захарова: ${rating.rangeLabel} (1 лист не интерпретируется)"
                            } else {
                                "Чистая морфометрия (без оценки среды)"
                            }
                            val txtColor = if (showZakharovScale) {
                                EcoColors.scoreTextColor(rating.score)
                            } else {
                                EcoColors.Emerald900
                            }
                            Text(
                                text = titleTxt,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = txtColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subTxt,
                                fontSize = 12.5.sp,
                                color = txtColor.copy(alpha = 0.88f)
                            )
                        }
                        Text(
                            text = "%.4f".format(leaf.zScore),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = if (showZakharovScale) EcoColors.scoreTextColor(rating.score) else EcoColors.Emerald900
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EcoColors.SurfaceTonal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val modeText = buildString {
                        append(if (leaf.isBlindMeasurement) "Слепой замер" else "Открытый замер")
                        if (!leaf.replicateOfLeafId.isNullOrBlank()) append(" · Повтор (ME)")
                        append(if (leaf.leafIntegrityVerified) " · B. pendula OK" else " · Не проверен")
                    }
                    Text(
                        text = modeText,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EcoColors.Slate700,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Морфометрия (Y1–Y5)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = EcoColors.Slate900
                )
                Spacer(modifier = Modifier.height(8.dp))

                val rows = listOf(
                    Triple("Y1 · Ширина половинок", "${"%.1f".format(leaf.traits.t1Width.xLeft)} / ${"%.1f".format(leaf.traits.t1Width.xRight)} $unit", leaf.traits.t1Width.yDiff),
                    Triple("Y2 · Длина 2-й жилки", "${"%.1f".format(leaf.traits.t2VeinLen.xLeft)} / ${"%.1f".format(leaf.traits.t2VeinLen.xRight)} $unit", leaf.traits.t2VeinLen.yDiff),
                    Triple("Y3 · Осн. 1–2 жилок", "${"%.1f".format(leaf.traits.t3VeinDist.xLeft)} / ${"%.1f".format(leaf.traits.t3VeinDist.xRight)} $unit", leaf.traits.t3VeinDist.yDiff),
                    Triple("Y4 · Края 1–2 жилок", "${"%.1f".format(leaf.traits.t4EndDist.xLeft)} / ${"%.1f".format(leaf.traits.t4EndDist.xRight)} $unit", leaf.traits.t4EndDist.yDiff),
                    Triple("Y5 · Угол 2-й жилки", "${"%.1f".format(leaf.traits.t5Angle.xLeft)}° / ${"%.1f".format(leaf.traits.t5Angle.xRight)}°", leaf.traits.t5Angle.yDiff)
                )

                rows.forEachIndexed { idx, (label, dims, y) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = EcoColors.Slate900)
                            Text("Л/П: $dims", fontSize = 12.5.sp, color = EcoColors.Slate500)
                        }
                        Text(
                            text = "%.4f".format(y),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = EcoColors.Slate900
                        )
                    }
                    if (idx < rows.lastIndex) HorizontalDivider(color = EcoColors.Slate100)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EcoColors.SurfaceTonal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Координаты: ${"%.5f".format(territory.latitude)}, ${"%.5f".format(territory.longitude)}",
                        fontSize = 12.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = EcoColors.Slate700,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Закрыть", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onShare,
                        colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Emerald600),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.25f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 4.dp))
                        Text("Поделиться", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
