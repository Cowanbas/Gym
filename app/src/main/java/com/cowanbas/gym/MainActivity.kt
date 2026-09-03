package com.cowanbas.gym

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import com.cowanbas.gym.ui.screens.MainHomeScreen
import com.cowanbas.gym.ui.theme.GymTheme
import com.cowanbas.gym.ui.theme.LocalAdvancedMode
import com.cowanbas.gym.ui.theme.LocalWeightUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("gym_store", Context.MODE_PRIVATE) }

            var advancedMode by remember { mutableStateOf(prefs.getBoolean("advanced_sets", false)) }
            var weightUnit by remember { mutableStateOf(prefs.getString("weight_unit", "kg") ?: "kg") }

            CompositionLocalProvider(
                LocalAdvancedMode provides advancedMode,
                LocalWeightUnit provides weightUnit
            ) {
                GymTheme {
                    MainHomeScreen(
                        onAdvancedChange = {
                            advancedMode = it
                            prefs.edit().putBoolean("advanced_sets", it).apply()
                        },
                        onUnitChange = {
                            weightUnit = it
                            prefs.edit().putString("weight_unit", it).apply()
                        }
                    )
                }
            }
        }
    }
}