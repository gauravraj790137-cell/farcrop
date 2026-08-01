package com.example.farcrop.model

import com.google.gson.annotations.SerializedName

data class PredictionResponse(
    val disease: String,
    val confidence: Double,
    val cause: String,
    val description: String,
    val treatment: List<String>,
    val products: List<String>,
    val explanation: String,
    @SerializedName("buy_links") val buyLinks: List<BuyLink>
)

data class BuyLink(
    val title: String,
    val url: String
)
