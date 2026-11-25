package com.example.bloomie.models

/** Root of `assets/coach_rules.json`. */
data class CoachData(
    val version: Int = 1,
    val title: String = "",
    val symptoms: List<CoachRule> = emptyList()
)

/** One symptom with its follow-up questions, possible causes and prevention advice. */
data class CoachRule(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val questions: List<CoachQuestion> = emptyList(),
    val causes: List<CoachCause> = emptyList(),
    val prevention: String = ""
)

data class CoachQuestion(
    val id: String = "",
    val text: String = "",
    val options: List<CoachOption> = emptyList()
)

/** An answer option. Choosing it adds one point to each cause listed in [causeIds]. */
data class CoachOption(
    val id: String = "",
    val label: String = "",
    val causeIds: List<String> = emptyList()
)

data class CoachCause(
    val id: String = "",
    val title: String = "",
    val urgency: String = "low",
    val explanation: String = "",
    val signs: List<String> = emptyList(),
    val remedies: List<String> = emptyList()
)
