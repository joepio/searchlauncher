package com.searchlauncher.app.data

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.localstorage.LocalStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class SearchRepository(private val context: Context) {

    private var appSearchSession: AppSearchSession? = null
    private val executor = Executors.newSingleThreadExecutor()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            val sessionFuture = LocalStorage.createSearchSessionAsync(
                LocalStorage.SearchContext.Builder(context, "searchlauncher_db")
                    .build()
            )
            appSearchSession = sessionFuture.get()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun searchApps(query: String): List<SearchResult.App> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = packageManager.queryIntentActivities(intent, 0)
            .filter { resolveInfo ->
                val appInfo = resolveInfo.activityInfo.applicationInfo
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            }
            .mapNotNull { resolveInfo ->
                try {
                    val appName = resolveInfo.loadLabel(packageManager).toString()
                    val packageName = resolveInfo.activityInfo.packageName

                    if (query.isEmpty() || appName.contains(query, ignoreCase = true)) {
                        SearchResult.App(
                            id = packageName,
                            title = appName,
                            subtitle = packageName,
                            icon = resolveInfo.loadIcon(packageManager),
                            packageName = packageName
                        )
                    } else null
                } catch (e: Exception) {
                    null
                }
            }

        // Sort by usage if permission is granted
        sortAppsByUsage(apps)
    }

    private fun sortAppsByUsage(apps: List<SearchResult.App>): List<SearchResult.App> {
        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usageStatsManager != null) {
                val currentTime = System.currentTimeMillis()
                val stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    currentTime - 1000 * 60 * 60 * 24 * 7, // Last 7 days
                    currentTime
                )

                val usageMap = stats.associateBy { it.packageName }
                apps.sortedByDescending {
                    usageMap[it.packageName]?.lastTimeUsed ?: 0
                }
            } else {
                apps
            }
        } catch (e: Exception) {
            apps
        }
    }

    suspend fun searchContent(query: String): List<SearchResult.Content> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult.Content>()

        try {
            val session = appSearchSession ?: return@withContext results

            val searchSpec = SearchSpec.Builder()
                .setSnippetCount(10)
                .setResultCountPerPage(20)
                .build()

            val searchResults = session.search(query, searchSpec)

            // Process search results
            // Note: This is a simplified implementation
            // Real apps would need to index content from various apps

        } catch (e: Exception) {
            e.printStackTrace()
        }

        results
    }

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val apps = searchApps(query)
        val content = searchContent(query)
        apps + content
    }

    fun close() {
        appSearchSession?.close()
        executor.shutdown()
    }
}
