package com.example.farcrop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.farcrop.model.PredictionResponse
import com.example.farcrop.ui.screens.HomeScreen
import com.example.farcrop.ui.screens.ResultScreen
import com.example.farcrop.ui.screens.SettingsScreen
import com.example.farcrop.ui.theme.FarCropTheme
import com.google.gson.Gson

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FarCropTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val gson = remember { Gson() }

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                onNavigateToResult = { response ->
                                    // Encode the response as JSON and pass via nav argument
                                    val json = gson.toJson(response)
                                    val encoded = java.net.URLEncoder.encode(json, "UTF-8")
                                    navController.navigate("result/$encoded")
                                }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("result/{responseJson}") { backStackEntry ->
                            val encoded = backStackEntry.arguments?.getString("responseJson") ?: ""
                            val json = java.net.URLDecoder.decode(encoded, "UTF-8")
                            val response = gson.fromJson(json, PredictionResponse::class.java)
                            ResultScreen(
                                response = response,
                                onBack = {
                                    navController.popBackStack("home", inclusive = false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
