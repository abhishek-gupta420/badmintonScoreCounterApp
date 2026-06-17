package com.abhi.badmintonscoreconunter

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests for the Badminton scoring logic.
 * This tests the rules for winning a set and handling deuce/sudden death.
 */
class BadmintonScoringLogicTest {

    private fun isSetOver(t1: Int, t2: Int, matchPoints: Int): Boolean {
        val maxPoints = matchPoints + 9
        return when {
            (t1 >= matchPoints || t2 >= matchPoints) && abs(t1 - t2) >= 2 -> true
            t1 == maxPoints || t2 == maxPoints -> true
            else -> false
        }
    }

    @Test
    fun `standard win at match points`() {
        // Official match to 21
        assertEquals(true, isSetOver(21, 19, 21))
        assertEquals(true, isSetOver(19, 21, 21))
        
        // Match not over yet
        assertEquals(false, isSetOver(20, 19, 21))
        assertEquals(false, isSetOver(20, 20, 21))
    }

    @Test
    fun `deuce rule - must win by 2`() {
        // Tied at 20-20, must reach 22-20
        assertEquals(false, isSetOver(21, 20, 21))
        assertEquals(true, isSetOver(22, 20, 21))
        
        // Tied at 25-25, must reach 27-25
        assertEquals(false, isSetOver(26, 25, 21))
        assertEquals(true, isSetOver(27, 25, 21))
    }

    @Test
    fun `sudden death at maximum points`() {
        // For 21 points, max is 30
        assertEquals(false, isSetOver(29, 29, 21))
        assertEquals(true, isSetOver(30, 29, 21))
        
        // For 11 points, max is 20
        assertEquals(false, isSetOver(19, 19, 11))
        assertEquals(true, isSetOver(20, 19, 11))
    }

    @Test
    fun `medium match rules`() {
        // Match to 15
        assertEquals(true, isSetOver(15, 13, 15))
        assertEquals(false, isSetOver(15, 14, 15))
        assertEquals(true, isSetOver(16, 14, 15))
    }
}
