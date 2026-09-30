package com.maxtasy.wakku.alarms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry
import com.maxtasy.wakku.R
import com.maxtasy.wakku.ui.theme.WakkuTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class AlarmEditScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // The test device may not run in English, so look strings up instead of hardcoding them.
    private fun str(@StringRes id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun setContent(
        hour: Int = 7,
        minute: Int = 0,
        repeatDays: Set<DayOfWeek> = emptySet(),
        label: String = "",
        isNew: Boolean = true,
        useCustomSettings: Boolean = false,
        snoozeMinutes: Int = 5,
        numberOfShakes: Int = 30,
        vibrationEnabled: Boolean = true,
        gradualVolume: Boolean = false,
        soundUri: String? = null,
        onTimeChange: (Int, Int) -> Unit = { _, _ -> },
        onDaysChange: (Set<DayOfWeek>) -> Unit = {},
        onLabelChange: (String) -> Unit = {},
        onUseCustomSettingsChange: (Boolean) -> Unit = {},
        onSnoozeMinutesChange: (Int) -> Unit = {},
        onNumberOfShakesChange: (Int) -> Unit = {},
        onVibrationEnabledChange: (Boolean) -> Unit = {},
        onGradualVolumeChange: (Boolean) -> Unit = {},
        onSoundUriChange: (String?) -> Unit = {},
        onSave: () -> Unit = {},
        onDelete: () -> Unit = {},
        onBack: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            WakkuTheme {
                AlarmEditScreen(
                    hour = hour,
                    minute = minute,
                    repeatDays = repeatDays,
                    label = label,
                    isNew = isNew,
                    useCustomSettings = useCustomSettings,
                    snoozeMinutes = snoozeMinutes,
                    numberOfShakes = numberOfShakes,
                    vibrationEnabled = vibrationEnabled,
                    gradualVolume = gradualVolume,
                    soundUri = soundUri,
                    onTimeChange = onTimeChange,
                    onDaysChange = onDaysChange,
                    onLabelChange = onLabelChange,
                    onUseCustomSettingsChange = onUseCustomSettingsChange,
                    onSnoozeMinutesChange = onSnoozeMinutesChange,
                    onNumberOfShakesChange = onNumberOfShakesChange,
                    onVibrationEnabledChange = onVibrationEnabledChange,
                    onGradualVolumeChange = onGradualVolumeChange,
                    onSoundUriChange = onSoundUriChange,
                    onSave = onSave,
                    onDelete = onDelete,
                    onBack = onBack,
                )
            }
        }
    }

    @Test
    fun newAlarmShowsNewAlarmTitleAndNoDeleteAction() {
        setContent(isNew = true)
        composeTestRule.onNodeWithText(str(R.string.new_alarm)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(str(R.string.delete_alarm)).assertDoesNotExist()
    }

    @Test
    fun existingAlarmShowsEditAlarmTitleAndDeleteAction() {
        setContent(isNew = false)
        composeTestRule.onNodeWithText(str(R.string.edit_alarm)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(str(R.string.delete_alarm)).assertIsDisplayed()
    }

    @Test
    fun showsFormattedTime() {
        setContent(hour = 7, minute = 5)
        composeTestRule.onNodeWithText("07:05").assertIsDisplayed()
    }

    @Test
    fun tappingTimeOpensTimePicker() {
        setContent(hour = 7, minute = 5)
        composeTestRule.onNodeWithText(str(R.string.ok)).assertDoesNotExist()
        composeTestRule.onNodeWithTag("alarmTimeText").performClick()
        composeTestRule.onNodeWithText(str(R.string.ok)).assertIsDisplayed()
    }

    @Test
    fun customSettingsControlsHiddenByDefault() {
        setContent(useCustomSettings = false)
        composeTestRule.onNodeWithText(str(R.string.snooze_time)).assertDoesNotExist()
        composeTestRule.onNodeWithText(str(R.string.shakes_to_stop)).assertDoesNotExist()
    }

    @Test
    fun customSettingsControlsVisibleWhenEnabled() {
        setContent(useCustomSettings = true)
        composeTestRule.onNodeWithText(str(R.string.snooze_time)).assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.shakes_to_stop)).assertIsDisplayed()
        // The screen is a scrollable column, so "Vibrate" and "Alarm sound" can land
        // below the fold on shorter screens — existence, not on-screen visibility, is
        // what this test cares about.
        composeTestRule.onNodeWithText(str(R.string.vibrate)).assertExists()
        composeTestRule.onNodeWithText(str(R.string.alarm_sound)).assertExists()
    }

    @Test
    fun togglingCustomSettingsRowInvokesCallback() {
        var value: Boolean? = null
        setContent(useCustomSettings = false, onUseCustomSettingsChange = { value = it })
        composeTestRule.onNodeWithText(str(R.string.custom_settings_for_alarm)).performClick()
        assertEquals(true, value)
    }

    @Test
    fun gradualVolumeRowOnlyExistsWithCustomSettings() {
        setContent(useCustomSettings = false)
        composeTestRule.onNodeWithText(str(R.string.gradual_volume)).assertDoesNotExist()
    }

    @Test
    fun togglingGradualVolumeRowInvokesCallback() {
        var value: Boolean? = null
        setContent(useCustomSettings = true, gradualVolume = false, onGradualVolumeChange = { value = it })
        composeTestRule.onNodeWithText(str(R.string.gradual_volume)).performClick()
        assertEquals(true, value)
    }

    @Test
    fun typingIntoLabelFieldInvokesCallback() {
        var value: String? = null
        setContent(label = "", onLabelChange = { value = it })
        composeTestRule.onNodeWithTag("alarmLabelField").performTextInput("Wake up")
        assertEquals("Wake up", value)
    }

    // The day chips' contentDescription is the device locale's full day name (see
    // DayOfWeekSelector), not always English, so tests must derive it the same way
    // rather than hardcoding "Monday".
    private val mondayFullName: String =
        DayOfWeek.MONDAY.getDisplayName(TextStyle.FULL, Locale.getDefault())

    @Test
    fun selectingUnselectedDayAddsIt() {
        var result: Set<DayOfWeek>? = null
        setContent(repeatDays = emptySet(), onDaysChange = { result = it })
        composeTestRule.onNodeWithContentDescription(mondayFullName).performClick()
        assertEquals(setOf(DayOfWeek.MONDAY), result)
    }

    @Test
    fun selectingAlreadySelectedDayRemovesIt() {
        var result: Set<DayOfWeek>? = null
        setContent(repeatDays = setOf(DayOfWeek.MONDAY), onDaysChange = { result = it })
        composeTestRule.onNodeWithContentDescription(mondayFullName).performClick()
        assertTrue(result?.isEmpty() ?: false)
    }

    @Test
    fun saveButtonInvokesCallback() {
        var saved = false
        setContent(onSave = { saved = true })
        composeTestRule.onNodeWithText(str(R.string.save)).performClick()
        assertTrue(saved)
    }

    @Test
    fun deleteButtonInvokesCallback() {
        var deleted = false
        setContent(isNew = false, onDelete = { deleted = true })
        composeTestRule.onNodeWithContentDescription(str(R.string.delete_alarm)).performClick()
        assertTrue(deleted)
    }

    @Test
    fun backButtonInvokesCallback() {
        var backPressed = false
        setContent(onBack = { backPressed = true })
        composeTestRule.onNodeWithContentDescription(str(R.string.back)).performClick()
        assertTrue(backPressed)
    }

    @Test
    fun deleteButtonAbsentMeansNoAccidentalDeleteOnNewAlarm() {
        var deleted = false
        setContent(isNew = true, onDelete = { deleted = true })
        composeTestRule.onNodeWithContentDescription(str(R.string.delete_alarm)).assertDoesNotExist()
        assertFalse(deleted)
    }
}
