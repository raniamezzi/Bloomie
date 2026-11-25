package com.example.bloomie.network

import com.example.bloomie.models.PlantNetResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/** Retrofit definition of the Pl@ntNet identification endpoint. */
interface PlantNetApiService {

    /**
     * Identifies the plant on one photo.
     *
     * @param project Pl@ntNet flora project, for example "all".
     * @param apiKey your Pl@ntNet API key.
     * @param images the photo as a multipart part named "images".
     * @param organs the plant organ shown in the photo ("auto", "leaf", "flower", ...).
     */
    @Multipart
    @POST("v2/identify/{project}")
    fun identify(
        @Path("project") project: String,
        @Query("api-key") apiKey: String,
        @Query("lang") language: String,
        @Query("nb-results") maxResults: Int,
        @Part images: MultipartBody.Part,
        @Part("organs") organs: RequestBody
    ): Call<PlantNetResponse>
}
