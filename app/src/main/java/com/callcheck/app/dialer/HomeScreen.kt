package com.callcheck.app.dialer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Real dialer + contacts. Tapping a contact fires an actual ACTION_CALL
 * intent (requires CALL_PHONE permission granted, and will place a real
 * call through the device's telephony stack — this is not a mockup).
 */
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    val contacts = remember { mutableStateOf(emptyList<Contact>()) }

    LaunchedEffect(Unit) {
        // In the real app, request READ_CONTACTS at runtime before this call.
        contacts.value = ContactsRepository.loadContacts(context)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Contacts") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Call protection") })
        }
        Spacer(Modifier.height(12.dp))

        when (tab) {
            0 -> LazyColumn {
                items(contacts.value) { contact ->
                    ListItem(
                        headlineContent = { Text(contact.name) },
                        supportingContent = { Text(contact.number) },
                        trailingContent = {
                            TextButton(onClick = {
                                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${contact.number}"))
                                context.startActivity(intent) // real call placed
                            }) { Text("Call") }
                        }
                    )
                }
            }
            1 -> CallProtectionStatus()
        }
    }
}

@Composable
fun CallProtectionStatus() {
    Column {
        Text("Two protections, two different guarantees:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Text("1. Spam/scam number screening — real, automatic, works for every call.")
        Text("Enable in Settings → Apps → Default apps → Caller ID & spam blocking.")
        Spacer(Modifier.height(16.dp))
        Text("2. Live voice-authenticity check — opt-in per call, requires speakerphone, and only analyzes what your mic picks up. See in-call screen for details and its real limits.")
    }
}
