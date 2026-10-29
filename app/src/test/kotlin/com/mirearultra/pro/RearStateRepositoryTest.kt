package com.mirearultra.pro

import com.mirearultra.pro.core.BatteryTelemetry
import com.mirearultra.pro.core.CallEvent
import com.mirearultra.pro.core.CallStatus
import com.mirearultra.pro.core.RearNotificationItem
import com.mirearultra.pro.core.RearScreenMode
import com.mirearultra.pro.core.RearStateRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RearStateRepositoryTest {

    @Before
    fun setUp() {
        RearStateRepository.updateSettings {
            it.copy(
                modIncomingCall = true,
                modNotifications = true,
                modLiveActivities = true,
                modSpeedDial = true,
                modDailyCards = true,
                modLedSignAndSos = true,
                modMiniGame = true,
                ecoBatterySaverMode = false,
                autoEcoBelow20Percent = true,
                ltpo1HzClockMode = true,
                skipRearWakeWhenFrontActive = true,
                rearScreenTimeoutSec = 20
            )
        }
        RearStateRepository.onCallEnded()
        RearStateRepository.setMode(RearScreenMode.HOME_LAUNCHER)
    }

    @Test
    fun testIncomingVipCallAndSpeakerphoneAnswer() {
        RearStateRepository.onIncomingCall(
            CallEvent(
                callerName = "Eşim ❤️",
                phoneNumber = "+90 532 999 88 77",
                sourceApp = "Telefon",
                status = CallStatus.RINGING
            )
        )

        val call = RearStateRepository.currentCall.value
        assertNotNull(call)
        assertEquals(CallStatus.RINGING, call?.status)
        assertTrue("Eşim should be recognized as VIP caller", call?.isVipCaller == true)
        assertEquals(RearScreenMode.INCOMING_CALL, RearStateRepository.activeMode.value)

        // Answer call -> should transition to ACTIVE with speakerphone enabled
        RearStateRepository.updateCallState {
            it.copy(status = CallStatus.ACTIVE, isSpeakerOn = true)
        }
        val activeCall = RearStateRepository.currentCall.value
        assertEquals(CallStatus.ACTIVE, activeCall?.status)
        assertTrue(activeCall?.isSpeakerOn == true)

        // End call -> should return to HOME_LAUNCHER
        RearStateRepository.onCallEnded()
        assertNull(RearStateRepository.currentCall.value)
        assertEquals(RearScreenMode.HOME_LAUNCHER, RearStateRepository.activeMode.value)
    }

    @Test
    fun testWhatsAppDirectReplyFlow() {
        RearStateRepository.postNotification(
            RearNotificationItem(
                id = "wa_test_1",
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                sender = "Annem ❤️",
                message = "Akşam yemeğe geliyor musun?"
            )
        )

        assertEquals(RearScreenMode.NOTIFICATION_POPUP, RearStateRepository.activeMode.value)
        val latest = RearStateRepository.latestPopupNotification.value
        assertNotNull(latest)
        assertTrue("Annem should be marked as VIP sender", latest?.isVip == true)

        // Simulate Direct Reply
        RearStateRepository.markNotificationReplied("wa_test_1", "Geliyorum, 10 dakikaya oradayım 👍")
        val updated = RearStateRepository.notifications.value.first { it.id == "wa_test_1" }
        assertTrue(updated.isReplied)
        assertEquals("Geliyorum, 10 dakikaya oradayım 👍", updated.lastReplyText)
    }

    @Test
    fun testModularFeatureToggleDisablesIncomingCallRearWake() {
        // Turn OFF Incoming Calls module
        RearStateRepository.updateSettings { it.copy(modIncomingCall = false) }

        RearStateRepository.onIncomingCall(
            CallEvent(
                callerName = "Test Arayan",
                phoneNumber = "05001112233",
                sourceApp = "Telefon"
            )
        )

        // Because modIncomingCall is OFF, rear mode should remain HOME_LAUNCHER
        assertEquals(RearScreenMode.HOME_LAUNCHER, RearStateRepository.activeMode.value)
    }

    @Test
    fun testBatteryEcoOptimizationAndAutoEcoBelow20Percent() {
        RearStateRepository.updateBatteryTelemetry(
            BatteryTelemetry(
                levelPercent = 15,
                isCharging = false,
                wattage = 0f,
                temperatureCelsius = 31.0f
            )
        )

        // Below 20% and not charging -> isEffectiveEcoMode() must automatically become true
        assertTrue(RearStateRepository.isEffectiveEcoMode())

        // Plug in 120W HyperCharge -> Eco mode disables while charging
        RearStateRepository.updateBatteryTelemetry(
            BatteryTelemetry(
                levelPercent = 15,
                isCharging = true,
                wattage = 120f,
                temperatureCelsius = 34.5f
            )
        )
        assertFalse(RearStateRepository.isEffectiveEcoMode())
    }
}
