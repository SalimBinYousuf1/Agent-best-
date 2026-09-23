package com.example.assistant.actions

import android.content.Context
import com.example.data.database.ActionHistoryEntity
import com.example.data.database.AssistantRepository
import com.example.data.groq.AssistantActionType
import com.example.data.groq.AssistantParsedDecision
import com.example.data.groq.DecisionKind
import com.example.data.preferences.EncryptedPreferencesManager

data class ActionResult(
    val replyText: String,
    val actionType: AssistantActionType,
    val status: String, // "AUTONOMOUS", "FALLBACK_LAUNCHED", "CLARIFICATION", "ERROR", "NONE"
    val details: String? = null
)

class ActionExecutor(
    private val context: Context,
    private val prefsManager: EncryptedPreferencesManager,
    private val repository: AssistantRepository
) {
    private val alarmHandler = AlarmActionHandler(context)
    private val calendarHandler = CalendarActionHandler(context)
    private val smsHandler = SmsActionHandler(context)
    private val callHandler = CallActionHandler(context)
    private val appLauncherHandler = AppLauncherHandler(context)

    suspend fun execute(
        decision: AssistantParsedDecision,
        userPrompt: String
    ): ActionResult {
        val isAutonomous = prefsManager.isAutonomousModeEnabled

        when (decision.kind) {
            DecisionKind.CONVERSATION -> {
                return ActionResult(
                    replyText = decision.assistantReply,
                    actionType = AssistantActionType.NONE,
                    status = "NONE"
                )
            }
            DecisionKind.CLARIFICATION -> {
                return ActionResult(
                    replyText = decision.assistantReply,
                    actionType = decision.action,
                    status = "CLARIFICATION"
                )
            }
            DecisionKind.UNSUPPORTED -> {
                return ActionResult(
                    replyText = decision.assistantReply,
                    actionType = AssistantActionType.NONE,
                    status = "UNSUPPORTED"
                )
            }
            DecisionKind.ACTION -> {
                return handleAction(decision, userPrompt, isAutonomous)
            }
        }
    }

    private suspend fun handleAction(
        decision: AssistantParsedDecision,
        userPrompt: String,
        isAutonomous: Boolean
    ): ActionResult {
        val params = decision.parameters

        return when (decision.action) {
            AssistantActionType.SET_ALARM -> {
                val parsed = alarmHandler.parseAlarmTime(
                    rawHour = params["hour"],
                    rawMinute = params["minute"],
                    rawAmPm = params["am_pm"],
                    title = params["title"] as? String,
                    userPrompt = userPrompt
                )
                val execResult = alarmHandler.executeSetAlarm(parsed, isAutonomous)
                val (msg, status) = when (execResult) {
                    is AlarmExecutionResult.Success -> execResult.userMessage to "AUTONOMOUS"
                    is AlarmExecutionResult.NeedsExactAlarmPermission -> execResult.userMessage to "PERMISSION_REQUIRED"
                    is AlarmExecutionResult.FallbackLaunched -> execResult.userMessage to "FALLBACK_LAUNCHED"
                    is AlarmExecutionResult.Error -> execResult.userMessage to "ERROR"
                }

                recordActionHistory("SET_ALARM", "Alarm ${parsed.hour}:${parsed.minute}", status, msg)
                ActionResult(replyText = msg, actionType = decision.action, status = status)
            }

            AssistantActionType.CREATE_CALENDAR_EVENT -> {
                val eventParams = calendarHandler.parseCalendarEvent(params)
                val execResult = calendarHandler.executeCreateEvent(eventParams, isAutonomous)
                val (msg, status) = when (execResult) {
                    is CalendarExecutionResult.InsertedDirectly -> execResult.userMessage to "AUTONOMOUS"
                    is CalendarExecutionResult.FallbackIntentLaunched -> execResult.userMessage to "FALLBACK_LAUNCHED"
                    is CalendarExecutionResult.ConflictDetected -> execResult.userMessage to "CONFLICT"
                    is CalendarExecutionResult.Error -> execResult.userMessage to "ERROR"
                }

                recordActionHistory("CREATE_CALENDAR_EVENT", eventParams.title, status, msg)
                ActionResult(replyText = msg, actionType = decision.action, status = status)
            }

            AssistantActionType.SEND_SMS -> {
                val recipientName = params["recipient_name"] as? String
                val rawNumber = params["phone_number"] as? String
                val messageBody = (params["message_body"] as? String)?.takeIf { it.isNotBlank() }
                    ?: (params["query"] as? String)?.takeIf { it.isNotBlank() }
                    ?: ""

                val execResult = smsHandler.executeSendSms(
                    recipientName = recipientName,
                    rawPhoneNumber = rawNumber,
                    messageBody = messageBody,
                    directSmsEnabled = prefsManager.isDirectSmsEnabled,
                    isAutonomous = isAutonomous
                )

                val (msg, status) = when (execResult) {
                    is SmsExecutionResult.SentDirectly -> execResult.userMessage to "AUTONOMOUS"
                    is SmsExecutionResult.FallbackComposerOpened -> execResult.userMessage to "FALLBACK_LAUNCHED"
                    is SmsExecutionResult.AmbiguousContact -> execResult.userMessage to "CLARIFICATION"
                    is SmsExecutionResult.ContactNotFound -> execResult.userMessage to "ERROR"
                    is SmsExecutionResult.Error -> execResult.userMessage to "ERROR"
                }

                recordActionHistory("SEND_SMS", recipientName ?: rawNumber ?: "SMS", status, msg)
                ActionResult(replyText = msg, actionType = decision.action, status = status)
            }

            AssistantActionType.MAKE_CALL -> {
                val recipientName = params["recipient_name"] as? String
                val rawNumber = params["phone_number"] as? String

                val execResult = callHandler.executeCall(
                    recipientName = recipientName,
                    rawPhoneNumber = rawNumber,
                    directCallEnabled = prefsManager.isDirectCallEnabled,
                    isAutonomous = isAutonomous
                )

                val (msg, status) = when (execResult) {
                    is CallExecutionResult.DirectCallPlaced -> execResult.userMessage to "AUTONOMOUS"
                    is CallExecutionResult.DialerOpened -> execResult.userMessage to "FALLBACK_LAUNCHED"
                    is CallExecutionResult.EmergencySafetyEnforced -> execResult.userMessage to "EMERGENCY_SAFETY"
                    is CallExecutionResult.AmbiguousContact -> execResult.userMessage to "CLARIFICATION"
                    is CallExecutionResult.ContactNotFound -> execResult.userMessage to "ERROR"
                    is CallExecutionResult.Error -> execResult.userMessage to "ERROR"
                }

                recordActionHistory("MAKE_CALL", recipientName ?: rawNumber ?: "Call", status, msg)
                ActionResult(replyText = msg, actionType = decision.action, status = status)
            }

            AssistantActionType.OPEN_APP -> {
                val appName = (params["app_name"] as? String)?.takeIf { it.isNotBlank() } ?: "YouTube"
                val execResult = appLauncherHandler.launchAppByName(appName)
                val (msg, status) = when (execResult) {
                    is AppLaunchResult.AppLaunched -> execResult.userMessage to "AUTONOMOUS"
                    is AppLaunchResult.AppNotFound -> execResult.userMessage to "ERROR"
                    is AppLaunchResult.Error -> execResult.userMessage to "ERROR"
                    else -> "App launch completed." to "AUTONOMOUS"
                }

                recordActionHistory("OPEN_APP", appName, status, msg)
                ActionResult(replyText = msg, actionType = decision.action, status = status)
            }

            AssistantActionType.OPEN_DEEP_LINK -> {
                val url = params["url"] as? String
                val query = params["query"] as? String

                val execResult = when {
                    !url.isNullOrBlank() -> appLauncherHandler.launchSafeUrl(url)
                    !query.isNullOrBlank() && query.lowercase().startsWith("directions to") -> {
                        appLauncherHandler.launchMapsSearch(query.removePrefix("directions to").trim())
                    }
                    !query.isNullOrBlank() -> appLauncherHandler.launchWebSearch(query)
                    else -> AppLaunchResult.Error("No valid destination specified.")
                }

                val (msg, status) = when (execResult) {
                    is AppLaunchResult.DeepLinkLaunched -> execResult.userMessage to "AUTONOMOUS"
                    is AppLaunchResult.WebSearchLaunched -> execResult.userMessage to "AUTONOMOUS"
                    is AppLaunchResult.Error -> execResult.userMessage to "ERROR"
                    else -> "Action executed." to "AUTONOMOUS"
                }

                recordActionHistory("OPEN_DEEP_LINK", url ?: query ?: "link", status, msg)
                ActionResult(replyText = msg, actionType = decision.action, status = status)
            }

            AssistantActionType.NONE -> {
                ActionResult(
                    replyText = decision.assistantReply,
                    actionType = AssistantActionType.NONE,
                    status = "NONE"
                )
            }
        }
    }

    private suspend fun recordActionHistory(
        actionType: String,
        target: String,
        status: String,
        message: String
    ) {
        repository.recordAction(
            ActionHistoryEntity(
                actionType = actionType,
                target = target,
                parametersJson = "{}",
                status = status,
                timestamp = System.currentTimeMillis(),
                executionMessage = message
            )
        )
    }
}
