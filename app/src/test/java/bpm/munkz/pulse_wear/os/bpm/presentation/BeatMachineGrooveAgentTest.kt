package bpm.munkz.pulse_wear.os.bpm.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BeatMachineGrooveAgentTest {
    @Test
    fun evaluatesLoosePocketPatternAboveStraightGrid() {
        val loosePocket = listOf(
            0b1100_0101_1000_0001,
            0b0001_0000_0001_0000,
            0b1010_1110_1010_1110,
            0b0000_1000_0000_1000,
            0,
            0,
            0b1000_0000_0100_0000,
            0,
        )
        val straightGrid = listOf(
            0b0001_0000_0001_0000,
            0b0001_0000_0001_0000,
            0b0101_0101_0101_0101,
            0,
            0,
            0,
            0,
            0,
        )

        val looseReport = BeatMachineGrooveAgent.evaluate(loosePocket)
        val straightReport = BeatMachineGrooveAgent.evaluate(straightGrid)

        assertTrue(looseReport.score > straightReport.score)
        assertTrue(looseReport.score >= 82)
        assertEquals("loose pocket", looseReport.label)
    }

    @Test
    fun delaysBackbeatAndOffbeatHatsForLaidBackPocket() {
        val stepDurationMs = BeatMachineGrooveAgent.stepDurationMs(96)

        assertTrue(BeatMachineGrooveAgent.timingOffsetMs(1, 4, stepDurationMs) > 0L)
        assertTrue(
            BeatMachineGrooveAgent.timingOffsetMs(2, 5, stepDurationMs) >
                BeatMachineGrooveAgent.timingOffsetMs(2, 4, stepDurationMs),
        )
        assertTrue(BeatMachineGrooveAgent.timingOffsetMs(1, 4, stepDurationMs) < stepDurationMs / 3L)
    }
}
