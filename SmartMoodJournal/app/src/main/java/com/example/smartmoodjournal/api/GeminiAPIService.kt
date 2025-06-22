package com.example.smartmoodjournal.api

import com.example.smartmoodjournal.model.GeminiRequest
import com.example.smartmoodjournal.model.GeminiResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiAPIService {
    @POST("v1beta/models/gemini-pro:generateContent")
    fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Call<GeminiResponse>
}
