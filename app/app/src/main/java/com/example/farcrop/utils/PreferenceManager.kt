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

    companion object {
        private const val SERVER_ADDRESS_KEY = "server_address"
        private const val DEFAULT_ADDRESS = "192.168.1.20:8000"
    }
}
