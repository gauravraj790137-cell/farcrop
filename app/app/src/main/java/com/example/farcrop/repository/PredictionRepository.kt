package com.example.farcrop.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.farcrop.model.CropCycle
import com.example.farcrop.model.V2StandardResponse
import com.example.farcrop.network.RetrofitClient
import com.example.farcrop.utils.PreferenceManager
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.ByteArrayOutputStream

/**
 * Thrown when the backend Gatekeeper rejects the image (HTTP 451).
 */
class GatekeeperRejectedException(message: String) : Exception(message)

class PredictionRepository(private val context: Context) {
    private val preferenceManager = PreferenceManager(context)
    private val gson = Gson()

    suspend fun uploadImage(
        imageBytes: ByteArray,
        cropCycle: CropCycle,
        photoTimestamp: String
    ): V2StandardResponse {
        if (imageBytes.isEmpty()) {
            throw IllegalStateException("Image is empty. Please try again.")
        }

        // ── Re-encode to a clean, fresh JPEG ────────────────────────────────
        // This clears any EXIF, HEIC/RAW artifacts, partial JPEG headers, or
        // wrong content-types that could cause a 400 from the backend validator.
        val jpegBytes = reEncodeAsJpeg(imageBytes)
            ?: throw IllegalStateException("Could not decode image. Please try a different photo.")

        // ── Build MultipartBody manually ─────────────────────────────────────
        // Using @Body + manual MultipartBody is safer than Retrofit's @Part
        // with nullable RequestBody — null @Part values can corrupt the boundary
        // or send empty parts that confuse FastAPI's form parser.
        val multipartBody = buildMultipart(jpegBytes, cropCycle, photoTimestamp)

        // ── Resolve the upload URL ───────────────────────────────────────────
        val baseUrl = preferenceManager.getBaseUrl()   // e.g. "http://192.168.1.20:8000/"
        val route   = preferenceManager.getApiRoute()  // e.g. "predict" or "api/predict"
        // Build an absolute URL so @Url resolves correctly regardless of base path
        val uploadUrl = baseUrl.trimEnd('/') + "/" + route.trimStart('/')

        val apiService = RetrofitClient.create(baseUrl)

        return try {
            apiService.uploadImage(url = uploadUrl, body = multipartBody)
        } catch (e: HttpException) {
            when (e.code()) {
                451 -> {
                    val errorBody = e.response()?.errorBody()?.string()
                    val rejection = runCatching {
                        gson.fromJson(errorBody, V2StandardResponse::class.java)
                    }.getOrNull()
                    val reason = rejection?.message
                        ?: "This image does not appear to contain a supported crop leaf."
                    throw GatekeeperRejectedException(reason)
                }
                else -> throw e
            }
        }
    }

    suspend fun testConnection(ip: String, port: String): Boolean {
        val cleanIp = ip.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .trimEnd('/')
        val baseUrl = "http://$cleanIp:${port.trim()}/"
        val apiService = RetrofitClient.create(baseUrl)
        return try {
            apiService.checkHealth().success
        } catch (e: Exception) {
            false
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Decode [src] with BitmapFactory then compress back to JPEG at 92 % quality.
     * Returns null if the bytes cannot be decoded as an image at all.
     */
    private fun reEncodeAsJpeg(src: ByteArray): ByteArray? {
        val bitmap = BitmapFactory.decodeByteArray(src, 0, src.size) ?: return null
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    /**
     * Build a multipart/form-data body that exactly matches the FastAPI endpoint:
     *
     *   image            – JPEG file part (required)
     *   cycle_id         – text/plain
     *   cycle_name       – text/plain
     *   crop_name        – text/plain
     *   plantation_date  – text/plain
     *   estimated_harvest_date – text/plain
     *   photo_timestamp  – text/plain
     *   latitude         – text/plain  (omitted when null)
     *   longitude        – text/plain  (omitted when null)
     *   notes            – text/plain  (omitted when null)
     *   treatments       – application/json (JSON array string)
     */
    private fun buildMultipart(
        jpegBytes: ByteArray,
        cropCycle: CropCycle,
        photoTimestamp: String
    ): RequestBody {
        val appJson   = "application/json; charset=utf-8".toMediaType()
        val imageJpeg = "image/jpeg".toMediaType()

        val treatmentsList = cropCycle.treatments.map { mapOf("name" to it.name, "date" to it.date) }
        val treatmentsJson = gson.toJson(treatmentsList)

        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)

        // Required image part — filename must end in .jpg so FastAPI reads content_type correctly
        builder.addFormDataPart(
            "image",
            "leaf_${System.currentTimeMillis()}.jpg",
            jpegBytes.toRequestBody(imageJpeg)
        )

        // Required text fields — use simple string overload (sets text/plain automatically)
        builder.addFormDataPart("cycle_id",              cropCycle.id)
        builder.addFormDataPart("cycle_name",            cropCycle.cycleName)
        builder.addFormDataPart("crop_name",             cropCycle.cropName)
        builder.addFormDataPart("plantation_date",       cropCycle.plantationDate)
        builder.addFormDataPart("estimated_harvest_date", cropCycle.estimatedHarvestDate)
        builder.addFormDataPart("photo_timestamp",        photoTimestamp)

        // Optional fields — only added when non-null/non-blank to avoid empty form parts
        cropCycle.latitude?.let  { builder.addFormDataPart("latitude",  it.toString()) }
        cropCycle.longitude?.let { builder.addFormDataPart("longitude", it.toString()) }
        cropCycle.notes?.let     { if (it.isNotBlank()) builder.addFormDataPart("notes", it) }

        // Treatments JSON array — send as application/json part
        builder.addPart(
            MultipartBody.Part.createFormData("treatments", null,
                treatmentsJson.toByteArray(Charsets.UTF_8).toRequestBody(appJson))
        )

        return builder.build()
    }
}
