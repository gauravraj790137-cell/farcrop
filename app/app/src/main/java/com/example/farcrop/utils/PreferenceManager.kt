package com.example.farcrop.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("farcrop_prefs", Context.MODE_PRIVATE)

    /** Returns the stored ip:port string, e.g. "192.168.1.20:8000" */
    fun getServerAddress(): String {
        return prefs.getString(SERVER_ADDRESS_KEY, DEFAULT_ADDRESS) ?: DEFAULT_ADDRESS
    }

    fun saveServerAddress(address: String) {
        prefs.edit().putString(SERVER_ADDRESS_KEY, address.trim()).apply()
    }

    /** Builds a full http base URL from the stored ip:port */
    fun getBaseUrl(): String {
        val address = getServerAddress().trimEnd('/')
        return if (address.startsWith("http://") || address.startsWith("https://")) {
            "$address/"
        } else {
            "http://$address/"
        }
    }

    fun getServerIP(): String {
        val address = getServerAddress()
        return address.substringBefore(":", "192.168.1.20")
    }

    fun getPort(): String {
        val address = getServerAddress()
        return address.substringAfter(":", "8000")
    }

    fun getApiRoute(): String {
        return prefs.getString(API_ROUTE_KEY, DEFAULT_API_ROUTE) ?: DEFAULT_API_ROUTE
    }

    fun saveServerSettings(ip: String, port: String, apiRoute: String) {
        val cleanIp = ip.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        val address = "$cleanIp:${port.trim()}"
        prefs.edit()
            .putString(SERVER_ADDRESS_KEY, address)
            .putString(API_ROUTE_KEY, apiRoute.trim())
            .apply()
    }

    fun getCropCyclesJson(): String {
        return prefs.getString(CROP_CYCLES_KEY, "[]") ?: "[]"
    }

    fun saveCropCyclesJson(json: String) {
        prefs.edit().putString(CROP_CYCLES_KEY, json).apply()
    }

    companion object {
        private const val SERVER_ADDRESS_KEY = "server_address"
        private const val API_ROUTE_KEY = "api_route"
        private const val CROP_CYCLES_KEY = "crop_cycles"
        private const val DEFAULT_ADDRESS = "192.168.1.20:8000"
        private const val DEFAULT_API_ROUTE = "predict"
    }
}

