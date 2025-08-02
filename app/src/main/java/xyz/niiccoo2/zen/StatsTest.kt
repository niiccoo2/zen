package xyz.niiccoo2.zen

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Returns the stats for the [date] (defaults to today) by processing raw UsageEvents.
 */
fun getDailyStats(context: Context, date: LocalDate = LocalDate.now()): List<Stat> {
    val usageManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        ?: return emptyList<Stat>() // Return empty if service is not available

    val utc = ZoneId.of("UTC")
    val defaultZone = ZoneId.systemDefault()

    // Define the query window in UTC based on the local date's start and end of day
    val dayStartInLocalZone = date.atStartOfDay(defaultZone)
    val start = dayStartInLocalZone.toInstant().toEpochMilli()
    val end = dayStartInLocalZone.plusDays(1).toInstant().toEpochMilli()

    val sortedEventsByPackage = mutableMapOf<String, MutableList<UsageEvents.Event>>()

    try {
        val systemEvents = usageManager.queryEvents(start, end)
        while (systemEvents.hasNextEvent()) {
            val event = UsageEvents.Event()
            systemEvents.getNextEvent(event)

            // Filter out events that might be outside the exact millisecond range if needed,
            // though queryEvents should already handle this.
            // if (event.timeStamp < start || event.timeStamp >= end) continue

            val packageEvents = sortedEventsByPackage.getOrPut(event.packageName) { mutableListOf() }
            packageEvents.add(event)
        }
    } catch (_: SecurityException) {
        // Handle cases where permission might be revoked or not granted
        // Log.e("getDailyStats", "Permission denied to query usage events", e)
        return emptyList()
    }


    val statsList = mutableListOf<Stat>()

    sortedEventsByPackage.forEach { (packageName, events) ->
        // It's good practice to sort events by timestamp to ensure correct order
        events.sortBy { it.timeStamp }

        var currentSessionStartTime = 0L
        var totalTimeInForeground = 0L
        val sessionStartTimesList = mutableListOf<ZonedDateTime>()

        for (event in events) {
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // If a foreground session was already tracked and not ended by a background event,
                    // and this new foreground event starts *after* it,
                    // it implies the previous session might have been cut short by the end of our query window
                    // or some other interruption. For simplicity, we just restart.
                    // More complex logic could try to cap the previous session at event.timeStamp.
                    currentSessionStartTime = event.timeStamp
                    sessionStartTimesList.add(
                        Instant.ofEpochMilli(currentSessionStartTime)
                            .atZone(utc) // Events are in UTC
                            .withZoneSameInstant(defaultZone) // Convert to display/local zone
                    )
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (currentSessionStartTime > 0) { // Only if a session was started
                        val sessionEndTime = event.timeStamp
                        totalTimeInForeground += (sessionEndTime - currentSessionStartTime)
                        currentSessionStartTime = 0L // Reset for the next session
                    }
                }
                // You might also consider UsageEvents.Event.ACTIVITY_STOPPED or CONFIGURATION_CHANGE
                // ACTIVITY_STOPPED is similar to MOVE_TO_BACKGROUND
                // CONFIGURATION_CHANGE (like screen rotation) can sometimes terminate and restart an activity,
                // potentially generating a quick background/foreground pair.
            }
        }

        // After iterating all events for an app, if a session was started but not ended
        // (i.e., app was still in foreground at the 'end' of our query window),
        // cap its duration at the 'end' of the query window.
        if (currentSessionStartTime > 0 && currentSessionStartTime < end) {
            totalTimeInForeground += (end - currentSessionStartTime)
        }

        if (totalTimeInForeground > 0) {
            statsList.add(Stat(packageName, totalTimeInForeground, sessionStartTimesList))
        }
    }
    return statsList
}
// Helper class to keep track of all of the stats
data class Stat(val packageName: String, val totalTime: Long, val startTimes: List<ZonedDateTime>)
