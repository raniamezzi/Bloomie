package com.example.bloomie.models

import java.io.Serializable

/**
 * Health status values stored in / derived for [Plant.healthStatus].
 *
 * HEALTHY, NEEDS_WATER and OVERDUE are derived automatically from the watering schedule
 * by DatabaseHelper. ATTENTION is a manual flag (e.g. pests) that is never overwritten.
 */
object HealthStatus {
    const val HEALTHY = "healthy"
    const val NEEDS_WATER = "needs_water"
    const val OVERDUE = "overdue"
    const val ATTENTION = "attention"
}

/**
 * A plant owned by the user. Maps to a row in the `plants` table.
 *
 * Timestamps are epoch milliseconds. [imageUri] holds an internal file URI/path string
 * (never raw image bytes); an empty string means "no photo".
 */
data class Plant(
    val id: Long = 0L,
    val name: String,
    val species: String = "",
    val nickname: String = "",
    val room: String = "",
    val wateringFrequencyDays: Int = 7,
    val lastWateredTimestamp: Long = 0L,
    val nextWateringTimestamp: Long = 0L,
    val sunlight: String = "",
    val notes: String = "",
    val imageUri: String = "",
    val healthStatus: String = HealthStatus.HEALTHY
) : Serializable {

    /** Nickname when set, otherwise the plant's name. */
    val displayName: String
        get() = if (nickname.isNotBlank()) nickname else name

    companion object {
        private const val serialVersionUID = 1L
    }
}
