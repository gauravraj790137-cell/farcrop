package com.example.farcrop.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.farcrop.gatekeeper.Gatekeeper
import com.example.farcrop.model.V2StandardResponse
import com.example.farcrop.ui.viewmodel.CropCycleViewModel
import com.example.farcrop.ui.viewmodel.V2UploadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureInspectionScreen(
    viewModel: CropCycleViewModel,
    cycleId: String,
    onNavigateToResult: (V2StandardResponse, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uploadState by viewModel.uploadState.collectAsState()
    val cameraUri by viewModel.cameraUri.collectAsState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var blurCheckPassed by remember { mutableStateOf(false) }
    var blurErrorReason by remember { mutableStateOf<String?>(null) }

    // Navigate to result on success
    LaunchedEffect(uploadState) {
        if (uploadState is V2UploadState.Success) {
            val response = (uploadState as V2UploadState.Success).response
            onNavigateToResult(response, cycleId)
            viewModel.resetUploadState()
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraUri != null) {
            selectedImageUri = cameraUri
            val result = runLocalChecks(context, cameraUri!!)
            blurCheckPassed = result.first
            blurErrorReason = result.second
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            val result = runLocalChecks(context, uri)
            blurCheckPassed = result.first
            blurErrorReason = result.second
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = viewModel.createCameraUri(context)
            cameraLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            val uri = viewModel.createCameraUri(context)
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leaf Scanner", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetUploadState()
                        onBack()
                    }) {
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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedImageUri == null) {
                // Capture placeholder box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "No photo captured yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { launchCamera() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Capture from Camera", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Collections, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select from Gallery", fontWeight = FontWeight.Bold)
                }

            } else {
                // Image preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Captured Leaf",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Quality check status card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Image Analysis",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        when {
                            blurErrorReason != null -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.WarningAmber,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        "Quality Check: Failed",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    blurErrorReason ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            blurCheckPassed -> {
                                Text(
                                    "✓ Image Quality: Sharp & Clear",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            else -> {
                                Text("Running on-device quality check…")
                            }
                        }
                    }
                }

                // Action buttons based on state
                if (blurErrorReason != null) {
                    Button(
                        onClick = {
                            selectedImageUri = null
                            blurCheckPassed = false
                            blurErrorReason = null
                            viewModel.resetUploadState()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Try Another Photo", fontWeight = FontWeight.Bold)
                    }
                } else if (blurCheckPassed) {
                    when (uploadState) {
                        is V2UploadState.Uploading, is V2UploadState.Checking -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("AI analysis in progress…")
                                }
                            }
                        }
                        is V2UploadState.Rejected -> {
                            val msg = (uploadState as V2UploadState.Rejected).reason
                            AlertDialog(
                                onDismissRequest = { viewModel.resetUploadState() },
                                title = { Text("Photo Rejected") },
                                text = { Text(msg) },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.resetUploadState()
                                        selectedImageUri = null
                                        blurCheckPassed = false
                                        blurErrorReason = null
                                    }) {
                                        Text("Retry")
                                    }
                                }
                            )
                        }
                        is V2UploadState.Error -> {
                            val msg = (uploadState as V2UploadState.Error).message
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    msg,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = {
                                        viewModel.uploadInspection(context, selectedImageUri!!, cycleId)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Retry Upload")
                                }
                            }
                        }
                        else -> {
                            Button(
                                onClick = {
                                    viewModel.uploadInspection(context, selectedImageUri!!, cycleId)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "Analyze Crop Health",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Run the on-device blur/sharpness gatekeeper against the given URI.
 * Returns (passed, reason): reason is null when the check passes.
 */
private fun runLocalChecks(context: Context, uri: Uri): Pair<Boolean, String?> {
    return try {
        val bytes: ByteArray? = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes == null || bytes.isEmpty()) {
            return Pair(false, "Could not load image. Please try again.")
        }
        val result = Gatekeeper(context).analyzeBytes(bytes)
        if (result.passed) Pair(true, null) else Pair(false, result.reason)
    } catch (e: Exception) {
        Pair(false, e.message ?: "Quality check failed.")
    }
}
