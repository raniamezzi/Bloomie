package com.example.bloomie.ui.main

import android.content.Context
import android.content.Intent
import com.example.bloomie.ui.assistant.CareCoachActivity
import com.example.bloomie.ui.assistant.ScheduleActivity
import com.example.bloomie.ui.details.AddEditPlantActivity
import com.example.bloomie.ui.details.PlantDetailsActivity

/** One place that knows how to open every secondary screen and which extras each one reads. */
object ScreenLauncher {

    /** Intent extra carrying a plant's database id (Long). */
    const val EXTRA_PLANT_ID = "plant_id"

    // Optional pre-fill values for AddEditPlantActivity (add mode only).
    const val EXTRA_PREFILL_NAME = "prefill_name"
    const val EXTRA_PREFILL_SPECIES = "prefill_species"
    const val EXTRA_PREFILL_IMAGE_PATH = "prefill_image_path"
    const val EXTRA_PREFILL_SUNLIGHT = "prefill_sunlight"
    const val EXTRA_PREFILL_WATERING_DAYS = "prefill_watering_days"

    fun openPlantDetails(context: Context, plantId: Long) {
        context.startActivity(
            Intent(context, PlantDetailsActivity::class.java).putExtra(EXTRA_PLANT_ID, plantId)
        )
    }

    /** Opens the add/edit form. Pass a plant id to edit, or null to add a new plant. */
    fun openAddEditPlant(context: Context, plantId: Long? = null) {
        val intent = Intent(context, AddEditPlantActivity::class.java)
        if (plantId != null) intent.putExtra(EXTRA_PLANT_ID, plantId)
        context.startActivity(intent)
    }

    /** Opens the add form with some fields filled in (from the Care Library or Identify results). */
    fun openAddEditPlantPrefilled(
        context: Context,
        name: String,
        species: String,
        imagePath: String,
        sunlight: String,
        wateringDays: Int
    ) {
        val intent = Intent(context, AddEditPlantActivity::class.java)
            .putExtra(EXTRA_PREFILL_NAME, name)
            .putExtra(EXTRA_PREFILL_SPECIES, species)
            .putExtra(EXTRA_PREFILL_IMAGE_PATH, imagePath)
            .putExtra(EXTRA_PREFILL_SUNLIGHT, sunlight)
            .putExtra(EXTRA_PREFILL_WATERING_DAYS, wateringDays)
        context.startActivity(intent)
    }

    fun openCoach(context: Context) {
        context.startActivity(Intent(context, CareCoachActivity::class.java))
    }

    fun openSchedule(context: Context) {
        context.startActivity(Intent(context, ScheduleActivity::class.java))
    }
}
