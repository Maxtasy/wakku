package com.maxtasy.wakku.ringing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.maxtasy.wakku.ui.theme.WakkuTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime

/**
 * Covers the pre-shake Stop/Snooze screen and the Stop -> shake-challenge
 * transition. The challenge's own shake counting comes from a real
 * accelerometer via [com.maxtasy.wakku.shake.ShakeDetector] and isn't
 * exercised here — that logic is unit tested directly on
 * [com.maxtasy.wakku.shake.ShakeCounter].
 */
@RunWith(AndroidJUnit4::class)
class RingingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        ringingId: Long? = 1L,
        alarmId: Long = 1L,
        currentTime: LocalTime = LocalTime.of(6, 45),
        startChallenge: Boolean = false,
        hour: Int = 7,
        minute: Int = 5,
        label: String = "",
        requiredShakes: Int = 30,
        onFinish: () -> Unit = {},
        onSnooze: () -> Unit = {},
        onStopTapped: () -> Unit = {},
        onChallengeCompleted: () -> Unit = {},
        onChallengeAbandoned: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            WakkuTheme {
                RingingScreen(
                    ringingId = ringingId,
                    alarmId = alarmId,
                    currentTime = currentTime,
                    startChallenge = startChallenge,
                    hour = hour,
                    minute = minute,
                    label = label,
                    requiredShakes = requiredShakes,
                    onFinish = onFinish,
                    onSnooze = onSnooze,
                    onStopTapped = onStopTapped,
                    onChallengeCompleted = onChallengeCompleted,
                    onChallengeAbandoned = onChallengeAbandoned,
                )
            }
        }
    }

    @Test
    fun showsCurrentTimeAndAlarmTimeWithLeadingZeros() {
        setContent(currentTime = LocalTime.of(6, 5), hour = 7, minute = 5)
        composeTestRule.onNodeWithText("06:05").assertIsDisplayed()
        composeTestRule.onNodeWithText("Set for 07:05").assertIsDisplayed()
    }

    @Test
    fun startChallengeSkipsStraightToShakeScreen() {
        var stopTapped = false
        setContent(startChallenge = true, requiredShakes = 5, onStopTapped = { stopTapped = true })
        composeTestRule.waitForIdle()
        assertTrue(stopTapped)
        composeTestRule.onNodeWithText("0 / 5").assertIsDisplayed()
    }

    @Test
    fun showsLabelWhenPresent() {
        setContent(label = "Wake up")
        composeTestRule.onNodeWithText("Wake up").assertIsDisplayed()
    }

    @Test
    fun fallsBackToGenericLabelWhenBlank() {
        setContent(label = "")
        composeTestRule.onNodeWithText("Alarm").assertIsDisplayed()
    }

    @Test
    fun snoozeButtonInvokesCallback() {
        var snoozed = false
        setContent(onSnooze = { snoozed = true })
        composeTestRule.onNodeWithText("Snooze").performClick()
        assertTrue(snoozed)
    }

    @Test
    fun stopButtonInvokesCallbackAndStartsShakeChallenge() {
        var stopTapped = false
        setContent(requiredShakes = 42, onStopTapped = { stopTapped = true })
        composeTestRule.onNodeWithText("Stop").performClick()
        assertTrue(stopTapped)
        composeTestRule.onNodeWithText("Shake to stop").assertIsDisplayed()
        composeTestRule.onNodeWithText("0 / 42").assertIsDisplayed()
    }

    @Test
    fun cancellingShakeChallengeInvokesAbandonedCallback() {
        var abandoned = false
        setContent(onChallengeAbandoned = { abandoned = true })
        composeTestRule.onNodeWithText("Stop").performClick()
        composeTestRule.onNodeWithText("Snooze instead").performClick()
        assertTrue(abandoned)
    }

    @Test
    fun finishesWhenRingingIdNoLongerMatchesAndNotShaking() {
        var finished = false
        setContent(ringingId = 2L, alarmId = 1L, onFinish = { finished = true })
        composeTestRule.waitForIdle()
        assertTrue(finished)
    }

    @Test
    fun doesNotFinishWhileRingingIdStillMatches() {
        var finished = false
        setContent(ringingId = 1L, alarmId = 1L, onFinish = { finished = true })
        composeTestRule.waitForIdle()
        assertFalse(finished)
    }

    @Test
    fun doesNotAutoFinishOnceShakeChallengeStarted() {
        var finishCount = 0
        setContent(ringingId = 1L, alarmId = 1L, onFinish = { finishCount++ })
        composeTestRule.onNodeWithText("Stop").performClick()
        composeTestRule.waitForIdle()
        // Tapping Stop flips isShaking to true, re-running the auto-finish
        // LaunchedEffect (keyed on ringingId AND isShaking) — it must stay a no-op.
        assertEquals(0, finishCount)
    }
}
