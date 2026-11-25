package com.example.bloomie.ui.main

import android.content.Context
import android.widget.TextView
import com.example.bloomie.R
import com.example.bloomie.data.db.DatabaseHelper
import com.example.bloomie.models.HealthStatus
import com.example.bloomie.models.Plant
import kotlin.math.roundToInt

/** Shared text and badge styling for plant rows and cards. */
object PlantUiHelper {

    private const val DAY_MS = 86_400_000.0

    /** Calendar days until the next watering: negative when overdue, 0 when due today. */
    fun daysUntilWatering(plant: Plant, now: Long = System.currentTimeMillis()): Int {
        val diff = DatabaseHelper.startOfDay(plant.nextWateringTimestamp) - DatabaseHelper.startOfDay(now)
        return (diff / DAY_MS).roundToInt()
    }

    /** "Water today", "Water tomorrow", "Water in 4 days" or "2 days overdue". */
    fun countdownText(context: Context, plant: Plant): String {
        val days = daysUntilWatering(plant)
        return when {
            days < -1 -> context.getString(R.string.water_overdue_days, -days)
            days == -1 -> context.getString(R.string.water_overdue_one_day)
            days == 0 -> context.getString(R.string.water_today)
            days == 1 -> context.getString(R.string.water_tomorrow)
            else -> context.getString(R.string.water_in_days, days)
        }
    }

    /** Fills [textView] with the countdown and colors it: red overdue, amber today, sage later. */
    fun styleCountdownPill(textView: TextView, plant: Plant) {
        val days = daysUntilWatering(plant)
        textView.text = countdownText(textView.context, plant)
        when {
            days < 0 -> applyBadge(textView, R.drawable.bg_badge_overdue, R.color.status_overdue)
            days == 0 -> applyBadge(textView, R.drawable.bg_badge_warning, R.color.status_warning)
            else -> applyBadge(textView, R.drawable.bg_badge_neutral, R.color.sage_green)
        }
    }

    /** Fills [textView] with the plant's health label and colors it to match. */
    fun styleStatusBadge(textView: TextView, plant: Plant) {
        val context = textView.context
        when (plant.healthStatus) {
            HealthStatus.OVERDUE -> {
                textView.text = context.getString(R.string.status_overdue)
                applyBadge(textView, R.drawable.bg_badge_overdue, R.color.status_overdue)
            }
            HealthStatus.NEEDS_WATER -> {
                textView.text = context.getString(R.string.status_needs_water)
                applyBadge(textView, R.drawable.bg_badge_warning, R.color.status_warning)
            }
            HealthStatus.ATTENTION -> {
                textView.text = context.getString(R.string.status_attention)
                applyBadge(textView, R.drawable.bg_badge_warning, R.color.status_warning)
            }
            else -> {
                textView.text = context.getString(R.string.status_healthy)
                applyBadge(textView, R.drawable.bg_badge_healthy, R.color.status_healthy)
            }
        }
    }

    /** Room name, or a neutral label when none was set. */
    fun roomLabel(context: Context, plant: Plant): String {
        return plant.room.ifBlank { context.getString(R.string.room_unassigned) }
    }

    private fun applyBadge(textView: TextView, backgroundRes: Int, textColorRes: Int) {
        textView.setBackgroundResource(backgroundRes)
        textView.setTextColor(textView.context.getColor(textColorRes))
    }
}
