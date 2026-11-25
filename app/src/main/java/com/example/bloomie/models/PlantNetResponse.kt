package com.example.bloomie.models

import java.io.Serializable

/**
 * Subset of the Pl@ntNet `/v2/identify/{project}` JSON response that Bloomie uses.
 * Every field is nullable because the API omits fields it has no data for.
 */
data class PlantNetResponse(
    val bestMatch: String? = null,
    val results: List<PlantNetResult>? = null,
    val remainingIdentificationRequests: Int? = null
)

data class PlantNetResult(
    val score: Double? = null,
    val species: PlantNetSpecies? = null
)

data class PlantNetSpecies(
    val scientificNameWithoutAuthor: String? = null,
    val scientificName: String? = null,
    val commonNames: List<String>? = null,
    val family: PlantNetName? = null,
    val genus: PlantNetName? = null
)

data class PlantNetName(
    val scientificNameWithoutAuthor: String? = null,
    val scientificName: String? = null
)

/**
 * One identification candidate shown on the results screen.
 * [score] is a confidence between 0.0 and 1.0.
 */
data class IdentifyMatch(
    val scientificName: String,
    val commonName: String,
    val family: String,
    val score: Double
) : Serializable {

    /** Confidence as a whole percentage between 0 and 100. */
    val percent: Int
        get() = (score * 100.0).toInt().coerceIn(0, 100)

    /** Common name when known, otherwise the scientific name. */
    val displayName: String
        get() = if (commonName.isNotBlank()) commonName else scientificName

    companion object {
        private const val serialVersionUID = 1L
    }
}
