package com.example.bloomie.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.bloomie.data.db.GuideRepository
import com.example.bloomie.models.IdentifyMatch
import com.example.bloomie.models.PlantNetResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import kotlin.math.abs

/**
 * Identifies a plant photo with Pl@ntNet. If no API key is configured, or the request fails for
 * any reason, it falls back to demo matches taken from the Care Library so the flow always works.
 * Results are always delivered on the main thread.
 */
object PlantIdentifier {

    /** Real Pl@ntNet results. */
    const val SOURCE_PLANTNET = "plantnet"

    /** Demo results because no API key has been configured. */
    const val SOURCE_DEMO_NO_KEY = "demo_no_key"

    /** Demo results because the network request failed. */
    const val SOURCE_DEMO_OFFLINE = "demo_offline"

    private const val MAX_RESULTS = 5
    private const val DEMO_DELAY_MS = 1200L

    class Result(val matches: List<IdentifyMatch>, val source: String)

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Starts an identification and returns the in-flight request (null when demo results are used)
     * so callers can cancel it when their screen goes away.
     */
    fun identify(context: Context, imageFile: File, onResult: (Result) -> Unit): Call<PlantNetResponse>? {
        val appContext = context.applicationContext

        if (!RetrofitClient.isApiKeyConfigured) {
            deliverDemo(appContext, imageFile, SOURCE_DEMO_NO_KEY, onResult)
            return null
        }

        val mimeType = when (imageFile.extension.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
        val imagePart = MultipartBody.Part.createFormData(
            "images",
            imageFile.name,
            imageFile.asRequestBody(mimeType.toMediaType())
        )
        val organs = "auto".toRequestBody("text/plain".toMediaType())

        val call = RetrofitClient.plantNetService.identify(
            RetrofitClient.PROJECT,
            RetrofitClient.API_KEY,
            "en",
            MAX_RESULTS,
            imagePart,
            organs
        )

        call.enqueue(object : Callback<PlantNetResponse> {
            override fun onResponse(call: Call<PlantNetResponse>, response: Response<PlantNetResponse>) {
                val matches = if (response.isSuccessful) toMatches(response.body()) else emptyList()
                if (matches.isNotEmpty()) {
                    onResult(Result(matches, SOURCE_PLANTNET))
                } else {
                    deliverDemo(appContext, imageFile, SOURCE_DEMO_OFFLINE, onResult)
                }
            }

            override fun onFailure(call: Call<PlantNetResponse>, t: Throwable) {
                if (call.isCanceled) return
                deliverDemo(appContext, imageFile, SOURCE_DEMO_OFFLINE, onResult)
            }
        })
        return call
    }

    private fun toMatches(body: PlantNetResponse?): List<IdentifyMatch> {
        val results = body?.results.orEmpty()
        return results
            .sortedByDescending { it.score ?: 0.0 }
            .take(MAX_RESULTS)
            .mapNotNull { result ->
                val species = result.species ?: return@mapNotNull null
                val scientific = species.scientificNameWithoutAuthor
                    ?: species.scientificName
                    ?: return@mapNotNull null
                IdentifyMatch(
                    scientificName = scientific,
                    commonName = species.commonNames?.firstOrNull().orEmpty(),
                    family = species.family?.scientificNameWithoutAuthor.orEmpty(),
                    score = (result.score ?: 0.0).coerceIn(0.0, 1.0)
                )
            }
    }

    private fun deliverDemo(
        context: Context,
        imageFile: File,
        source: String,
        onResult: (Result) -> Unit
    ) {
        val matches = demoMatches(context, imageFile)
        mainHandler.postDelayed({ onResult(Result(matches, source)) }, DEMO_DELAY_MS)
    }

    /** Picks three library plants based on the photo, so the same photo always gives the same demo. */
    private fun demoMatches(context: Context, imageFile: File): List<IdentifyMatch> {
        val guides = GuideRepository.load(context)
        if (guides.isEmpty()) return emptyList()

        val seed = abs(imageFile.length() + imageFile.name.hashCode().toLong())
        val start = (seed % guides.size).toInt()
        val scores = listOf(0.93, 0.74, 0.58)

        return scores.mapIndexed { index, score ->
            val guide = guides[(start + index * 3) % guides.size]
            IdentifyMatch(
                scientificName = guide.scientificName,
                commonName = guide.commonName,
                family = "",
                score = score
            )
        }.distinctBy { it.scientificName }
    }
}
