package com.example.assistant.actions

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import androidx.core.content.ContextCompat

sealed class CallExecutionResult {
    data class DirectCallPlaced(val userMessage: String, val recipient: String) : CallExecutionResult()
    data class DialerOpened(val userMessage: String, val recipient: String) : CallExecutionResult()
    data class EmergencySafetyEnforced(val userMessage: String) : CallExecutionResult()
    data class AmbiguousContact(val userMessage: String, val candidates: List<ResolvedContact>) : CallExecutionResult()
    data class ContactNotFound(val userMessage: String) : CallExecutionResult()
    data class Error(val userMessage: String) : CallExecutionResult()
}

class CallActionHandler(private val context: Context) {

    companion object {
        val EMERGENCY_NUMBERS = setOf(
            "911", "112", "999", "000", "110", "119", "100", "101", "102", "108"
        )
    }

    fun isEmergencyNumber(number: String): Boolean {
        val clean = number.replace(Regex("[^0-9]"), "")
        return EMERGENCY_NUMBERS.contains(clean) || PhoneNumberUtils.isEmergencyNumber(clean)
    }

    fun lookupContact(query: String): List<ResolvedContact> {
        val results = mutableListOf<ResolvedContact>()
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasPermission || query.isBlank()) {
            return emptyList()
        }

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )
            if (cursor != null) {
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIdx)
                    val rawNumber = cursor.getString(numberIdx)
                    val cleanNumber = rawNumber.replace(Regex("[^0-9+]"), "")
                    if (cleanNumber.isNotBlank()) {
                        results.add(ResolvedContact(name, cleanNumber))
                    }
                }
            }
        } catch (e: Exception) {
            // Safe handling
        } finally {
            cursor?.close()
        }
        return results.distinctBy { it.phoneNumber }
    }

    /**
     * Executes phone call.
     * CRITICAL SECURITY RULE: Emergency numbers are ALWAYS routed through ACTION_DIAL,
     * never placed directly automatically.
     */
    fun executeCall(
        recipientName: String?,
        rawPhoneNumber: String?,
        directCallEnabled: Boolean,
        isAutonomous: Boolean
    ): CallExecutionResult {
        val targetNumber: String
        val targetDisplayName: String

        if (!rawPhoneNumber.isNullOrBlank()) {
            targetNumber = rawPhoneNumber.replace(Regex("[^0-9+]"), "")
            targetDisplayName = recipientName ?: targetNumber
        } else if (!recipientName.isNullOrBlank()) {
            val contacts = lookupContact(recipientName)
            when {
                contacts.isEmpty() -> {
                    return CallExecutionResult.ContactNotFound("I could not find '$recipientName' in your contacts.")
                }
                contacts.size > 1 -> {
                    val names = contacts.take(3).joinToString(", ") { "${it.displayName} (${it.phoneNumber})" }
                    return CallExecutionResult.AmbiguousContact(
                        userMessage = "I found multiple phone numbers for '$recipientName': $names. Which would you like to call?",
                        candidates = contacts
                    )
                }
                else -> {
                    targetNumber = contacts.first().phoneNumber
                    targetDisplayName = contacts.first().displayName
                }
            }
        } else {
            return CallExecutionResult.Error("Please specify who to call.")
        }

        // Emergency safety override
        if (isEmergencyNumber(targetNumber)) {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$targetNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            return CallExecutionResult.EmergencySafetyEnforced(
                userMessage = "Emergency number dialed on phone screen for your safety."
            )
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        // Direct call only if direct calling enabled + permission held + autonomous mode active
        if (directCallEnabled && hasCallPermission && isAutonomous) {
            return try {
                val callIntent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$targetNumber")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(callIntent)
                CallExecutionResult.DirectCallPlaced(
                    userMessage = "Calling $targetDisplayName directly.",
                    recipient = targetDisplayName
                )
            } catch (e: Exception) {
                // Fallback to dialer
                launchDialer(targetNumber, targetDisplayName)
            }
        }

        return launchDialer(targetNumber, targetDisplayName)
    }

    private fun launchDialer(targetNumber: String, targetDisplayName: String): CallExecutionResult {
        return try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$targetNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            CallExecutionResult.DialerOpened(
                userMessage = "Dialer opened for $targetDisplayName.",
                recipient = targetDisplayName
            )
        } catch (e: Exception) {
            CallExecutionResult.Error("Could not launch phone dialer.")
        }
    }
}
