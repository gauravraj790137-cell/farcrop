package com.example.farcrop.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farcrop.gatekeeper.Gatekeeper
import com.example.farcrop.model.CropCycle
import com.example.farcrop.model.Inspection
import com.example.farcrop.model.V2StandardResponse
import com.example.farcrop.repository.CropCycleRepository
import com.example.farcrop.repository.GatekeeperRejectedException
import com.example.farcrop.repository.PredictionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class V2UploadState {
    object Idle : V2UploadState()
    object Checking : V2UploadState()
    object Uploading : V2UploadState()
    data class Rejected(val reason: String) : V2UploadState()
    data class Success(val response: V2StandardResponse) : V2UploadState()
    data class Error(val message: String) : V2UploadState()
}

sealed class ConnectionTestState {
    object Idle : ConnectionTestState()
    object Testing : ConnectionTestState()
    object Success : ConnectionTestState()
    data class Error(val message: String) : ConnectionTestState()
}

class CropCycleViewModel(context: Context) : ViewModel() {

    private val repository = CropCycleRepository(context)
    private val predictionRepository = PredictionRepository(context)

    val cropCycles: StateFlow<List<CropCycle>> = repository.cropCycles

    private val _uploadState = MutableStateFlow<V2UploadState>(V2UploadState.Idle)
    val uploadState: StateFlow<V2UploadState> = _uploadState.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val connectionState: StateFlow<ConnectionTestState> = _connectionState.asStateFlow()

    private val _cameraUri = MutableStateFlow<Uri?>(null)
    val cameraUri: StateFlow<Uri?> = _cameraUri.asStateFlow()

    fun createCameraUri(context: Context): Uri {
        val cameraDir = File(context.cacheDir, "camera").also { it.mkdirs() }
        val tempFile = File.createTempFile("IMG_", ".jpg", cameraDir)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        _cameraUri.value = uri
        return uri
    }

    fun addCropCycle(cropCycle: CropCycle) {
        viewModelScope.launch {
            repository.addCropCycle(cropCycle)
        }
    }

    fun getCropCycle(id: String): CropCycle? {
        return repository.getCropCycle(id)
    }

    @SuppressLint("MissingPermission")
    fun autoDetectLocation(
        context: Context,
        onResult: (latitude: Double, longitude: Double) -> Unit,
        onError: (String) -> Unit
    ) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        try {
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                    bestLocation = loc
                }
            }
            if (bestLocation != null) {
                onResult(bestLocation.latitude, bestLocation.longitude)
                return
            }

            val activeProvider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                LocationManager.GPS_PROVIDER
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                LocationManager.NETWORK_PROVIDER
            } else {
                null
            }

            if (activeProvider == null) {
                onError("No active GPS or Network provider found. Please turn on location services.")
                return
            }

            @Suppress("DEPRECATION")
            locationManager.requestSingleUpdate(activeProvider, object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    onResult(location.latitude, location.longitude)
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }, null)
        } catch (e: SecurityException) {
            onError("Location permissions not granted.")
        } catch (e: Exception) {
            onError(e.message ?: "Failed to get location.")
        }
    }

    fun testConnection(ip: String, port: String) {
        viewModelScope.launch {
            _connectionState.value = ConnectionTestState.Testing
            val success = predictionRepository.testConnection(ip, port)
            if (success) {
                _connectionState.value = ConnectionTestState.Success
            } else {
                _connectionState.value = ConnectionTestState.Error("Connection failed. Verify address & server state.")
            }
        }
    }

    fun resetConnectionState() {
        _connectionState.value = ConnectionTestState.Idle
    }

    fun resetUploadState() {
        _uploadState.value = V2UploadState.Idle
    }

    fun uploadInspection(context: Context, uri: Uri, cropCycleId: String) {
        viewModelScope.launch {
            _uploadState.value = V2UploadState.Checking

            val imageBytes = withContext(Dispatchers.IO) {
                readUriBytes(context, uri)
            }

            if (imageBytes == null || imageBytes.isEmpty()) {
                _uploadState.value = V2UploadState.Error("Could not read image file.")
                return@launch
            }

            // On-device blur check
            val gatekeeperResult = withContext(Dispatchers.IO) {
                Gatekeeper(context).analyzeBytes(imageBytes)
            }

            if (!gatekeeperResult.passed) {
                _uploadState.value = V2UploadState.Rejected(gatekeeperResult.reason)
                return@launch
            }

            _uploadState.value = V2UploadState.Uploading

            val cropCycle = repository.getCropCycle(cropCycleId) ?: run {
                _uploadState.value = V2UploadState.Error("Crop cycle not found.")
                return@launch
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())

            try {
                val response = predictionRepository.uploadImage(imageBytes, cropCycle, timestamp)
                
                if (response.success && response.data?.prediction != null) {
                    val prediction = response.data.prediction
                    val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    
                    val isHealthy = prediction.disease.contains("healthy", ignoreCase = true)
                    val severity = when {
                        isHealthy -> "None"
                        prediction.disease.contains("late_blight", ignoreCase = true) -> "High"
                        prediction.disease.contains("early_blight", ignoreCase = true) -> "Medium"
                        prediction.disease.contains("spot", ignoreCase = true) -> "Medium"
                        else -> "Low"
                    }

                    val newInspection = Inspection(
                        date = dateFormatted,
                        disease = prediction.disease,
                        confidence = prediction.confidence,
                        severity = severity,
                        cause = prediction.cause,
                        description = prediction.description,
                        explanation = prediction.explanation,
                        treatmentSteps = prediction.treatment,
                        recommendedProducts = prediction.products,
                        imagePath = uri.toString()
                    )

                    val updatedInspections = cropCycle.inspections.toMutableList()
                    updatedInspections.add(newInspection)
                    repository.updateCropCycle(cropCycle.copy(inspections = updatedInspections))

                    _uploadState.value = V2UploadState.Success(response)
                } else {
                    _uploadState.value = V2UploadState.Error(response.message)
                }
            } catch (e: GatekeeperRejectedException) {
                _uploadState.value = V2UploadState.Rejected(e.message ?: "Unsupported crop leaf.")
            } catch (e: Exception) {
                _uploadState.value = V2UploadState.Error(e.message ?: "Upload failed. Verify server is online.")
            }
        }
    }

    private fun readUriBytes(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }
}
