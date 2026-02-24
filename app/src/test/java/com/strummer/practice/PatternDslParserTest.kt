package com.strummer.practice

import com.strummer.practice.model.StepKind
import com.strummer.practice.util.PatternDslParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternDslParserTest {
    @Test
    fun parsesBasicDslTokens() {
        val pattern = PatternDslParser.parse(
            id = "p1",
            name = "Test",
            subdivision = 8,
            dsl = "D D U U D U"
        )

        assertEquals(6, pattern.steps.size)
        assertEquals(StepKind.Down, pattern.steps[0].kind)
        assertEquals(StepKind.Up, pattern.steps[2].kind)
    }

    @Test
    fun handlesExtraWhitespaceAndRests() {
        val pattern = PatternDslParser.parse(
            id = "p2",
            name = "Test2",
            subdivision = 8,
            dsl = "  D   -   D U  - U D U "
        )

        assertEquals(8, pattern.steps.size)
        assertEquals(StepKind.Rest, pattern.steps[1].kind)
        assertEquals(StepKind.Rest, pattern.steps[4].kind)
    }

    @Test(expected = IllegalArgumentException::class)
    fun failsOnInvalidToken() {
        PatternDslParser.parse("p3", "Bad", 8, "D Q U")
    }

    @Test
    fun uppercaseNormalizationWorks() {
        val pattern = PatternDslParser.parse("p4", "Case", 8, "d u x -")
        assertTrue(pattern.steps.map { it.kind }.containsAll(listOf(StepKind.Down, StepKind.Up, StepKind.Mute, StepKind.Rest)))
    }
}
