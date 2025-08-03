package xyz.niiccoo2.zen.utils

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

fun getAppUsage(context: Context, start: Long, end: Long): Map<String, Long> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val usageEvents = usageStatsManager.queryEvents(start - (3 * 60 * 60 * 1000), end) // 3-hour buffer

    val usageMap = mutableMapOf<String, Long>()
    val lastResumedEvents = mutableMapOf<String, UsageEvents.Event>()

    //val event = UsageEvents.Event()
    while (usageEvents.hasNextEvent()) {
        val event = UsageEvents.Event()  // new object each iteration
        usageEvents.getNextEvent(event)
        val key = event.packageName + event.className

        when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> lastResumedEvents[key] = event
            UsageEvents.Event.ACTIVITY_PAUSED,
            UsageEvents.Event.ACTIVITY_STOPPED -> {
                val resumedEvent = lastResumedEvents.remove(key)
                if (resumedEvent != null && event.timeStamp > start) {
                    val resumeTime = maxOf(resumedEvent.timeStamp, start)
                    val duration = event.timeStamp - resumeTime
                    usageMap[event.packageName] = usageMap.getOrDefault(event.packageName, 0L) + duration
                }
            }
        }
    }

    // Add ongoing sessions from last resumed events
    lastResumedEvents.values
        .groupBy { it.packageName }
        .forEach { (packageName, events) ->
            val mostRecent = events.maxByOrNull { it.timeStamp }!!
            usageMap[packageName] = usageMap.getOrDefault(packageName, 0L) + (end - mostRecent.timeStamp)
        }

    // Return usage time in seconds
    return usageMap.filterValues { it > 0 }
        .mapValues { it.value }
}

fun getTodaysAppUsage(context: Context): Map<String, Long> {
    val start = getStartOfTodayMillis()
    val end = System.currentTimeMillis()
    return getAppUsage(context, start, end)
}

fun getSingleAppUsage(context: Context, packageName: String): Long {
    val appUsage = getTodaysAppUsage(context)
    return appUsage[packageName] ?: 0
}