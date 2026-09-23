package com.example.assistant.actions

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore

sealed class AppLaunchResult {
    data class AppLaunched(val userMessage: String, val appName: String, val packageName: String) : AppLaunchResult()
    data class WebSearchLaunched(val userMessage: String, val query: String) : AppLaunchResult()
    data class DeepLinkLaunched(val userMessage: String, val uri: String) : AppLaunchResult()
    data class AppNotFound(val userMessage: String) : AppLaunchResult()
    data class Error(val userMessage: String) : AppLaunchResult()
}

class AppLauncherHandler(private val context: Context) {

    /**
     * Resolves an installed application matching query against app labels or package names.
     */
    fun findInstalledApp(query: String): Pair<String, String>? {
        val pm = context.packageManager
        val cleanQuery = query.lowercase().trim()

        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in installedApps) {
            // Only consider launchable apps
            if (pm.getLaunchIntentForPackage(app.packageName) == null) continue

            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (label == cleanQuery || label.contains(cleanQuery) || app.packageName.lowercase().contains(cleanQuery)) {
                return Pair(pm.getApplicationLabel(app).toString(), app.packageName)
            }
        }
        return null
    }

    fun launchAppByName(appName: String): AppLaunchResult {
        // Special case: Camera
        if (appName.lowercase().contains("camera")) {
            return try {
                val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (cameraIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(cameraIntent)
                    return AppLaunchResult.AppLaunched("Opening Camera.", "Camera", "system.camera")
                }
                val fallbackIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (fallbackIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(fallbackIntent)
                    return AppLaunchResult.AppLaunched("Opening Camera.", "Camera", "system.camera")
                }
                AppLaunchResult.AppNotFound("The Camera application was not found.")
            } catch (e: Exception) {
                AppLaunchResult.Error("Could not launch Camera.")
            }
        }

        // Search installed apps
        val match = findInstalledApp(appName)
        if (match != null) {
            val (resolvedName, packageName) = match
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntent != null) {
                return try {
                    context.startActivity(launchIntent)
                    AppLaunchResult.AppLaunched("Opening $resolvedName.", resolvedName, packageName)
                } catch (e: Exception) {
                    AppLaunchResult.Error("Could not open $resolvedName.")
                }
            }
        }

        return AppLaunchResult.AppNotFound("The application '$appName' is not installed.")
    }

    fun launchWebSearch(query: String): AppLaunchResult {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                AppLaunchResult.WebSearchLaunched("Searching web for '$query'.", query)
            } else {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                AppLaunchResult.WebSearchLaunched("Searching web for '$query'.", query)
            }
        } catch (e: Exception) {
            AppLaunchResult.Error("Could not perform web search.")
        }
    }

    fun launchMapsSearch(query: String): AppLaunchResult {
        return try {
            val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
                AppLaunchResult.DeepLinkLaunched("Opening Maps for '$query'.", gmmIntentUri.toString())
            } else {
                val browserMaps = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/${Uri.encode(query)}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserMaps)
                AppLaunchResult.DeepLinkLaunched("Opening Maps for '$query'.", browserMaps.dataString ?: "")
            }
        } catch (e: Exception) {
            AppLaunchResult.Error("Could not launch navigation.")
        }
    }

    fun launchEmailCompose(recipient: String?, subject: String?, body: String?): AppLaunchResult {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${recipient ?: ""}")
                if (!subject.isNullOrBlank()) putExtra(Intent.EXTRA_SUBJECT, subject)
                if (!body.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            AppLaunchResult.DeepLinkLaunched("Opening email composer.", "mailto")
        } catch (e: Exception) {
            AppLaunchResult.Error("No email app available.")
        }
    }

    fun launchSafeUrl(url: String): AppLaunchResult {
        return try {
            val uri = Uri.parse(url)
            if (uri.scheme != "http" && uri.scheme != "https") {
                return AppLaunchResult.Error("Only secure web links (https) are supported.")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            AppLaunchResult.DeepLinkLaunched("Opening link in browser.", url)
        } catch (e: Exception) {
            AppLaunchResult.Error("Could not open link.")
        }
    }
}
