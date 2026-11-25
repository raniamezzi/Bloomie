package com.example.bloomie.models

import java.io.Serializable

/**
 * One watering event. Maps to a row in the `watering_history` table.
 * [plantName] is stored alongside [plantId] so history stays readable even if the plant is deleted.
 */
data class WateringLog(
    val id: Long = 0L,
    val plantId: Long,
    val plantName: String,
    val wateredTimestamp: Long,
    val notes: String = ""
) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}
