package com.example.farcrop.model

import java.util.UUID

data class CropCycle(
    val id: String = UUID.randomUUID().toString(),
    val cropName: String,
    val cycleName: String,
    val plantationDate: String, // YYYY-MM-DD
    val estimatedHarvestDate: String, // YYYY-MM-DD
    val latitude: Double?,
    val longitude: Double?,
    val notes: String?,
    val treatments: List<Treatment> = emptyList(),
    val inspections: List<Inspection> = emptyList()
)

data class Treatment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val date: String // YYYY-MM-DD
)

data class Inspection(
    val id: String = UUID.randomUUID().toString(),
    val date: String, // YYYY-MM-DD HH:MM
    val disease: String,
    val confidence: Double,
    val severity: String,
    val cause: String,
    val description: String,
    val explanation: String,
    val treatmentSteps: List<String>,
    val recommendedProducts: List<String>,
    val imagePath: String? = null
)
