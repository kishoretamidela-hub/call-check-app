package com.callcheck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.callcheck.app.auth.OtpScreen
import com.callcheck.app.auth.PhoneEntryScreen
import com.callcheck.app.dialer.HomeScreen
import com.callcheck.app.ui.theme.CallCheckTheme

/**
 * Real navigation shell. No simulated screens here — every route below
 * calls into actual Android APIs (Firebase Auth, ContactsContract,
 * ACTION_CALL, CallScreeningService). Search for "LIMITATION:" comments
 * throughout this project for the handful of places Android itself
 * blocks a fully "genuine" implementation.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CallCheckTheme {
                AppNavHost()
            }
        }
    }
}

@Composable
fun AppNavHost(nav: NavHostController = rememberNavController()) {
    NavHost(navController = nav, startDestination = "phone_entry") {
        composable("phone_entry") { PhoneEntryScreen(nav) }
        composable("otp/{verificationId}/{phone}") { backStackEntry ->
            val verificationId = backStackEntry.arguments?.getString("verificationId") ?: ""
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            OtpScreen(nav, verificationId, phone)
        }
        composable("home") { HomeScreen() }
    }
}
