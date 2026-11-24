package com.zyven.fnfassistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TapPlannerTest {
    private val hitY = 0.115f
    private val lead = 90L
    private val speed = 0.001f // normalized screen height per millisecond (~1 screen/second)

    /** A simulated note: y(t) = y0 + dir * speed * t. [tall] > 0 makes it a hold note (top = y). */
    private data class Sim(val lane: Int, val y0: Float, val dir: Float = -1f, val tall: Float = 0f)

    private fun candidate(sim: Sim, y: Float): NoteCandidate =
        if (sim.tall > 0f) {
            NoteCandidate(sim.lane, y + sim.tall / 2f, 900, y, y + sim.tall)
        } else {
            NoteCandidate(sim.lane, y, 500, y - 0.07f, y + 0.07f)
        }

    /** Feeds a frame every 50 ms (0..1200 ms) and returns (lane, absolute tap time in ms). */
    private fun simulate(vararg sims: Sim, stationary: Boolean = false): List<Pair<Int, Long>> {
        val planner = TapPlanner()
        val taps = ArrayList<Pair<Int, Long>>()
        var t = 0L
        while (t <= 1200L) {
            val frame = ArrayList<NoteCandidate>()
            for (sim in sims) {
                val y = if (stationary) sim.y0 else sim.y0 + sim.dir * speed * t
                // The detector only reports notes between 10% and 84% of the screen height.
                if (y in 0.10f..0.84f) frame.add(candidate(sim, y))
            }
            for (plan in planner.update(frame, t, hitY, lead)) taps.add(plan.lane to (t + plan.delayMs))
            t += 50L
        }
        return taps
    }

    private fun crossingMs(y0: Float, headOffset: Float = 0f): Double =
        ((y0 + headOffset - hitY) / speed).toDouble()

    @Test
    fun singleNoteIsTappedOnceAtTheRightTime() {
        val taps = simulate(Sim(lane = 2, y0 = 0.80f))
        assertEquals(1, taps.size)
        assertEquals(2, taps[0].first)
        assertEquals(crossingMs(0.80f) - lead, taps[0].second.toDouble(), 3.0)
    }

    @Test
    fun chordTapsBothLanesTogether() {
        val taps = simulate(Sim(lane = 1, y0 = 0.80f), Sim(lane = 3, y0 = 0.80f))
        assertEquals(2, taps.size)
        assertEquals(setOf(1, 3), taps.map { it.first }.toSet())
        assertEquals(taps[0].second.toDouble(), taps[1].second.toDouble(), 3.0)
    }

    @Test
    fun twoNotesInTheSameLaneAreBothTapped() {
        val taps = simulate(Sim(lane = 4, y0 = 0.80f), Sim(lane = 4, y0 = 1.00f))
        assertEquals(2, taps.size)
        val times = taps.map { it.second.toDouble() }.sorted()
        assertEquals(crossingMs(0.80f) - lead, times[0], 3.0)
        assertEquals(crossingMs(1.00f) - lead, times[1], 3.0)
    }

    @Test
    fun noteMovingAwayFromTheLineIsIgnored() {
        val taps = simulate(Sim(lane = 2, y0 = 0.25f, dir = 1f))
        assertTrue(taps.isEmpty())
    }

    @Test
    fun stationaryShapeIsIgnored() {
        val taps = simulate(Sim(lane = 2, y0 = 0.30f), stationary = true)
        assertTrue(taps.isEmpty())
    }

    @Test
    fun holdNoteIsTappedOnceAtItsHead() {
        val taps = simulate(Sim(lane = 3, y0 = 0.80f, tall = 0.30f))
        assertEquals(1, taps.size)
        assertEquals(crossingMs(0.80f, headOffset = 0.06f) - lead, taps[0].second.toDouble(), 3.0)
    }

    @Test
    fun resetForgetsAllTracks() {
        val planner = TapPlanner()
        planner.update(listOf(candidate(Sim(1, 0.5f), 0.50f)), 0L, hitY, lead)
        planner.reset()
        val plans = planner.update(listOf(candidate(Sim(1, 0.5f), 0.45f)), 50L, hitY, lead)
        assertTrue(plans.isEmpty())
    }
}
