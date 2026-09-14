package com.callcheck.app.dialer

import android.content.Context
import android.provider.ContactsContract

data class Contact(val name: String, val number: String)

/**
 * Reads REAL contacts off the device via ContactsContract. Requires the
 * READ_CONTACTS runtime permission to have been granted (request it from
 * the UI layer before calling this — omitted here for brevity).
 */
object ContactsRepository {
    fun loadContacts(context: Context): List<Contact> {
        val results = mutableListOf<Contact>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                results.add(Contact(it.getString(nameIdx) ?: "Unknown", it.getString(numberIdx) ?: ""))
            }
        }
        return results.distinctBy { c -> c.number }
    }
}
