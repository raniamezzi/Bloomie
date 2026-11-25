package com.example.bloomie.data.db

import android.content.Context
import com.example.bloomie.models.CoachData
import com.google.gson.Gson
import com.google.gson.JsonParseException
import java.io.IOException

/** Loads the Care Coach rules from `assets/coach_rules.json`. */
object CoachRepository {

    @Volatile
    private var cache: CoachData? = null

    /** Returns the rules, or null if the file is missing or malformed. */
    fun load(context: Context): CoachData? {
        cache?.let { return it }

        val data: CoachData? = try {
            context.applicationContext.assets.open("coach_rules.json").bufferedReader().use { reader ->
                Gson().fromJson(reader, CoachData::class.java)
            }
        } catch (e: IOException) {
            null
        } catch (e: JsonParseException) {
            null
        }

        if (data != null && data.symptoms.isNotEmpty()) cache = data
        return cache
    }
}
