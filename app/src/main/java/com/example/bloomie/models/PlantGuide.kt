package com.example.bloomie.models

import java.io.Serializable

/**
 * A Care Library entry. Field names match the keys in `assets/plants.json` exactly,
 * so the file can be parsed directly with Gson.
 */
data class PlantGuide(
    val id: String,
    val commonName: String,
    val scientificName: String,
    val category: String,
    val difficulty: String,
    val watering: String,
    val sunlight: String,
    val humidity: String,
    val temperature: String,
    val soil: String,
    val toxicity: String,
    val description: String,
    val careTips: List<String>
) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}
