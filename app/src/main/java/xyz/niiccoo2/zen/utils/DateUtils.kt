package xyz.niiccoo2.zen.utils

import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
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

/**
 * Returns a string representation of a given number of milliseconds in hours, minutes, and seconds.
 * shortFormat: 1h 2m 3s
 * longFormat: 1 hour 2 minutes 3 seconds
 *
 * @param millis The number of milliseconds.
 * @param longFormat If true, the output will be in a longer format.
 * @return A string representation of the given number of milliseconds.
 */
fun millisToNormalTime(millis: Long, longFormat : Boolean = false): String {
    val (hours, minutes) = millisToHourAndMinute(millis)
    if (longFormat) {
        if (hours == 0 && minutes >= 1) {
            return "$minutes minutes"
        } else if (hours != 0) {
            return "$hours hours $minutes minutes"
        } else {
            val textSecs = millis / 1000
            return "$textSecs seconds"
        }
    } else {
        if (hours == 0 && minutes >= 1) {
            return "$minutes min"
        } else if (hours != 0) {
            return "$hours h $minutes min"
        } else {
            val textSecs = millis / 1000
            return "$textSecs secs"
        }
    }

}