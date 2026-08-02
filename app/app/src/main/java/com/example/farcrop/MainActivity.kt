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
import com.example.farcrop.model.V2StandardResponse
import com.example.farcrop.ui.screens.CaptureInspectionScreen
import com.example.farcrop.ui.screens.CreateCropCycleScreen
import com.example.farcrop.ui.screens.CropCycleDetailsScreen
import com.example.farcrop.ui.screens.MainScreenContainer
import com.example.farcrop.ui.screens.ResultScreen
import com.example.farcrop.ui.theme.FarCropTheme
import com.example.farcrop.ui.viewmodel.CropCycleViewModel
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
                    val viewModel = remember { CropCycleViewModel(applicationContext) }

                    NavHost(
                        navController = navController,
                        startDestination = "main"
                    ) {
                        composable("main") {
                            MainScreenContainer(
                                viewModel = viewModel,
                                onNavigateToCreateCycle = {
                                    navController.navigate("create_crop_cycle")
                                },
                                onNavigateToCycleDetails = { cycleId ->
                                    navController.navigate("crop_cycle_details/$cycleId")
                                }
                            )
                        }

                        composable("create_crop_cycle") {
                            CreateCropCycleScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("crop_cycle_details/{cycleId}") { backStackEntry ->
                            val cycleId = backStackEntry.arguments?.getString("cycleId") ?: ""
                            CropCycleDetailsScreen(
                                viewModel = viewModel,
                                cycleId = cycleId,
                                onNavigateToCapture = {
                                    navController.navigate("capture_inspection/$cycleId")
                                },
                                onNavigateToResult = { response, cId ->
                                    val json = gson.toJson(response)
                                    val encoded = java.net.URLEncoder.encode(json, "UTF-8")
                                    navController.navigate("result/$encoded/$cId")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("capture_inspection/{cycleId}") { backStackEntry ->
                            val cycleId = backStackEntry.arguments?.getString("cycleId") ?: ""
                            CaptureInspectionScreen(
                                viewModel = viewModel,
                                cycleId = cycleId,
                                onNavigateToResult = { response, cId ->
                                    val json = gson.toJson(response)
                                    val encoded = java.net.URLEncoder.encode(json, "UTF-8")
                                    navController.navigate("result/$encoded/$cId") {
                                        popUpTo("crop_cycle_details/$cId") { inclusive = false }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("result/{responseJson}/{cycleId}") { backStackEntry ->
                            val encoded = backStackEntry.arguments?.getString("responseJson") ?: ""
                            val cycleId = backStackEntry.arguments?.getString("cycleId") ?: ""
                            val json = java.net.URLDecoder.decode(encoded, "UTF-8")
                            val response = gson.fromJson(json, V2StandardResponse::class.java)
                            ResultScreen(
                                response = response,
                                cycleId = cycleId,
                                onBack = {
                                    navController.popBackStack("crop_cycle_details/$cycleId", inclusive = false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

