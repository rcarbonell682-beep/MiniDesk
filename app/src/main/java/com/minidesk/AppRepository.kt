package com.minidesk

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import java.util.concurrent.Executors

data class AppItem(
    val label: String,
    val packageName: String,
    val activityName: String,
    val normalizedLabel: String
) : Comparable<AppItem> {
    override fun compareTo(other: AppItem): Int {
        return this.label.compareTo(other.label, ignoreCase = true)
    }
}

object AppRepository {
    private val executor = Executors.newSingleThreadExecutor()
    private val cachedApps = mutableListOf<AppItem>()
    var isLoaded = false
        private set

    fun loadApps(packageManager: PackageManager, onLoaded: (List<AppItem>) -> Unit) {
        executor.execute {
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)
            val list = mutableListOf<AppItem>()
            
            for (resolveInfo in resolveInfos) {
                val label = resolveInfo.loadLabel(packageManager).toString()
                val pkg = resolveInfo.activityInfo.packageName
                val cls = resolveInfo.activityInfo.name
                list.add(AppItem(label, pkg, cls, label.lowercase()))
            }
            list.sort()
            synchronized(cachedApps) {
                cachedApps.clear()
                cachedApps.addAll(list)
                isLoaded = true
            }
            onLoaded(cachedApps)
        }
    }

    fun search(query: String): List<AppItem> {
        val trimmed = query.lowercase().trim()
        synchronized(cachedApps) {
            if (trimmed.isEmpty()) {
                return cachedApps.toList()
            }
            return cachedApps.filter { it.normalizedLabel.contains(trimmed) }
        }
    }
}
