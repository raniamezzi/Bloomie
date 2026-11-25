package com.example.bloomie.ui.assistant

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloomie.R
import com.example.bloomie.data.db.DatabaseHelper
import com.example.bloomie.databinding.ActivityScheduleBinding
import com.example.bloomie.models.Plant
import com.example.bloomie.notifications.WateringNotificationHelper
import com.example.bloomie.ui.main.PlantUiHelper
import com.example.bloomie.ui.main.ScreenLauncher

/**
 * Screen 11: all plants grouped into Overdue, Due Today, Upcoming This Week and Later.
 * Watering a plant moves it to its new place in the timeline right away.
 * Tapping the bell button schedules a 5-second test watering notification;
 * long-pressing it re-schedules the daily 9:00 AM reminder.
 */
class ScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScheduleBinding
    private lateinit var db: DatabaseHelper
    private lateinit var adapter: ScheduleGroupAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DatabaseHelper(this)

        adapter = ScheduleGroupAdapter(
            onWater = { plant -> waterPlant(plant) },
            onOpen = { plant -> ScreenLauncher.openPlantDetails(this, plant.id) }
        )
        binding.recyclerGroups.layoutManager = LinearLayoutManager(this)
        binding.recyclerGroups.adapter = adapter
        binding.recyclerGroups.itemAnimator = null

        binding.btnBack.setOnClickListener { finish() }

        binding.btnTestNotification.setOnClickListener {
            WateringNotificationHelper.scheduleTestReminder(this, 5)
            Toast.makeText(this, "Test reminder scheduled in 5 seconds", Toast.LENGTH_SHORT).show()
        }

        binding.btnTestNotification.setOnLongClickListener {
            WateringNotificationHelper.scheduleDailyReminder(this)
            Toast.makeText(this, "Daily watering reminders active at 9:00 AM", Toast.LENGTH_SHORT).show()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }

    private fun waterPlant(plant: Plant) {
        if (db.markAsWatered(plant.id)) {
            Toast.makeText(this, getString(R.string.watered_success, plant.displayName), Toast.LENGTH_SHORT).show()
            reload()
        } else {
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show()
        }
    }

    private fun reload() {
        val groups = buildGroups(db.getAllPlants())
        adapter.submitList(groups)
        binding.txtEmpty.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerGroups.visibility = if (groups.isEmpty()) View.GONE else View.VISIBLE
    }

    /** Splits plants (already sorted by next watering date) into timeline sections, skipping empty ones. */
    private fun buildGroups(plants: List<Plant>): List<ScheduleGroup> {
        val overdue = mutableListOf<Plant>()
        val today = mutableListOf<Plant>()
        val week = mutableListOf<Plant>()
        val later = mutableListOf<Plant>()

        for (plant in plants) {
            val days = PlantUiHelper.daysUntilWatering(plant)
            when {
                days < 0 -> overdue.add(plant)
                days == 0 -> today.add(plant)
                days <= WEEK_DAYS -> week.add(plant)
                else -> later.add(plant)
            }
        }

        return listOf(
            ScheduleGroup(ScheduleKind.OVERDUE, overdue),
            ScheduleGroup(ScheduleKind.TODAY, today),
            ScheduleGroup(ScheduleKind.WEEK, week),
            ScheduleGroup(ScheduleKind.LATER, later)
        ).filter { it.plants.isNotEmpty() }
    }

    private companion object {
        const val WEEK_DAYS = 7
    }
}