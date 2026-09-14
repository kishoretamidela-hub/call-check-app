package com.callcheck.app.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import java.net.URLEncoder

@Composable
fun PhoneEntryScreen(nav: NavHostController, vm: PhoneAuthViewModel = viewModel_stub()) {
    val context = LocalContext.current
    var digits by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Verify your number", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("A real SMS code will be sent via Firebase — this is not simulated.")
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = digits,
            onValueChange = { digits = it.filter(Char::isDigit).take(10) },
            label = { Text("10-digit mobile number") },
            prefix = { Text("+91 ") },
            modifier = Modifier.fillMaxWidth()
        )
        if (vm.errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(vm.errorMessage!!, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = digits.length == 10 && !vm.isLoading,
            onClick = {
                val e164 = "+91$digits"
                val activity = context as android.app.Activity
                vm.sendOtp(
                    activity = activity,
                    e164Phone = e164,
                    onCodeSent = { verificationId ->
                        val encodedPhone = URLEncoder.encode(e164, "UTF-8")
                        nav.navigate("otp/$verificationId/$encodedPhone")
                    },
                    onAutoVerified = { nav.navigate("home") }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (vm.isLoading) "Sending…" else "Send code")
        }
    }
}

// Small helper so this file compiles standalone in the snippet; in the real
// project use androidx.lifecycle.viewmodel.compose.viewModel() instead.
@Composable
private fun viewModel_stub(): PhoneAuthViewModel = remember { PhoneAuthViewModel() }
