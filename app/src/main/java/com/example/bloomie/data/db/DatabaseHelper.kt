package com.example.bloomie.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.bloomie.models.HealthStatus
import com.example.bloomie.models.Plant
import com.example.bloomie.models.WateringLog
import java.util.Calendar

/**
 * SQLite storage for Bloomie: the `plants` and `watering_history` tables.
 *
 * A new database starts empty, so each user builds their own garden.
 */
class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    /** Today's care progress: [completed] watering tasks out of [total]. */
    data class CareProgress(val completed: Int, val total: Int) {
        val fraction: Float
            get() = if (total == 0) 1f else completed.toFloat() / total.toFloat()
        val isAllDone: Boolean
            get() = completed >= total
    }

    // ---------------------------------------------------------------------------------------
    // Schema
    // ---------------------------------------------------------------------------------------

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_PLANTS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT NOT NULL,
                $COL_SPECIES TEXT NOT NULL DEFAULT '',
                $COL_NICKNAME TEXT NOT NULL DEFAULT '',
                $COL_ROOM TEXT NOT NULL DEFAULT '',
                $COL_FREQUENCY INTEGER NOT NULL DEFAULT 7,
                $COL_LAST_WATERED INTEGER NOT NULL DEFAULT 0,
                $COL_NEXT_WATERING INTEGER NOT NULL DEFAULT 0,
                $COL_SUNLIGHT TEXT NOT NULL DEFAULT '',
                $COL_NOTES TEXT NOT NULL DEFAULT '',
                $COL_IMAGE TEXT NOT NULL DEFAULT '',
                $COL_HEALTH TEXT NOT NULL DEFAULT '${HealthStatus.HEALTHY}'
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_HISTORY (
                $COL_H_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_H_PLANT_ID INTEGER NOT NULL,
                $COL_H_PLANT_NAME TEXT NOT NULL,
                $COL_H_WATERED INTEGER NOT NULL,
                $COL_H_NOTES TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_plants_next ON $TABLE_PLANTS($COL_NEXT_WATERING)")
        db.execSQL("CREATE INDEX idx_history_plant ON $TABLE_HISTORY($COL_H_PLANT_ID)")
        db.execSQL("CREATE INDEX idx_history_date ON $TABLE_HISTORY($COL_H_WATERED)")

    }

    /**
     * Version 1 is the first schema, so an upgrade simply rebuilds the tables.
     * Add incremental ALTER TABLE migrations here when the schema changes in later versions.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLANTS")
        onCreate(db)
    }

    // ---------------------------------------------------------------------------------------
    // Plants: create / update / delete
    // ---------------------------------------------------------------------------------------

    /** Inserts [plant] (its id is ignored) and returns the new row id, or -1 on failure. */
    fun insertPlant(plant: Plant): Long {
        return insertPlantRow(writableDatabase, plant)
    }

    /** Updates the plant with the same id. Returns true if a row was changed. */
    fun updatePlant(plant: Plant): Boolean {
        val frequency = plant.wateringFrequencyDays.coerceAtLeast(1)
        val next = if (plant.lastWateredTimestamp > 0L) {
            addDays(plant.lastWateredTimestamp, frequency)
        } else {
            plant.nextWateringTimestamp
        }
        val values = plantValues(plant.copy(wateringFrequencyDays = frequency, nextWateringTimestamp = next))
        return writableDatabase.update(TABLE_PLANTS, values, "$COL_ID = ?", arrayOf(plant.id.toString())) > 0
    }

    /** Deletes a plant. Its watering history is kept so streaks are not affected. */
    fun deletePlant(plantId: Long): Boolean {
        return writableDatabase.delete(TABLE_PLANTS, "$COL_ID = ?", arrayOf(plantId.toString())) > 0
    }

    /** Sets or clears the manual "needs attention" flag on a plant. */
    fun setNeedsAttention(plantId: Long, needsAttention: Boolean): Boolean {
        val values = ContentValues().apply {
            put(COL_HEALTH, if (needsAttention) HealthStatus.ATTENTION else HealthStatus.HEALTHY)
        }
        return writableDatabase.update(TABLE_PLANTS, values, "$COL_ID = ?", arrayOf(plantId.toString())) > 0
    }

    // ---------------------------------------------------------------------------------------
    // Plants: queries
    // ---------------------------------------------------------------------------------------

    fun getPlantById(plantId: Long): Plant? {
        return readableDatabase.query(
            TABLE_PLANTS, null, "$COL_ID = ?", arrayOf(plantId.toString()), null, null, null
        ).use { cursor -> if (cursor.moveToFirst()) cursor.toPlant() else null }
    }

    /** All plants, soonest watering first. */
    fun getAllPlants(): List<Plant> {
        return queryPlants(null, null)
    }

    /** Plants in [room], soonest watering first. */
    fun getPlantsByRoom(room: String): List<Plant> {
        return queryPlants("$COL_ROOM = ?", arrayOf(room))
    }

    /** Case-insensitive search over name, nickname, species and room. */
    fun searchPlants(query: String): List<Plant> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return getAllPlants()
        val like = "%$trimmed%"
        return queryPlants(
            "$COL_NAME LIKE ? OR $COL_NICKNAME LIKE ? OR $COL_SPECIES LIKE ? OR $COL_ROOM LIKE ?",
            arrayOf(like, like, like, like)
        )
    }

    /** Distinct, non-empty room names used by the user's plants. */
    fun getRooms(): List<String> {
        val rooms = mutableListOf<String>()
        readableDatabase.rawQuery(
            "SELECT DISTINCT $COL_ROOM FROM $TABLE_PLANTS WHERE $COL_ROOM != '' ORDER BY $COL_ROOM COLLATE NOCASE",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) rooms.add(cursor.getString(0))
        }
        return rooms
    }

    fun getPlantCount(): Int {
        return DatabaseUtils.queryNumEntries(readableDatabase, TABLE_PLANTS).toInt()
    }

    /** Plants whose next watering falls anywhere within today. */
    fun getPlantsDueToday(): List<Plant> {
        val now = System.currentTimeMillis()
        return queryPlants(
            "$COL_NEXT_WATERING >= ? AND $COL_NEXT_WATERING <= ?",
            arrayOf(startOfDay(now).toString(), endOfDay(now).toString())
        )
    }

    /** Plants whose next watering was before the start of today. */
    fun getOverduePlants(): List<Plant> {
        val now = System.currentTimeMillis()
        return queryPlants("$COL_NEXT_WATERING < ?", arrayOf(startOfDay(now).toString()))
    }

    /** Overdue plants followed by plants due today: everything that needs water right now. */
    fun getPlantsNeedingWater(): List<Plant> {
        val now = System.currentTimeMillis()
        return queryPlants("$COL_NEXT_WATERING <= ?", arrayOf(endOfDay(now).toString()))
    }

    /** Plants due after today but within the next [days] days (tomorrow counts as day 1). */
    fun getPlantsDueWithinDays(days: Int): List<Plant> {
        val now = System.currentTimeMillis()
        val from = endOfDay(now)
        val to = endOfDay(addDays(now, days.coerceAtLeast(1)))
        return queryPlants(
            "$COL_NEXT_WATERING > ? AND $COL_NEXT_WATERING <= ?",
            arrayOf(from.toString(), to.toString())
        )
    }

    // ---------------------------------------------------------------------------------------
    // Watering: mark as watered, history, streaks and progress
    // ---------------------------------------------------------------------------------------

    /**
     * Records a watering: writes a history entry and moves the plant's schedule forward by its
     * watering frequency. Returns false if the plant does not exist.
     */
    fun markAsWatered(
        plantId: Long,
        notes: String = "",
        wateredAt: Long = System.currentTimeMillis()
    ): Boolean {
        val plant = getPlantById(plantId) ?: return false
        val db = writableDatabase
        db.beginTransaction()
        try {
            val frequency = plant.wateringFrequencyDays.coerceAtLeast(1)
            val keptStatus =
                if (plant.healthStatus == HealthStatus.ATTENTION) HealthStatus.ATTENTION else HealthStatus.HEALTHY
            val plantValues = ContentValues().apply {
                put(COL_LAST_WATERED, wateredAt)
                put(COL_NEXT_WATERING, addDays(wateredAt, frequency))
                put(COL_HEALTH, keptStatus)
            }
            val updated = db.update(TABLE_PLANTS, plantValues, "$COL_ID = ?", arrayOf(plantId.toString()))
            if (updated == 0) return false

            insertLogRow(db, WateringLog(plantId = plantId, plantName = plant.displayName, wateredTimestamp = wateredAt, notes = notes))
            db.setTransactionSuccessful()
            return true
        } finally {
            db.endTransaction()
        }
    }

    /** Waters every plant that is due today or overdue. Returns how many plants were watered. */
    fun markAllDueAsWatered(): Int {
        val due = getPlantsNeedingWater()
        var count = 0
        for (plant in due) {
            if (markAsWatered(plant.id)) count++
        }
        return count
    }

    /** Watering history for one plant, newest first. */
    fun getWateringHistory(plantId: Long): List<WateringLog> {
        return queryLogs("$COL_H_PLANT_ID = ?", arrayOf(plantId.toString()), null)
    }

    /** Watering history across all plants, newest first. Pass [limit] to cap the result size. */
    fun getAllWateringHistory(limit: Int? = null): List<WateringLog> {
        return queryLogs(null, null, limit?.toString())
    }

    fun deleteWateringLog(logId: Long): Boolean {
        return writableDatabase.delete(TABLE_HISTORY, "$COL_H_ID = ?", arrayOf(logId.toString())) > 0
    }

    fun getTotalWaterings(): Int {
        return DatabaseUtils.queryNumEntries(readableDatabase, TABLE_HISTORY).toInt()
    }

    /**
     * Consecutive days (ending today, or yesterday if nothing is watered yet today)
     * on which at least one plant was watered.
     */
    fun getCurrentStreakDays(): Int {
        val days = getWateringDayStarts()
        if (days.isEmpty()) return 0

        val today = startOfDay(System.currentTimeMillis())
        val yesterday = addDays(today, -1)
        var cursorDay = when {
            days.contains(today) -> today
            days.contains(yesterday) -> yesterday
            else -> return 0
        }
        var streak = 0
        while (days.contains(cursorDay)) {
            streak++
            cursorDay = addDays(cursorDay, -1)
        }
        return streak
    }

    /** Longest run of consecutive watering days ever recorded. */
    fun getLongestStreakDays(): Int {
        val sortedDays = getWateringDayStarts().sorted()
        var longest = 0
        var run = 0
        var previous: Long? = null
        for (day in sortedDays) {
            run = if (previous != null && addDays(previous, 1) == day) run + 1 else 1
            if (run > longest) longest = run
            previous = day
        }
        return longest
    }

    /** Number of distinct plants watered today. */
    fun getWateredTodayCount(): Int {
        val now = System.currentTimeMillis()
        return DatabaseUtils.longForQuery(
            readableDatabase,
            "SELECT COUNT(DISTINCT $COL_H_PLANT_ID) FROM $TABLE_HISTORY WHERE $COL_H_WATERED >= ? AND $COL_H_WATERED <= ?",
            arrayOf(startOfDay(now).toString(), endOfDay(now).toString())
        ).toInt()
    }

    /** Completed vs. total watering tasks for today (watered today + still due or overdue). */
    fun getTodayProgress(): CareProgress {
        val completed = getWateredTodayCount()
        val pending = getPlantsNeedingWater().size
        return CareProgress(completed = completed, total = completed + pending)
    }

    // ---------------------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------------------

    private fun queryPlants(selection: String?, args: Array<String>?): List<Plant> {
        val plants = mutableListOf<Plant>()
        readableDatabase.query(
            TABLE_PLANTS, null, selection, args, null, null, "$COL_NEXT_WATERING ASC, $COL_NAME COLLATE NOCASE ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) plants.add(cursor.toPlant())
        }
        return plants
    }

    private fun queryLogs(selection: String?, args: Array<String>?, limit: String?): List<WateringLog> {
        val logs = mutableListOf<WateringLog>()
        readableDatabase.query(
            TABLE_HISTORY, null, selection, args, null, null, "$COL_H_WATERED DESC, $COL_H_ID DESC", limit
        ).use { cursor ->
            while (cursor.moveToNext()) {
                logs.add(
                    WateringLog(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_H_ID)),
                        plantId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_H_PLANT_ID)),
                        plantName = cursor.getString(cursor.getColumnIndexOrThrow(COL_H_PLANT_NAME)),
                        wateredTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_H_WATERED)),
                        notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_H_NOTES))
                    )
                )
            }
        }
        return logs
    }

    /** Distinct local-midnight timestamps of every day that has at least one watering. */
    private fun getWateringDayStarts(): Set<Long> {
        val days = HashSet<Long>()
        readableDatabase.rawQuery("SELECT $COL_H_WATERED FROM $TABLE_HISTORY", null).use { cursor ->
            while (cursor.moveToNext()) days.add(startOfDay(cursor.getLong(0)))
        }
        return days
    }

    private fun Cursor.toPlant(): Plant {
        val next = getLong(getColumnIndexOrThrow(COL_NEXT_WATERING))
        val storedStatus = getString(getColumnIndexOrThrow(COL_HEALTH))
        return Plant(
            id = getLong(getColumnIndexOrThrow(COL_ID)),
            name = getString(getColumnIndexOrThrow(COL_NAME)),
            species = getString(getColumnIndexOrThrow(COL_SPECIES)),
            nickname = getString(getColumnIndexOrThrow(COL_NICKNAME)),
            room = getString(getColumnIndexOrThrow(COL_ROOM)),
            wateringFrequencyDays = getInt(getColumnIndexOrThrow(COL_FREQUENCY)),
            lastWateredTimestamp = getLong(getColumnIndexOrThrow(COL_LAST_WATERED)),
            nextWateringTimestamp = next,
            sunlight = getString(getColumnIndexOrThrow(COL_SUNLIGHT)),
            notes = getString(getColumnIndexOrThrow(COL_NOTES)),
            imageUri = getString(getColumnIndexOrThrow(COL_IMAGE)),
            healthStatus = resolveStatus(storedStatus, next)
        )
    }

    /** Derives overdue / needs-water / healthy from the schedule unless the manual flag is set. */
    private fun resolveStatus(storedStatus: String, nextWatering: Long): String {
        if (storedStatus == HealthStatus.ATTENTION) return HealthStatus.ATTENTION
        val now = System.currentTimeMillis()
        return when {
            nextWatering < startOfDay(now) -> HealthStatus.OVERDUE
            nextWatering <= endOfDay(now) -> HealthStatus.NEEDS_WATER
            else -> HealthStatus.HEALTHY
        }
    }

    private fun plantValues(plant: Plant): ContentValues {
        return ContentValues().apply {
            put(COL_NAME, plant.name.trim())
            put(COL_SPECIES, plant.species.trim())
            put(COL_NICKNAME, plant.nickname.trim())
            put(COL_ROOM, plant.room.trim())
            put(COL_FREQUENCY, plant.wateringFrequencyDays.coerceAtLeast(1))
            put(COL_LAST_WATERED, plant.lastWateredTimestamp)
            put(COL_NEXT_WATERING, plant.nextWateringTimestamp)
            put(COL_SUNLIGHT, plant.sunlight.trim())
            put(COL_NOTES, plant.notes.trim())
            put(COL_IMAGE, plant.imageUri)
            // Only the manual flag is persisted as-is; schedule statuses are re-derived on read.
            put(
                COL_HEALTH,
                if (plant.healthStatus == HealthStatus.ATTENTION) HealthStatus.ATTENTION else HealthStatus.HEALTHY
            )
        }
    }

    private fun insertPlantRow(db: SQLiteDatabase, plant: Plant): Long {
        val frequency = plant.wateringFrequencyDays.coerceAtLeast(1)
        val now = System.currentTimeMillis()
        val next = when {
            plant.nextWateringTimestamp > 0L -> plant.nextWateringTimestamp
            plant.lastWateredTimestamp > 0L -> addDays(plant.lastWateredTimestamp, frequency)
            else -> now
        }
        val values = plantValues(plant.copy(wateringFrequencyDays = frequency, nextWateringTimestamp = next))
        return db.insert(TABLE_PLANTS, null, values)
    }

    private fun insertLogRow(db: SQLiteDatabase, log: WateringLog): Long {
        val values = ContentValues().apply {
            put(COL_H_PLANT_ID, log.plantId)
            put(COL_H_PLANT_NAME, log.plantName)
            put(COL_H_WATERED, log.wateredTimestamp)
            put(COL_H_NOTES, log.notes)
        }
        return db.insert(TABLE_HISTORY, null, values)
    }

    companion object {
        const val DATABASE_NAME = "bloomie.db"
        const val DATABASE_VERSION = 1

        const val TABLE_PLANTS = "plants"
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_SPECIES = "scientific_name"
        const val COL_NICKNAME = "nickname"
        const val COL_ROOM = "room"
        const val COL_FREQUENCY = "watering_frequency_days"
        const val COL_LAST_WATERED = "last_watered_date"
        const val COL_NEXT_WATERING = "next_watering_date"
        const val COL_SUNLIGHT = "light_requirement"
        const val COL_NOTES = "notes"
        const val COL_IMAGE = "image_path"
        const val COL_HEALTH = "health_status"

        const val TABLE_HISTORY = "watering_history"
        const val COL_H_ID = "id"
        const val COL_H_PLANT_ID = "plant_id"
        const val COL_H_PLANT_NAME = "plant_name"
        const val COL_H_WATERED = "watered_date"
        const val COL_H_NOTES = "notes"

        /** Local midnight at the start of the day containing [timestamp]. */
        fun startOfDay(timestamp: Long): Long {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }

        /** The last millisecond of the day containing [timestamp]. */
        fun endOfDay(timestamp: Long): Long {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = startOfDay(timestamp)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            return calendar.timeInMillis - 1L
        }

        /** Adds (or subtracts, when negative) whole calendar days, safe across DST changes. */
        fun addDays(timestamp: Long, days: Int): Long {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timestamp
            calendar.add(Calendar.DAY_OF_YEAR, days)
            return calendar.timeInMillis
        }
    }
}
