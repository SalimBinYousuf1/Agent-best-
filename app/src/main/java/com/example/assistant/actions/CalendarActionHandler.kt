package com.example.assistant.actions

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.TimeZone

data class CalendarEventParams(
    val title: String,
    val startTimeMillis: Long,
    val durationMinutes: Int = 60,
    val location: String? = null,
    val description: String? = null
)

sealed class CalendarExecutionResult {
    data class InsertedDirectly(val userMessage: String, val eventId: Long) : CalendarExecutionResult()
    data class FallbackIntentLaunched(val userMessage: String) : CalendarExecutionResult()
    data class ConflictDetected(val userMessage: String, val existingEventTitle: String) : CalendarExecutionResult()
    data class Error(val userMessage: String) : CalendarExecutionResult()
}

class CalendarActionHandler(private val context: Context) {

    fun parseCalendarEvent(params: Map<String, Any?>): CalendarEventParams {
        val title = (params["title"] as? String)?.takeIf { it.isNotBlank() } ?: "Scheduled Event"
        val location = params["location"] as? String
        val description = params["description"] as? String

        val startMillis = when (val raw = params["start_time_millis"]) {
            is Number -> raw.toLong().takeIf { it > System.currentTimeMillis() }
            else -> null
        } ?: run {
            val cal = Calendar.getInstance()
            val daysAhead = (params["days_ahead"] as? Number)?.toInt() ?: 0
            val hour = (params["hour"] as? Number)?.toInt() ?: (cal.get(Calendar.HOUR_OF_DAY) + 1)
            val minute = (params["minute"] as? Number)?.toInt() ?: 0

            cal.add(Calendar.DAY_OF_YEAR, daysAhead)
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }

        val durationMinutes = (params["duration_minutes"] as? Number)?.toInt() ?: 60

        return CalendarEventParams(
            title = title,
            startTimeMillis = startMillis,
            durationMinutes = durationMinutes,
            location = location,
            description = description
        )
    }

    /**
     * Checks for conflict or duplicate events if READ_CALENDAR permission is granted.
     */
    fun checkExistingConflict(startTimeMillis: Long, endTimeMillis: Long): String? {
        val hasReadPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasReadPermission) return null

        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
        )
        val selection = "(${CalendarContract.Events.DTSTART} < ?) AND (${CalendarContract.Events.DTEND} > ?)"
        val selectionArgs = arrayOf(endTimeMillis.toString(), startTimeMillis.toString())

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val titleIndex = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                if (titleIndex != -1) {
                    return cursor.getString(titleIndex)
                }
            }
        } catch (e: Exception) {
            // Ignore if query fails
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * Inserts event directly if WRITE_CALENDAR is held, otherwise launches pre-filled system intent.
     */
    fun executeCreateEvent(
        event: CalendarEventParams,
        autonomous: Boolean
    ): CalendarExecutionResult {
        val endTimeMillis = event.startTimeMillis + (event.durationMinutes * 60 * 1000L)

        // Conflict check
        val conflict = checkExistingConflict(event.startTimeMillis, endTimeMillis)
        if (conflict != null && !autonomous) {
            return CalendarExecutionResult.ConflictDetected(
                userMessage = "You already have '$conflict' scheduled around this time.",
                existingEventTitle = conflict
            )
        }

        val hasWritePermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasWritePermission && autonomous) {
            try {
                // Find primary calendar ID
                var calendarId: Long = 1L
                val calCursor = context.contentResolver.query(
                    CalendarContract.Calendars.CONTENT_URI,
                    arrayOf(CalendarContract.Calendars._ID),
                    CalendarContract.Calendars.VISIBLE + " = 1",
                    null,
                    null
                )
                if (calCursor != null) {
                    if (calCursor.moveToFirst()) {
                        calendarId = calCursor.getLong(0)
                    }
                    calCursor.close()
                }

                val values = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.TITLE, event.title)
                    put(CalendarContract.Events.DESCRIPTION, event.description ?: "Created by Salim Assistant")
                    put(CalendarContract.Events.EVENT_LOCATION, event.location ?: "")
                    put(CalendarContract.Events.DTSTART, event.startTimeMillis)
                    put(CalendarContract.Events.DTEND, endTimeMillis)
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                }

                val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                if (uri != null) {
                    val id = uri.lastPathSegment?.toLongOrNull() ?: 1L
                    return CalendarExecutionResult.InsertedDirectly(
                        userMessage = "Event '${event.title}' scheduled autonomously.",
                        eventId = id
                    )
                }
            } catch (e: Exception) {
                // Fallback to pre-filled intent below
            }
        }

        // Fallback to pre-filled intent
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, event.title)
                putExtra(CalendarContract.Events.DESCRIPTION, event.description ?: "Created by Salim Assistant")
                putExtra(CalendarContract.Events.EVENT_LOCATION, event.location ?: "")
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startTimeMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTimeMillis)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CalendarExecutionResult.FallbackIntentLaunched(
                userMessage = "Calendar opened with '${event.title}' pre-filled for review."
            )
        } catch (e: Exception) {
            CalendarExecutionResult.Error(
                userMessage = "Calendar permission is required for direct event creation."
            )
        }
    }
}
