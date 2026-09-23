package com.example.data.permissions

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

data class PermissionStatus(
    val name: String,
    val permission: String,
    val isGranted: Boolean,
    val rationale: String,
    val isDangerous: Boolean = true
)

data class RoleStatus(
    val roleName: String,
    val roleConstant: String,
    val isAvailable: Boolean,
    val isHeld: Boolean,
    val rationale: String
)

class PermissionManager(private val context: Context) {

    val requiredPermissions = listOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR,
        Manifest.permission.SEND_SMS,
        Manifest.permission.CALL_PHONE
    )

    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }

    fun getRoleStatus(roleConstant: String, friendlyName: String, rationale: String): RoleStatus {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(roleConstant)) {
                return RoleStatus(
                    roleName = friendlyName,
                    roleConstant = roleConstant,
                    isAvailable = true,
                    isHeld = roleManager.isRoleHeld(roleConstant),
                    rationale = rationale
                )
            }
        }
        return RoleStatus(
            roleName = friendlyName,
            roleConstant = roleConstant,
            isAvailable = false,
            isHeld = false,
            rationale = rationale
        )
    }

    fun getAssistantRoleStatus(): RoleStatus = getRoleStatus(
        RoleManager.ROLE_ASSISTANT,
        "Default Assistant",
        "Enables voice invocation and system-wide assist session handling."
    )

    fun getSmsRoleStatus(): RoleStatus = getRoleStatus(
        RoleManager.ROLE_SMS,
        "Default SMS App",
        "Enables full autonomous direct SMS dispatch without system prompt."
    )

    fun getDialerRoleStatus(): RoleStatus = getRoleStatus(
        RoleManager.ROLE_DIALER,
        "Default Dialer",
        "Enables direct autonomous calling without external dialer approval."
    )

    fun createRequestRoleIntent(roleConstant: String): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(roleConstant)) {
                return roleManager.createRequestRoleIntent(roleConstant)
            }
        }
        return null
    }

    fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }

    fun openExactAlarmSettings(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", activity.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        }
    }

    fun openBatterySettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${activity.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            activity.startActivity(fallback)
        }
    }

    fun openDefaultAppsSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            openAppSettings(activity)
        }
    }

    fun isMicrophoneGranted(): Boolean = isPermissionGranted(Manifest.permission.RECORD_AUDIO)
    fun isContactsGranted(): Boolean = isPermissionGranted(Manifest.permission.READ_CONTACTS)
    fun isCalendarGranted(): Boolean = isPermissionGranted(Manifest.permission.READ_CALENDAR) && isPermissionGranted(Manifest.permission.WRITE_CALENDAR)
    fun isDefaultAssistantApp(): Boolean = getAssistantRoleStatus().isHeld

    fun openVoiceAssistantSettings(context: Context) {
        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallback)
            } catch (e2: Exception) {
                if (context is Activity) openAppSettings(context)
            }
        }
    }

    fun requestIgnoreBatteryOptimization(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    fun requestRole(activity: Activity, roleName: String, launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
        val roleConstant = when {
            roleName.contains("Assistant", ignoreCase = true) -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) RoleManager.ROLE_ASSISTANT else ""
            roleName.contains("SMS", ignoreCase = true) -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) RoleManager.ROLE_SMS else ""
            else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) RoleManager.ROLE_DIALER else ""
        }
        if (roleConstant.isNotEmpty()) {
            val intent = createRequestRoleIntent(roleConstant)
            if (intent != null) {
                launcher.launch(intent)
                return
            }
        }
        openDefaultAppsSettings(activity)
    }
}
