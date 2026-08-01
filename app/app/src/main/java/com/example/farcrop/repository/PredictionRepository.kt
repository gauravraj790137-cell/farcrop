package com.example.farcrop.repository

import android.content.Context
import com.example.farcrop.model.PredictionResponse
import com.example.farcrop.network.RetrofitClient
import com.example.farcrop.utils.PreferenceManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class PredictionRepository(private val context: Context) {
    private val preferenceManager = PreferenceManager(context)

    suspend fun uploadImage(imageFile: File): PredictionResponse {
        val apiService = RetrofitClient.create(preferenceManager.getBaseUrl())
        // Must be an explicit MIME type — the backend rejects wildcards like "image/*"
        val mimeType = when (imageFile.extension.lowercase()) {
            "png"  -> "image/png"
            "webp" -> "image/webp"
            else   -> "image/jpeg"   // covers .jpg, .jpeg, and any unnamed temp file
        }
        val requestBody = imageFile.asRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("image", imageFile.name, requestBody)
        return apiService.uploadImage(part)
    }
}
