package ru.ecobirch.fa.app.ui.components

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
    onDismissCrashLog: () -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var useGemini by remember(settings.useCloudGeminiAi) { mutableStateOf(settings.useCloudGeminiAi) }
    var privacyAccepted by remember(settings.privacyPolicyAccepted) { mutableStateOf(settings.privacyPolicyAccepted) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Приватность и настройки",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = EcoColors.Slate900
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = EcoColors.Emerald50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Emerald100)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Политика конфиденциальности (152-ФЗ)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = EcoColors.Emerald900)
                        Text(
                            text = "• Все замеры, координаты и фотографии хранятся локально на вашем устройстве (SQLite Room + внутренняя память).\n" +
                                "• По умолчанию приложение работает 100% офлайн с локальным алгоритмом поиска контура листа.\n" +
                                "• Сетевые запросы выполняются только к OpenStreetMap/Nominatim (при загрузке карты и поиске адресов) или к Google Gemini API, если вы добровольно включите опцию ниже и укажете свой API-ключ.",
                            fontSize = 11.sp,
                            color = EcoColors.Slate700,
                            lineHeight = 16.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = privacyAccepted,
                                onCheckedChange = {
                                    privacyAccepted = it
                                    onAcceptPrivacy(it)
                                }
                            )
                            Text(
                                text = "Ознакомлен(а) с условиями обработки данных",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EcoColors.Slate900
                            )
                        }
                    }
                }

                HorizontalDivider(color = EcoColors.Slate200)

                Text("Опциональный ИИ Gemini Vision", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EcoColors.Slate900)
                Text(
                    text = "При включении и наличии согласия фото листа передаётся в Google Gemini API для автоматической расстановки 12 точек. При отключении или отсутствии сети используется локальный анализатор.",
                    fontSize = 11.sp,
                    color = EcoColors.Slate500
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Включить Gemini Vision", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = useGemini,
                        enabled = privacyAccepted,
                        onCheckedChange = { useGemini = it }
                    )
                }

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Gemini API Key (необязательно)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = EcoColors.Slate200)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Включать демо-данные в экспорт JSON", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("По умолчанию демо-территории изолированы", fontSize = 10.sp, color = EcoColors.Slate500)
                    }
                    Switch(
                        checked = settings.includeDemoInExport,
                        onCheckedChange = onSetIncludeDemoExport
                    )
                }

                if (!lastCrashLog.isNullOrBlank()) {
                    HorizontalDivider(color = EcoColors.Slate200)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF1F2)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Диагностика последнего сбоя (данные журнала сохранены)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = EcoColors.Score5)
                            Text(
                                text = lastCrashLog.take(360),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = EcoColors.Slate700
                            )
                            OutlinedButton(onClick = onDismissCrashLog) {
                                Text("Скрыть отчёт", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Закрыть")
                    }
                    Button(
                        onClick = {
                            onSaveGeminiConfig(useGemini && privacyAccepted, apiKey)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EcoColors.Emerald600),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}
