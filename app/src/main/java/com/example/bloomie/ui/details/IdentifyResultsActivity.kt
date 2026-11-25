package com.example.bloomie.ui.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloomie.R
import com.example.bloomie.data.db.GuideRepository
import com.example.bloomie.databinding.ActivityIdentifyResultsBinding
import com.example.bloomie.models.IdentifyMatch
import com.example.bloomie.network.PlantIdentifier
import com.example.bloomie.ui.main.PlantImageLoader
import com.example.bloomie.ui.main.ScreenLauncher
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken

/**
 * Screen 10: the scanned photo and the species candidates. "Save to My Garden" opens the
 * add/edit form pre-filled with the selected species and the photo.
 */
class IdentifyResultsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIdentifyResultsBinding
    private lateinit var matchAdapter: IdentifyMatchAdapter

    private var imagePath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdentifyResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH).orEmpty()
        val source = intent.getStringExtra(EXTRA_SOURCE).orEmpty()
        val matches = parseMatches(intent.getStringExtra(EXTRA_MATCHES_JSON))

        binding.btnBack.setOnClickListener { finish() }
        PlantImageLoader.load(binding.imgScanned, imagePath)
        bindNotice(source)

        matchAdapter = IdentifyMatchAdapter(matches) { }
        binding.recyclerMatches.layoutManager = LinearLayoutManager(this)
        binding.recyclerMatches.adapter = matchAdapter

        val hasMatches = matches.isNotEmpty()
        binding.recyclerMatches.visibility = if (hasMatches) View.VISIBLE else View.GONE
        binding.txtHint.visibility = if (hasMatches) View.VISIBLE else View.GONE
        binding.txtEmpty.visibility = if (hasMatches) View.GONE else View.VISIBLE
        binding.btnSave.visibility = if (hasMatches) View.VISIBLE else View.GONE

        binding.btnSave.setOnClickListener { saveSelectedMatch() }
    }

    private fun bindNotice(source: String) {
        val messageRes = when (source) {
            PlantIdentifier.SOURCE_PLANTNET -> R.string.results_notice_live
            PlantIdentifier.SOURCE_DEMO_OFFLINE -> R.string.results_notice_demo_offline
            else -> R.string.results_notice_demo_no_key
        }
        binding.txtNotice.setText(messageRes)
        binding.txtNotice.setBackgroundResource(
            if (source == PlantIdentifier.SOURCE_PLANTNET) R.drawable.bg_badge_healthy else R.drawable.bg_badge_warning
        )
    }

    private fun saveSelectedMatch() {
        val match = matchAdapter.selectedMatch() ?: return

        // If the species is in the Care Library, reuse its watering and light advice.
        val guide = GuideRepository.findByScientificName(this, match.scientificName)
        val sunlight = guide?.let { GuideRepository.sunlightOptionFor(this, it.sunlight) }.orEmpty()
        val wateringDays = guide?.let { GuideRepository.wateringDaysFor(it) } ?: DEFAULT_WATERING_DAYS

        ScreenLauncher.openAddEditPlantPrefilled(
            context = this,
            name = match.displayName,
            species = match.scientificName,
            imagePath = imagePath,
            sunlight = sunlight,
            wateringDays = wateringDays
        )
    }

    private fun parseMatches(json: String?): List<IdentifyMatch> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<IdentifyMatch>>() {}.type
            Gson().fromJson<List<IdentifyMatch>>(json, type).orEmpty()
        } catch (e: JsonParseException) {
            emptyList()
        }
    }

    companion object {
        private const val EXTRA_IMAGE_PATH = "identify_image_path"
        private const val EXTRA_MATCHES_JSON = "identify_matches_json"
        private const val EXTRA_SOURCE = "identify_source"
        private const val DEFAULT_WATERING_DAYS = 7

        /** Opens the results screen for [imagePath] with the given [matches]. */
        fun start(context: Context, imagePath: String, matches: List<IdentifyMatch>, source: String) {
            val intent = Intent(context, IdentifyResultsActivity::class.java)
                .putExtra(EXTRA_IMAGE_PATH, imagePath)
                .putExtra(EXTRA_MATCHES_JSON, Gson().toJson(matches))
                .putExtra(EXTRA_SOURCE, source)
            context.startActivity(intent)
        }
    }
}
