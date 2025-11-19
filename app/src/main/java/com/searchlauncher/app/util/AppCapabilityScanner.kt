package com.searchlauncher.app.util

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import org.xmlpull.v1.XmlPullParser

data class AppCapability(
    val packageName: String,
    val activityName: String,
    val schemes: List<String>,
    val mimeTypes: List<String>,
    val actions: List<String>
)

object AppCapabilityScanner {
    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

    fun scan(context: Context): List<AppCapability> {
        val capabilities = mutableListOf<AppCapability>()
        val pm = context.packageManager
        // Get all installed packages
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (appInfo in packages) {
            // Skip system apps that aren't updated (optional optimization, maybe we want system handlers too)
            // For now, scanning everything.

            try {
                val pkgContext = context.createPackageContext(appInfo.packageName, 0)
                val assets = pkgContext.assets
                val parser = assets.openXmlResourceParser("AndroidManifest.xml")

                val appCaps = parseManifest(appInfo.packageName, parser)
                if (appCaps.isNotEmpty()) {
                    capabilities.addAll(appCaps)
                }
                parser.close()
            } catch (e: Exception) {
                // Ignore errors (e.g. restricted packages)
            }
        }
        return capabilities
    }

    private fun parseManifest(packageName: String, parser: XmlResourceParser): List<AppCapability> {
        val appCapabilities = mutableListOf<AppCapability>()
        var currentActivity: String? = null
        var currentSchemes = mutableListOf<String>()
        var currentMimeTypes = mutableListOf<String>()
        var currentActions = mutableListOf<String>()
        var isExported = false

        // We only care about activities with intent-filters that have data/actions

        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name
                        if (name == "activity" || name == "activity-alias") {
                            // New activity start
                            val rawName = parser.getAttributeValue(ANDROID_NS, "name")
                            currentActivity = if (rawName != null) {
                                if (rawName.startsWith(".")) {
                                    packageName + rawName
                                } else if (!rawName.contains(".")) {
                                    "$packageName.$rawName"
                                } else {
                                    rawName
                                }
                            } else null

                            // Check exported
                            val exportedVal = parser.getAttributeValue(ANDROID_NS, "exported")
                            isExported = exportedVal == "true"

                            // Reset filters
                            currentSchemes = mutableListOf()
                            currentMimeTypes = mutableListOf()
                            currentActions = mutableListOf()

                        } else if (name == "action") {
                             val action = parser.getAttributeValue(ANDROID_NS, "name")
                             if (action != null) currentActions.add(action)
                        } else if (name == "data") {
                            val scheme = parser.getAttributeValue(ANDROID_NS, "scheme")
                            val mimeType = parser.getAttributeValue(ANDROID_NS, "mimeType")

                            if (scheme != null) currentSchemes.add(scheme)
                            if (mimeType != null) currentMimeTypes.add(mimeType)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "activity" || parser.name == "activity-alias") {
                            // End of activity, save if interesting
                            if (isExported && currentActivity != null &&
                                (currentSchemes.isNotEmpty() || currentMimeTypes.isNotEmpty())) {

                                appCapabilities.add(
                                    AppCapability(
                                        packageName,
                                        currentActivity!!,
                                        currentSchemes.distinct(),
                                        currentMimeTypes.distinct(),
                                        currentActions.distinct()
                                    )
                                )
                            }
                            currentActivity = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // Parsing failed
        }

        return appCapabilities
    }
}
