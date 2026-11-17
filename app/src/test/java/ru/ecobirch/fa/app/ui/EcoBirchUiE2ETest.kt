package ru.ecobirch.fa.app.ui

import android.Manifest
import android.content.Context
import android.location.LocationManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import ru.ecobirch.fa.app.EcoBirchApplication
import ru.ecobirch.fa.app.MainActivity
import ru.ecobirch.fa.app.data.local.EcoBirchDatabase

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = EcoBirchApplication::class)
class EcoBirchUiE2ETest {

    companion object {
        @JvmStatic
        @AfterClass
        fun closeDatabaseAfterClass() {
            EcoBirchDatabase.resetInstanceForTesting()
        }
    }

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetAppState() {
        val app = composeRule.activity.application as EcoBirchApplication
        app.settingsRepository.setPrivacyPolicyAccepted(false)
        app.settingsRepository.updateGeminiConfig(false, "")
        app.settingsRepository.setIncludeDemoInExport(false)
        app.settingsRepository.setBlindMeasurementMode(false)
        app.settingsRepository.setEnableZakharovScale(false)
        runBlocking {
            app.journalRepository.clearAllUserTerritories()
        }
        composeRule.waitForIdle()
    }

    @Test
    fun navigationReconnaissanceAcrossAllTabsAndDialogs() {
        // 1. Scan screen initial state
        composeRule.onNodeWithText("Сканер листьев").assertIsDisplayed()
        composeRule.onNodeWithText("Сделать снимок").assertIsDisplayed()
        composeRule.onNodeWithText("Галерея").assertIsDisplayed()

        // 2. Methodology tab & all 4 sub-tabs
        composeRule.onNodeWithText("Методика").performClick()
        composeRule.onNodeWithText("Методика оценки").assertIsDisplayed()
        composeRule.onNodeWithText("1. Цифровая морфометрия и FA").assertIsDisplayed()

        composeRule.onNodeWithText("Признаки").performClick()
        composeRule.onNodeWithText("Признак 1: Ширина половинок").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() }
        composeRule.onNodeWithText("Признак 5: Угол 2-й боковой жилки").performScrollTo().performClick()
        composeRule.onNodeWithText("Признак 5").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Шкала").performScrollTo().performClick()
        composeRule.onNodeWithText("Справочная 5-балльная шкала Захарова (Betula pendula)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("< 0.040").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Берёза").performScrollTo().performClick()
        composeRule.onNodeWithText("Как отличить берёзу повислую (Betula pendula)").performScrollTo().assertIsDisplayed()

        // 3. Journal tab: load demo data, expand territory, open Eco-Passport, test safe 2-step clear
        composeRule.onNodeWithText("Журнал").performClick()
        composeRule.onNodeWithText("Журнал наблюдений").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("+ Демо-данные").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("+ Демо-данные").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Точки (3)").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Точки (3)").performScrollTo().performClick()
        composeRule.onNodeWithText("Ботанический сад (Демо · Контроль)").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Статус выборки: Достоверная (6 дер., n = 12 ≥ 10)")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Scroll past the Snackbar / into view, open Eco-Passport from first leaf & close it cleanly
        composeRule.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() }
        composeRule.onAllNodesWithContentDescription("Эко-паспорт")
            .onFirst()
            .performScrollTo()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("Морфометрический паспорт листа").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Морфометрический паспорт листа").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Закрыть").performScrollTo().performClick()

        // Test 2-step clear confirmation without losing data
        composeRule.onNodeWithContentDescription("Очистить журнал").performScrollTo().performClick()
        composeRule.onNodeWithText("Очистить весь журнал?").assertIsDisplayed()
        composeRule.onAllNodesWithText("Экспорт JSON").onLast().assertIsDisplayed()
        composeRule.onNodeWithText("Продолжить").performClick()
        composeRule.onNodeWithText("Подтвердите удаление").assertIsDisplayed()
        composeRule.onNodeWithText("Отмена").performClick()

        // 4. Map tab: verify pure morphometry mode by default, then enable Zakharov scale in Settings
        composeRule.onNodeWithText("Карта").performClick()
        composeRule.onNodeWithText("Морфометрия (баллы отключены)").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Добавить точку").assertIsDisplayed()

        // 5. Settings: accept 152-FZ consent and toggle optional Zakharov scale
        composeRule.onNodeWithText("Сканер").performClick()
        composeRule.onNodeWithContentDescription("Настройки и приватность").performClick()
        composeRule.onNodeWithText("Приватность и настройки").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Политика конфиденциальности (152-ФЗ)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Ознакомлен(а) с условиями обработки данных").performScrollTo().performClick()
        val switchMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
        composeRule.onAllNodes(switchMatcher)[3].performScrollTo().performClick()
        composeRule.onNodeWithText("Сохранить").performScrollTo().performClick()
        composeRule.onNodeWithText("Приватность и настройки").assertDoesNotExist()

        composeRule.onNodeWithText("Карта").performClick()
        composeRule.onNodeWithText("I <0.040").assertIsDisplayed()
        composeRule.onNodeWithText("V >.054").assertIsDisplayed()
    }

    @Test
    fun interactiveScanTemplateCalibrationAndBlindModeFlow() {
        val app = composeRule.activity.application as EcoBirchApplication
        Shadows.shadowOf(app).grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val lm = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        Shadows.shadowOf(lm).setProviderEnabled(LocationManager.GPS_PROVIDER, false)
        Shadows.shadowOf(lm).setProviderEnabled(LocationManager.NETWORK_PROVIDER, false)

        // Enable blind measurement mode first
        app.settingsRepository.setBlindMeasurementMode(true)
        composeRule.waitForIdle()

        // Open neutralized template directly on Scan screen
        composeRule.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() }
        composeRule.onNodeWithText("Низкая асимметрия (контроль)").performScrollTo().performClick()

        // Verify blind mode hides Z/Y1-Y5 readout while preserving calibration controls
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithText("Слепой режим · без подсказок").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Слепой режим · без подсказок").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Слепой режим замера активен").performScrollTo().assertIsDisplayed()

        // Tree hierarchy selector & integrity checkbox
        composeRule.onNodeWithText("+ Дерево").performScrollTo().performClick()
        composeRule.onNodeWithText("Дерево №2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("B. pendula · укороченный побег, без повреждений").performScrollTo().assertIsDisplayed()

        // Landmark chips & micro-nudge still work in blind mode
        composeRule.onNodeWithText("Основание листа (черешок)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Верш.").performClick()
        composeRule.onNodeWithText("Вершина листа (апекс)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Вправо").performScrollTo().performClick()

        // Turn off blind mode to verify unmasked Z-score & calibration before saving
        app.settingsRepository.setBlindMeasurementMode(false)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("ИНДЕКС ЛИСТА Z").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Y1 · Ширина").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Y5 · Угол жилки").performScrollTo().assertIsDisplayed()

        // Calibrate physical mm reference
        composeRule.onNodeWithText("Масштаб").performScrollTo().performClick()
        composeRule.onNodeWithText("Калибровка масштаба").assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).performTextInput("60.0")
        composeRule.onNodeWithText("Применить").performClick()
        composeRule.onNodeWithText("60.0 мм").performScrollTo().assertIsDisplayed()

        // Save triggers location chooser -> route to OSM map picker
        composeRule.onNodeWithText("Сохранить в журнал").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Куда сохранить замер?").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Выбрать на карте OSM").performClick()
        composeRule.onNodeWithText("Нажмите на карту, чтобы привязать скан листа").assertIsDisplayed()
    }

    @Test
    fun geminiSwitchRequiresPrivacyPolicyAcceptance() {
        composeRule.onNodeWithContentDescription("Настройки и приватность").performClick()
        composeRule.onNodeWithText("Включить Gemini Vision").performScrollTo().assertIsDisplayed()

        val switchMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
        val switches = composeRule.onAllNodes(switchMatcher)
        switches[0].assertIsNotEnabled()

        composeRule.onNodeWithText("Ознакомлен(а) с условиями обработки данных").performScrollTo().performClick()
        switches[0].assertIsEnabled()

        composeRule.onNodeWithText("Закрыть").performScrollTo().performClick()
        composeRule.onNodeWithText("Сканер листьев").assertIsDisplayed()
    }
}
