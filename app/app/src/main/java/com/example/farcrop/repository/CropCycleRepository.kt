package com.example.farcrop.repository

import android.content.Context
import com.example.farcrop.model.CropCycle
import com.example.farcrop.utils.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CropCycleRepository(context: Context) {
    private val preferenceManager = PreferenceManager(context)
    private val gson = Gson()

    private val _cropCycles = MutableStateFlow<List<CropCycle>>(emptyList())
    val cropCycles: StateFlow<List<CropCycle>> = _cropCycles.asStateFlow()

    init {
        loadCropCycles()
    }

    private fun loadCropCycles() {
        val json = preferenceManager.getCropCyclesJson()
        val type = object : TypeToken<List<CropCycle>>() {}.type
        val list: List<CropCycle> = runCatching {
            gson.fromJson<List<CropCycle>>(json, type)
        }.getOrNull() ?: emptyList()
        _cropCycles.value = list
    }

    fun saveCropCycles(list: List<CropCycle>) {
        _cropCycles.value = list
        val json = gson.toJson(list)
        preferenceManager.saveCropCyclesJson(json)
    }

    fun addCropCycle(cycle: CropCycle) {
        val current = _cropCycles.value.toMutableList()
        current.add(cycle)
        saveCropCycles(current)
    }

    fun updateCropCycle(cycle: CropCycle) {
        val current = _cropCycles.value.map {
            if (it.id == cycle.id) cycle else it
        }
        saveCropCycles(current)
    }

    fun getCropCycle(id: String): CropCycle? {
        return _cropCycles.value.find { it.id == id }
    }
}
