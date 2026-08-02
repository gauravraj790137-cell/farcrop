package com.example.farcrop.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.farcrop.model.CropCycle
import com.example.farcrop.model.Treatment
import com.example.farcrop.ui.viewmodel.CropCycleViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCropCycleScreen(
    viewModel: CropCycleViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var cropName by remember { mutableStateOf("") }
    var cycleName by remember { mutableStateOf("") }
    var plantationDate by remember { mutableStateOf("") }
    var estimatedHarvestDate by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var notes by remember { mutableStateOf("") }
    
    // Treatments
    var treatments = remember { mutableStateListOf<Treatment>() }
    var newTreatmentName by remember { mutableStateOf("") }
    var newTreatmentDate by remember { mutableStateOf("") }

    var expandedCropDropdown by remember { mutableStateOf(false) }
    val supportedCrops = listOf("Tomato", "Potato", "Pepper bell")

    var showError by remember { mutableStateOf<String?>(null) }
    var isDetectingLocation by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isDetectingLocation = true
            viewModel.autoDetectLocation(context,
                onResult = { lat, lng ->
                    latitude = lat
                    longitude = lng
                    isDetectingLocation = false
                },
                onError = { err ->
                    showError = err
                    isDetectingLocation = false
                }
            )
        } else {
            showError = "Location permissions are required to auto-detect GPS."
        }
    }

    fun pickDate(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val calendarSelected = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                onDateSelected(sdf.format(calendarSelected.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    fun triggerAutoDetect() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun saveCycle() {
        if (cropName.isBlank() || cycleName.isBlank() || plantationDate.isBlank() || estimatedHarvestDate.isBlank()) {
            showError = "Please fill in all required fields (marked *)."
            return
        }
        
        val newCycle = CropCycle(
            cropName = cropName,
            cycleName = cycleName,
            plantationDate = plantationDate,
            estimatedHarvestDate = estimatedHarvestDate,
            latitude = latitude,
            longitude = longitude,
            notes = notes.ifBlank { null },
            treatments = treatments.toList()
        )
        
        viewModel.addCropCycle(newCycle)
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Crop Cycle", fontWeight = FontWeight.Bold) },
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
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Basic Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // Crop Selection Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = cropName,
                            onValueChange = { cropName = it },
                            label = { Text("Crop Name *") },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedCropDropdown = true },
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                TextButton(onClick = { expandedCropDropdown = true }) {
                                    Text("Select")
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = expandedCropDropdown,
                            onDismissRequest = { expandedCropDropdown = false }
                        ) {
                            supportedCrops.forEach { crop ->
                                DropdownMenuItem(
                                    text = { Text(crop) },
                                    onClick = {
                                        cropName = crop
                                        expandedCropDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = cycleName,
                        onValueChange = { cycleName = it },
                        label = { Text("Cycle Name / Field Label *") },
                        placeholder = { Text("e.g. Field A, Tomato patch 1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Plantation Date
                    OutlinedTextField(
                        value = plantationDate,
                        onValueChange = { },
                        label = { Text("Plantation Date *") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.clickable {
                                pickDate { plantationDate = it }
                            })
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pickDate { plantationDate = it } },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Harvest Date
                    OutlinedTextField(
                        value = estimatedHarvestDate,
                        onValueChange = { },
                        label = { Text("Estimated Harvest Date *") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.clickable {
                                pickDate { estimatedHarvestDate = it }
                            })
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pickDate { estimatedHarvestDate = it } },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Location card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("GPS Coordinates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Button(
                        onClick = { triggerAutoDetect() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isDetectingLocation) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Detecting GPS...")
                        } else {
                            Icon(Icons.Filled.MyLocation, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Auto-Detect GPS Location")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = latitude?.toString() ?: "",
                            onValueChange = { latitude = it.toDoubleOrNull() },
                            label = { Text("Latitude") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = longitude?.toString() ?: "",
                            onValueChange = { longitude = it.toDoubleOrNull() },
                            label = { Text("Longitude") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Notes Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Farmer Notes (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("Write down any details, organic methods, soil condition...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Treatments Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Treatments Applied (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    if (treatments.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            treatments.forEach { t ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(t.name, fontWeight = FontWeight.Bold)
                                        Text(t.date, style = MaterialTheme.typography.labelSmall)
                                    }
                                    IconButton(onClick = { treatments.remove(t) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    // Add new treatment block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTreatmentName,
                            onValueChange = { newTreatmentName = it },
                            label = { Text("Treatment Name") },
                            placeholder = { Text("e.g. Neem Oil Spray") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = newTreatmentDate,
                            onValueChange = {},
                            label = { Text("Date") },
                            readOnly = true,
                            trailingIcon = {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.clickable {
                                    pickDate { newTreatmentDate = it }
                                })
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { pickDate { newTreatmentDate = it } },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (newTreatmentName.isNotBlank() && newTreatmentDate.isNotBlank()) {
                                treatments.add(Treatment(name = newTreatmentName, date = newTreatmentDate))
                                newTreatmentName = ""
                                newTreatmentDate = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Treatment")
                    }
                }
            }

            // Error display
            if (showError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(showError ?: "", color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        TextButton(onClick = { showError = null }) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = { saveCycle() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Create Crop Cycle", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
