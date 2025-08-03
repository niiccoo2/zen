package xyz.niiccoo2.zen.utils

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Returns the time at midnight in the users local timezone.
 *
 * @return The time at midnight in the users local timezone.
 */
fun getStartOfTodayMillis(): Long {
    val todayLocalDate: LocalDate = LocalDate.now(ZoneId.systemDefault())

    val startOfTodayZonedDateTime: ZonedDateTime = todayLocalDate.atStartOfDay(ZoneId.systemDefault())

    return startOfTodayZonedDateTime.toInstant().toEpochMilli()
}

/**
 * Returns a pair of hours and minutes from a given number of milliseconds.
 *
 * @param millis The number of milliseconds.
 * @return A pair of hours and minutes.
 */
fun millisToHourAndMinute(millis: Long): Pair<Int, Int> {
    if (millis <= 0) {
        return Pair(0, 0)
    }
    val totalMinutes = (millis / 60000.0)
    val hours = totalMinutes.toInt() / 60
    val minutes = totalMinutes.toInt() % 60
    return Pair(hours, minutes)
}