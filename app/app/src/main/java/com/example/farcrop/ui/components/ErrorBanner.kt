package com.example.farcrop.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ErrorBanner(message: String) {
    Text(text = message, color = MaterialTheme.colorScheme.error)
}
