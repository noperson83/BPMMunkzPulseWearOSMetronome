package bpm.munkz.pulse_wear.os.bpm.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProTrialGateTest {
    @Test
    fun proFreeControlsUnlockDuringTrial() {
        assertFalse(
            proFeatureControlsEnabled(
                isProFree = true,
                trialSettingsEnabled = false,
                purchaseUnlocked = false,
            )
        )
        assertTrue(
            proFeatureControlsEnabled(
                isProFree = true,
                trialSettingsEnabled = true,
                purchaseUnlocked = false,
            )
        )
        assertTrue(
            proFeatureControlsEnabled(
                isProFree = false,
                trialSettingsEnabled = false,
                purchaseUnlocked = false,
            )
        )
    }

    @Test
    fun proFreeControlsUnlockAfterPurchase() {
        assertTrue(
            proFeatureControlsEnabled(
                isProFree = true,
                trialSettingsEnabled = false,
                purchaseUnlocked = true,
            )
        )
        assertTrue(
            proFeatureControlsEnabled(
                isProFree = true,
                trialSettingsEnabled = true,
                purchaseUnlocked = true,
            )
        )
    }
}
