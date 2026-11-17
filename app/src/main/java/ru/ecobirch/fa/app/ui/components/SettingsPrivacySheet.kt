package ru.ecobirch.fa.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ru.ecobirch.fa.app.data.local.AppSettings
import ru.ecobirch.fa.app.ui.theme.EcoColors

@Composable
fun SettingsPrivacyDialog(
    settings: AppSettings,
    lastCrashLog: String?,
    onAcceptPrivacy: (Boolean) -> Unit,
    onSaveGeminiConfig: (Boolean, String) -> Unit,
    onSetIncludeDemoExport: (Boolean) -> Unit,
    onSetBlindMeasurementMode: (Boolean) -> Unit,
    onSetEnableZakharovScale: (Boolean) -> Unit,
    onDismissCrashLog: () -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var useGemini by remember(settings.useCloudGeminiAi) { mutableStateOf(settings.useCloudGeminiAi) }
    var privacyAccepted by remember(settings.privacyPolicyAccepted) { mutableStateOf(settings.privacyPolicyAccepted) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Приватность и настройки",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 21.sp,
                    color = EcoColors.Slate900
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EcoColors.Emerald50
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Политика конфиденциальности (152-ФЗ)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = EcoColors.Emerald900
                        )
                        Text(
                            text = "• Все замеры, координаты и фотографии хранятся локально на вашем устройстве (SQLite Room + внутренняя память).\n" +
                                "• По умолчанию приложение работает офлайн с локальным алгоритмом поиска контура листа.\n" +
                                "• Сетевые запросы идут только к OpenStreetMap/Nominatim (при загрузке карты и поиске адресов) или к Google Gemini API, если вы добровольно включите эту опцию.",
                            fontSize = 13.sp,
                            color = EcoColors.Slate700,
                            lineHeight = 19.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val next = !privacyAccepted
                                    privacyAccepted = next
                                    onAcceptPrivacy(next)
                                }
                        ) {
                            Checkbox(
                                checked = privacyAccepted,
                                onCheckedChange = {
                                    privacyAccepted = it
                                    onAcceptPrivacy(it)
                                }
                            )
                            Text(
                                text = "Ознакомлен(а) с условиями обработки данных",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EcoColors.Slate900
                            )
                        }
                    }
                }

                HorizontalDivider(color = EcoColors.Hairline)

                Text(
                    text = "Опциональный ИИ Gemini Vision",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = EcoColors.Slate900
                )
                Text(
                    text = "При включении фото листа передаётся в Google Gemini API для автоматической расстановки 12 точек. При отключении или без сети работает локальный анализатор.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = EcoColors.Slate500
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Включить Gemini Vision", fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = useGemini,
                        enabled = privacyAccepted,
                        onCheckedChange = { useGemini = it }
                    )
                }

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Gemini API Key (необязательно)", fontSize = 13.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = EcoColors.Hairline)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Включать демо-данные в экспорт JSON", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("По умолчанию демо-зоны изолированы", fontSize = 12.5.sp, color = EcoColors.Slate500)
                    }
                    Switch(
                        checked = settings.includeDemoInExport,
                        onCheckedChange = onSetIncludeDemoExport
                    )
                }

                HorizontalDivider(color = EcoColors.Hairline)

                Text(
                    text = "Научный протокол (Kozlov 2025 / Graham 2021)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = EcoColors.Slate900
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Слепой режим измерений", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Скрывать участок, Z и баллы во время расстановки точек", fontSize = 12.5.sp, color = EcoColors.Slate500)
                    }
                    Switch(
                        checked = settings.blindMeasurementMode,
                        onCheckedChange = onSetBlindMeasurementMode
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("Ориентировочная шкала Захарова (1–5)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Выключено в режиме чистой морфометрии до валидации DA/AS/ME", fontSize = 12.5.sp, color = EcoColors.Slate500)
                    }
                    Switch(
                        checked = settings.enableZakharovScale,
                        onCheckedChange = onSetEnableZakharovScale
                    )
                }

                if (!lastCrashLog.isNullOrBlank()) {
                    HorizontalDivider(color = EcoColors.Hairline)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = EcoColors.Score5Bg
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Диагностика последнего сбоя (данные журнала сохранены)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = EcoColors.Score5
                            )
                            Text(
                                text = lastCrashLog.take(360),
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = EcoColors.Slate700
                            )
                            OutlinedButton(onClick = onDismissCrashLog, shape = RoundedCornerShape(12.dp)) {
                                Text("Скрыть отчёт", fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Закрыть", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            onSaveGeminiConfig(useGemini && privacyAccepted, apiKey)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Emerald600),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.25f).height(48.dp)
                    ) {
                        Text("Сохранить", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
