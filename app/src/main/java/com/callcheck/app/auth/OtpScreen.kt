package com.callcheck.app.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun OtpScreen(nav: NavHostController, verificationId: String, phone: String, vm: PhoneAuthViewModel = remember { PhoneAuthViewModel() }) {
    var code by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Enter the code", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Sent to $phone by Firebase — check your real SMS inbox.")
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { code = it.filter(Char::isDigit).take(6) },
            label = { Text("6-digit code") },
            modifier = Modifier.fillMaxWidth()
        )
        if (vm.errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(vm.errorMessage!!, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = code.length == 6 && !vm.isLoading,
            onClick = {
                vm.verifyOtp(verificationId, code) {
                    nav.navigate("home") { popUpTo("phone_entry") { inclusive = true } }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (vm.isLoading) "Verifying…" else "Verify")
        }
    }
}
