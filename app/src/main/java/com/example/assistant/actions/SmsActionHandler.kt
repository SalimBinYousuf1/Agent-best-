package com.example.assistant.actions

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

data class ResolvedContact(
    val displayName: String,
    val phoneNumber: String
)

sealed class SmsExecutionResult {
    data class SentDirectly(val userMessage: String, val recipient: String) : SmsExecutionResult()
    data class FallbackComposerOpened(val userMessage: String, val recipient: String) : SmsExecutionResult()
    data class AmbiguousContact(val userMessage: String, val candidates: List<ResolvedContact>) : SmsExecutionResult()
    data class ContactNotFound(val userMessage: String) : SmsExecutionResult()
    data class Error(val userMessage: String) : SmsExecutionResult()
}

class SmsActionHandler(private val context: Context) {

    /**
     * Resolves contact name to phone numbers.
     * Detects ambiguous contacts (multiple matches) to prevent dispatch to wrong person.
     */
    fun lookupContactNumbers(contactQuery: String): List<ResolvedContact> {
        val results = mutableListOf<ResolvedContact>()
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasPermission || contactQuery.isBlank()) {
            return emptyList()
        }

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$contactQuery%")

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
            // Read error handled safely
        } finally {
            cursor?.close()
        }
        return results.distinctBy { it.phoneNumber }
    }

    fun isValidPhoneNumber(input: String): Boolean {
        val clean = input.replace(Regex("[^0-9+]"), "")
        return clean.length >= 3 && PhoneNumberUtils.isGlobalPhoneNumber(clean)
    }

    /**
     * Executes SMS sending. If direct SMS is enabled and SEND_SMS permission is held,
     * sends directly autonomously. Otherwise opens Messages pre-filled.
     */
    fun executeSendSms(
        recipientName: String?,
        rawPhoneNumber: String?,
        messageBody: String,
        directSmsEnabled: Boolean,
        isAutonomous: Boolean
    ): SmsExecutionResult {
        if (messageBody.isBlank()) {
            return SmsExecutionResult.Error("Text message content is empty.")
        }

        // Determine destination number
        val targetNumber: String
        val targetDisplayName: String

        if (!rawPhoneNumber.isNullOrBlank() && isValidPhoneNumber(rawPhoneNumber)) {
            targetNumber = rawPhoneNumber
            targetDisplayName = recipientName ?: rawPhoneNumber
        } else if (!recipientName.isNullOrBlank()) {
            val contacts = lookupContactNumbers(recipientName)
            when {
                contacts.isEmpty() -> {
                    return SmsExecutionResult.ContactNotFound("I could not find '$recipientName' in your contacts.")
                }
                contacts.size > 1 -> {
                    // Ambiguous contact!
                    val names = contacts.take(3).joinToString(", ") { "${it.displayName} (${it.phoneNumber})" }
                    return SmsExecutionResult.AmbiguousContact(
                        userMessage = "I found multiple numbers for '$recipientName': $names. Which one would you like to text?",
                        candidates = contacts
                    )
                }
                else -> {
                    targetNumber = contacts.first().phoneNumber
                    targetDisplayName = contacts.first().displayName
                }
            }
        } else {
            return SmsExecutionResult.Error("Please specify a recipient name or phone number.")
        }

        val hasSendSmsPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.SEND_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        // Direct SMS path
        if (directSmsEnabled && hasSendSmsPermission && isAutonomous) {
            try {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                val parts = smsManager.divideMessage(messageBody)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(targetNumber, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(targetNumber, null, messageBody, null, null)
                }

                return SmsExecutionResult.SentDirectly(
                    userMessage = "Text sent directly to $targetDisplayName.",
                    recipient = targetDisplayName
                )
            } catch (e: Exception) {
                // Fall back to composer intent
            }
        }

        // Fallback: Open system SMS app pre-filled
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$targetNumber")
                putExtra("sms_body", messageBody)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            SmsExecutionResult.FallbackComposerOpened(
                userMessage = "Messages opened for $targetDisplayName with your text ready to send.",
                recipient = targetDisplayName
            )
        } catch (e: Exception) {
            SmsExecutionResult.Error("Could not launch messaging application.")
        }
    }
}
