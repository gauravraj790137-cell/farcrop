package com.example.farcrop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.farcrop.model.CropCycle
import com.example.farcrop.model.Inspection
import com.example.farcrop.model.V2StandardResponse
import com.example.farcrop.model.PredictionPayload
import com.example.farcrop.model.PredictionData
import com.example.farcrop.model.BuyLink
import com.example.farcrop.ui.viewmodel.CropCycleViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropCycleDetailsScreen(
    viewModel: CropCycleViewModel,
    cycleId: String,
    onNavigateToCapture: () -> Unit,
    onNavigateToResult: (V2StandardResponse, String) -> Unit,
    onBack: () -> Unit
) {
    val cropCycles by viewModel.cropCycles.collectAsState()
    val cycle = cropCycles.find { it.id == cycleId }

    if (cycle == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Crop cycle not found.")
        }
        return
    }

    val progress = getTimelineProgress(cycle.plantationDate, cycle.estimatedHarvestDate)
    val progressPct = (progress * 100).toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cycle.cycleName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCapture,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Leaf", fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = cycle.cropName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Field ID: ${cycle.id.take(8).uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Timeline Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Growth Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Planted", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(cycle.plantationDate, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "$progressPct%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Harvest (Est)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(cycle.estimatedHarvestDate, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Notes Card
            if (cycle.notes != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Farmer Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(cycle.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // GPS Location Card
            if (cycle.latitude != null && cycle.longitude != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Lat: ${cycle.latitude}, Lng: ${cycle.longitude}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Treatments Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Treatments Applied", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    if (cycle.treatments.isEmpty()) {
                        Text("No treatments added to this cycle.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        cycle.treatments.forEach { t ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(t.name, fontWeight = FontWeight.Medium)
                                Text(t.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Inspections History Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Inspection History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    if (cycle.inspections.isEmpty()) {
                        Text("No health scans recorded yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            cycle.inspections.reversed().forEach { ins ->
                                val isInsHealthy = ins.disease.contains("healthy", ignoreCase = true)
                                val statusColor = if (isInsHealthy) MaterialTheme.colorScheme.secondary else Color(0xFFC62828)
                                val label = if (isInsHealthy) "Healthy" else ins.disease.replace("___", " — ").replace("_", " ")

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // Convert local Inspection back to V2StandardResponse structure to open on ResultScreen
                                            val std = V2StandardResponse(
                                                success = true,
                                                apiVersion = "2.0",
                                                stage = "prediction",
                                                code = "SUCCESS",
                                                message = "Completed",
                                                data = PredictionPayload(
                                                    prediction = PredictionData(
                                                        disease = ins.disease,
                                                        confidence = ins.confidence,
                                                        cause = ins.cause,
                                                        description = ins.description,
                                                        treatment = ins.treatmentSteps,
                                                        products = ins.recommendedProducts,
                                                        explanation = ins.explanation,
                                                        buyLinks = emptyList()
                                                    ),
                                                    cropCycle = null,
                                                    generatedAt = ins.date
                                                )
                                            )
                                            onNavigateToResult(std, cycleId)
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isInsHealthy) Icons.Filled.Favorite else Icons.Filled.Warning,
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(label, fontWeight = FontWeight.Bold, color = statusColor)
                                            Text(ins.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${(ins.confidence * 1f).toInt()}%", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // padding for FAB
        }
    }
}

private fun getTimelineProgress(plantationDateStr: String, harvestDateStr: String): Float {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val plantation = sdf.parse(plantationDateStr)?.time ?: return 0f
        val harvest = sdf.parse(harvestDateStr)?.time ?: return 0f
        val today = Date().time

        if (today <= plantation) return 0f
        if (today >= harvest) return 1f

        val total = harvest - plantation
        val current = today - plantation
        (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    } catch (e: Exception) {
        0.5f
    }
}
