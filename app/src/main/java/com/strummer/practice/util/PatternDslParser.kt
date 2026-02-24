package com.strummer.practice.util

import com.strummer.practice.model.PatternStep
import com.strummer.practice.model.StrumPattern

object PatternDslParser {
    fun parse(id: String, name: String, subdivision: Int, dsl: String): StrumPattern {
        require(subdivision == 8 || subdivision == 16) { "Subdivision must be 8 or 16" }

        val tokens = dsl
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { it.trim().uppercase() }

        require(tokens.isNotEmpty()) { "Pattern cannot be empty" }

        val steps = tokens.map { token ->
            when (token) {
                "D", "U", "X", "-" -> PatternStep(kindRaw = token, accent = false)
                else -> throw IllegalArgumentException("Invalid step token: $token")
            }
        }

        return StrumPattern(
            id = id,
            name = name,
            subdivision = subdivision,
            steps = steps
        )
    }
}
