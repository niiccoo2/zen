package xyz.niiccoo2.zen.utils

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.wear.compose.materialcore.is24HourFormat
import java.text.DateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.text.format

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

/**
 * Formats LocalTime into a human-readable string, strictly respecting the
 * device's system setting for 12/24 hour format.
 *
 * @param time The LocalTime to format.
 * @param context The application context, needed to check the system's 12/24 hour setting.
 * @return A formatted time string according to the device's 12/24 hour preference.
 */
@OptIn(UnstableApi::class)
fun formatLocalTime(time: LocalTime, context: Context): String {
    return try {
        val formatter: DateTimeFormatter
        if (android.text.format.DateFormat.is24HourFormat(context)) {

            formatter = DateTimeFormatter.ofPattern("HH:mm")
        } else {
            formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        }
        time.format(formatter)
    } catch (e: Exception) {
        Log.w(
            "TimeFormatUtil",
            "Error formatting time $time. Falling back to default.",
            e
        )
        time.toString()
    }
}

fun getFormattedScheduledBlockTimes(appSettings: BlockedAppSettings, context: Context): String? {
    if (appSettings.isEffectivelyAlwaysBlocked) {
        return null
    }
    if (appSettings.scheduledBlocks.isEmpty() || appSettings.scheduledBlocks == TimeBlock.NO_SCHEDULE) {
        return null
    }

    val formattedTimes = appSettings.scheduledBlocks.joinToString(separator = ", ") { timeBlock ->
        val startTimeFormatted = formatLocalTime(timeBlock.startTime, context)
        val endTimeFormatted = formatLocalTime(timeBlock.endTime, context)
        "$startTimeFormatted - $endTimeFormatted"
    }
    return "Blocked: $formattedTimes"
}