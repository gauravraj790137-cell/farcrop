package com.example.farcrop.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.farcrop.model.PredictionResponse
import com.example.farcrop.repository.PredictionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class UploadState {
    object Idle : UploadState()
    object Loading : UploadState()
    data class Success(val response: PredictionResponse) : UploadState()
    data class Error(val message: String) : UploadState()
}

class HomeViewModel : ViewModel() {

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    private val _cameraUri = MutableStateFlow<Uri?>(null)
    val cameraUri: StateFlow<Uri?> = _cameraUri.asStateFlow()

    /** Creates a temp file in cache/camera/ and returns a FileProvider URI for the camera intent. */
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

    /** Copy a content URI (gallery pick) to a temp cache file and upload it. */
    fun uploadFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Loading
            try {
                val tempFile = copyUriToTempFile(context, uri)
                val repo = PredictionRepository(context)
                val response = repo.uploadImage(tempFile)
                _uploadState.value = UploadState.Success(response)
                tempFile.delete()
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(
                    e.message ?: "Upload failed. Check your server address."
                )
            }
        }
    }

    /** Upload the image captured by the camera (already written to cameraUri). */
    fun uploadFromCamera(context: Context) {
        val uri = _cameraUri.value ?: run {
            _uploadState.value = UploadState.Error("Camera image not found.")
            return
        }
        uploadFromUri(context, uri)
    }

    fun resetState() {
        _uploadState.value = UploadState.Idle
    }

    private fun copyUriToTempFile(context: Context, uri: Uri): File {
        val cacheDir = File(context.cacheDir, "camera").also { it.mkdirs() }
        // Use an explicit .jpg extension so the repository can derive the correct MIME type
        val tempFile = File.createTempFile("UPLOAD_", ".jpg", cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Cannot open image URI.")
        return tempFile
    }
}
