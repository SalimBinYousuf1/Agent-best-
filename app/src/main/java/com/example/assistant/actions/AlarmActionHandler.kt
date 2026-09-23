package com.example.assistant.actions

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import java.util.Calendar
import java.util.Locale

data class AlarmParseResult(
    val hour: Int, // 0-23
    val minute: Int, // 0-59
    val message: String,
    val isTomorrowRollover: Boolean
)

sealed class AlarmExecutionResult {
    data class Success(val userMessage: String, val hour: Int, val minute: Int, val isRollover: Boolean) : AlarmExecutionResult()
    data class NeedsExactAlarmPermission(val userMessage: String) : AlarmExecutionResult()
    data class FallbackLaunched(val userMessage: String) : AlarmExecutionResult()
    data class Error(val userMessage: String) : AlarmExecutionResult()
}

class AlarmActionHandler(private val context: Context) {

    /**
     * Parses alarm time from natural parameters with automatic tomorrow rollover
     * if the requested time has already passed today.
     */
    fun parseAlarmTime(
        rawHour: Any?,
        rawMinute: Any?,
        rawAmPm: Any?,
        title: String? = null,
        userPrompt: String = ""
    ): AlarmParseResult {
        var hour = when (rawHour) {
            is Number -> rawHour.toInt()
            is String -> rawHour.toIntOrNull() ?: 7
            else -> 7
        }

        val minute = when (rawMinute) {
            is Number -> rawMinute.toInt()
            is String -> rawMinute.toIntOrNull() ?: 0
            else -> 0
        }

        val amPm = rawAmPm?.toString()?.uppercase(Locale.ROOT) ?: ""
        if (amPm == "PM" && hour < 12) {
            hour += 12
        } else if (amPm == "AM" && hour == 12) {
            hour = 0
        }

        // Check if user specified tomorrow explicitly or if time has already passed today
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)

        val isPastToday = (hour < currentHour) || (hour == currentHour && minute <= currentMinute)
        val mentionsTomorrow = userPrompt.lowercase().contains("tomorrow")

        val isTomorrow = mentionsTomorrow || isPastToday
        val msg = title?.takeIf { it.isNotBlank() } ?: "Salim Alarm"

        return AlarmParseResult(
            hour = hour,
            minute = minute,
            message = msg,
            isTomorrowRollover = isTomorrow
        )
    }

    /**
     * Executes alarm action via AlarmClock.ACTION_SET_ALARM.
     */
    fun executeSetAlarm(
        parseResult: AlarmParseResult,
        autonomous: Boolean
    ): AlarmExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, parseResult.hour)
                putExtra(AlarmClock.EXTRA_MINUTES, parseResult.minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, parseResult.message)
                if (autonomous) {
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val packageManager = context.packageManager
            if (intent.resolveActivity(packageManager) != null) {
                context.startActivity(intent)
                val timeStr = String.format(Locale.getDefault(), "%02d:%02d", parseResult.hour, parseResult.minute)
                val dayQualifier = if (parseResult.isTomorrowRollover) " for tomorrow" else ""
                AlarmExecutionResult.Success(
                    userMessage = "Alarm set for $timeStr$dayQualifier.",
                    hour = parseResult.hour,
                    minute = parseResult.minute,
                    isRollover = parseResult.isTomorrowRollover
                )
            } else {
                AlarmExecutionResult.FallbackLaunched(
                    userMessage = "No clock application found to set alarm automatically. Please open your clock app."
                )
            }
        } catch (e: SecurityException) {
            AlarmExecutionResult.NeedsExactAlarmPermission(
                userMessage = "Clock permission was blocked by Android. Please grant alarm permission."
            )
        } catch (e: Exception) {
            AlarmExecutionResult.Error(
                userMessage = "The Clock app rejected this alarm. Open Clock and set it manually."
            )
        }
    }
}
