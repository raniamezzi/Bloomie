package com.example.bloomie.ui.details

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloomie.R
import com.example.bloomie.data.db.DatabaseHelper
import com.example.bloomie.data.db.ImageStorage
import com.example.bloomie.databinding.ActivityPlantDetailsBinding
import com.example.bloomie.models.HealthStatus
import com.example.bloomie.models.Plant
import com.example.bloomie.ui.main.DateFormatter
import com.example.bloomie.ui.main.PlantImageLoader
import com.example.bloomie.ui.main.PlantUiHelper
import com.example.bloomie.ui.main.ScreenLauncher
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Screen 06: one plant's photo, care details, watering history and actions.
 * Expects the plant's database id in [ScreenLauncher.EXTRA_PLANT_ID].
 */
class PlantDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlantDetailsBinding
    private lateinit var db: DatabaseHelper
    private lateinit var historyAdapter: WateringHistoryAdapter

    private var plantId: Long = -1L
    private var currentPlant: Plant? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlantDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)
        plantId = intent.getLongExtra(ScreenLauncher.EXTRA_PLANT_ID, -1L)

        historyAdapter = WateringHistoryAdapter()
        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerHistory.adapter = historyAdapter

        binding.btnBack.setOnClickListener { finish() }
        binding.btnWaterNow.setOnClickListener { waterNow() }
        binding.btnEdit.setOnClickListener {
            if (plantId > 0) ScreenLauncher.openAddEditPlant(this, plantId)
        }
        binding.btnDelete.setOnClickListener { confirmDelete() }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }

    private fun reload() {
        val plant = if (plantId > 0) db.getPlantById(plantId) else null
        if (plant == null) {
            Toast.makeText(this, R.string.details_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        currentPlant = plant
        bindPlant(plant)
        bindHistory()
    }

    private fun bindPlant(plant: Plant) {
        binding.txtName.text = plant.displayName
        binding.txtSpecies.text = plant.species.ifBlank { plant.name }
        PlantUiHelper.styleStatusBadge(binding.txtHealthBadge, plant)

        binding.txtWateringValue.text = getString(R.string.every_n_days, plant.wateringFrequencyDays)
        binding.txtSunlightValue.text = plant.sunlight.ifBlank { "-" }
        binding.txtRoomValue.text = PlantUiHelper.roomLabel(this, plant)
        binding.txtNextValue.text = DateFormatter.formatDate(plant.nextWateringTimestamp)
        binding.txtNextCountdown.text = PlantUiHelper.countdownText(this, plant)

        binding.txtLastWatered.text = if (plant.lastWateredTimestamp > 0L) {
            getString(R.string.last_watered, DateFormatter.formatDate(plant.lastWateredTimestamp))
        } else {
            getString(R.string.never_watered)
        }

        binding.cardNotes.visibility = if (plant.notes.isBlank()) View.GONE else View.VISIBLE
        binding.txtNotes.text = plant.notes

        bindHeroImage(plant)

        // Detach the listener while the state is set so it does not fire a database write.
        binding.switchAttention.setOnCheckedChangeListener(null)
        binding.switchAttention.isChecked = plant.healthStatus == HealthStatus.ATTENTION
        binding.switchAttention.setOnCheckedChangeListener { _, isChecked ->
            db.setNeedsAttention(plantId, isChecked)
            reload()
        }
    }

    private fun bindHeroImage(plant: Plant) {
        PlantImageLoader.load(binding.imgHero, plant.imageUri)
        if (plant.imageUri.isBlank()) {
            // The leaf placeholder is a small icon, so give it room in the large hero area.
            val padding = (resources.displayMetrics.density * PLACEHOLDER_PADDING_DP).toInt()
            binding.imgHero.setPadding(padding, padding, padding, padding)
        }
    }

    private fun bindHistory() {
        val logs = db.getWateringHistory(plantId)
        historyAdapter.submitList(logs)
        binding.txtNoHistory.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun waterNow() {
        val plant = currentPlant ?: return
        if (db.markAsWatered(plant.id)) {
            Toast.makeText(this, getString(R.string.watered_success, plant.displayName), Toast.LENGTH_SHORT).show()
            reload()
        } else {
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete() {
        val plant = currentPlant ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_plant_title)
            .setMessage(getString(R.string.delete_plant_message, plant.displayName))
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                if (db.deletePlant(plant.id)) {
                    ImageStorage.deleteIfPermanent(this, plant.imageUri)
                    Toast.makeText(this, R.string.plant_deleted, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private companion object {
        const val PLACEHOLDER_PADDING_DP = 96
    }
}
