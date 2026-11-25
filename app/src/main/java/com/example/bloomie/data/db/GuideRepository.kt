package com.example.bloomie.data.db

import android.content.Context
import com.example.bloomie.R
import com.example.bloomie.models.PlantGuide
import com.google.gson.Gson
import com.google.gson.JsonParseException
import java.io.IOException

/** Loads the Care Library from `assets/plants.json` (cached after the first read). */
object GuideRepository {

    @Volatile
    private var cache: List<PlantGuide>? = null

    private val imageCache = mutableMapOf<String, Int>()

    private val wateringPattern =
        Regex("(\\d+)(?:\\s*-\\s*(\\d+))?\\s*(day|week)", RegexOption.IGNORE_CASE)

    fun load(context: Context): List<PlantGuide> {
        cache?.let { return it }

        val guides: List<PlantGuide> = try {
            context.applicationContext.assets.open("plants.json").bufferedReader().use { reader ->
                Gson().fromJson(reader, Array<PlantGuide>::class.java)?.toList().orEmpty()
            }
        } catch (e: IOException) {
            emptyList()
        } catch (e: JsonParseException) {
            emptyList()
        }

        if (guides.isNotEmpty()) cache = guides
        return guides
    }

    fun findById(context: Context, id: String): PlantGuide? {
        return load(context).firstOrNull { it.id == id }
    }

    fun findByScientificName(context: Context, scientificName: String): PlantGuide? {
        val wanted = scientificName.trim()
        if (wanted.isEmpty()) return null
        val all = load(context)

        all.firstOrNull { it.scientificName.equals(wanted, ignoreCase = true) }?.let { return it }

        val genus = wanted.substringBefore(" ")
        return all.firstOrNull {
            it.scientificName.substringBefore(" ").equals(genus, ignoreCase = true)
        }
    }

    fun imageResFor(context: Context, guide: PlantGuide): Int {
        return imageCache.getOrPut(guide.id) {
            val resId = context.applicationContext.resources.getIdentifier(
                "plant_${guide.id}",
                "drawable",
                context.packageName
            )
            if (resId != 0) resId else R.drawable.img_welcome_plant
        }
    }

    /**
     * Reads the first number out of text such as "Every 7-10 days" or "Every 2-3 weeks"
     * and returns it as a number of days (default 7).
     */
    fun wateringDaysFor(guide: PlantGuide): Int {
        val match = wateringPattern.find(guide.watering) ?: return DEFAULT_WATERING_DAYS
        val amount = match.groupValues[1].toIntOrNull() ?: return DEFAULT_WATERING_DAYS
        val days = if (match.groupValues[3].equals("week", ignoreCase = true)) amount * 7 else amount
        return days.coerceIn(1, 90)
    }

    /** Maps free guide text such as "Low light to bright indirect light" to one of the form's four options. */
    fun sunlightOptionFor(context: Context, guideSunlight: String): String {
        val text = guideSunlight.lowercase()
        return when {
            text.contains("full sun") || text.contains("direct sun") -> context.getString(R.string.sunlight_full)
            text.contains("partial") -> context.getString(R.string.sunlight_partial)
            text.startsWith("low") -> context.getString(R.string.sunlight_low)
            else -> context.getString(R.string.sunlight_indirect)
        }
    }

    private const val DEFAULT_WATERING_DAYS = 7
}