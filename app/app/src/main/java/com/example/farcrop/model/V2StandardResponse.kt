package com.example.farcrop.model

import com.google.gson.annotations.SerializedName

data class V2StandardResponse(
    val success: Boolean,
    @SerializedName("api_version") val apiVersion: String?,
    val stage: String,
    val code: String,
    val message: String,
    val data: PredictionPayload?
)

data class PredictionPayload(
    val prediction: PredictionData?,
    @SerializedName("crop_cycle") val cropCycle: CropCycleMetadata?,
    @SerializedName("generated_at") val generatedAt: String?
)

data class PredictionData(
    val disease: String,
    val confidence: Double,
    val cause: String,
    val description: String,
    val treatment: List<String> = emptyList(),
    val products: List<String> = emptyList(),
    val explanation: String,
    @SerializedName("buy_links") val buyLinks: List<BuyLink> = emptyList()
)

data class CropCycleMetadata(
    @SerializedName("cycle_id") val cycleId: String,
    @SerializedName("cycle_name") val cycleName: String,
    @SerializedName("crop_name") val cropName: String,
    @SerializedName("plantation_date") val plantationDate: String,
    @SerializedName("estimated_harvest_date") val estimatedHarvestDate: String,
    @SerializedName("photo_timestamp") val photoTimestamp: String,
    val latitude: Double?,
    val longitude: Double?,
    val notes: String?,
    val treatments: List<CropCycleTreatment> = emptyList()
)

data class CropCycleTreatment(
    val name: String,
    val date: String
)

data class BuyLink(
    val title: String,
    val url: String,
    val thumbnail: String? = null,
    val brand: String? = null,
    val price: String? = null,
    val rating: String? = null
)
