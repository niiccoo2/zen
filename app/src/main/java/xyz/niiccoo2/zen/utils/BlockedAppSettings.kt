package xyz.niiccoo2.zen.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient // For computed properties not to be serialized
import java.time.LocalTime

// Assuming LocalTimeSerializer is in this package (xyz.niiccoo2.zen.utils)
// or correctly imported if it's in another package.

@Serializable
data class TimeBlock(
    val startTime: @Serializable(with = LocalTimeSerializer::class) LocalTime,
    val endTime: @Serializable(with = LocalTimeSerializer::class) LocalTime
) {
    companion object {
        // Define a canonical representation for an "always blocked" schedule
        val ALWAYS_BLOCKED_SCHEDULE: List<TimeBlock> = listOf(
            TimeBlock(startTime = LocalTime.MIN, endTime = LocalTime.MAX)
        )

        // Define what "no schedule" or "never blocked by schedule" looks like
        val NO_SCHEDULE: List<TimeBlock> = emptyList()
    }
}

@Serializable
data class BlockedAppSettings(
    val packageName: String?,
    // Replace isAlwaysBlocked with a list of TimeBlock objects
    var scheduledBlocks: List<TimeBlock> = TimeBlock.NO_SCHEDULE, // Default to no blocks
    var isOnBreak: Boolean = false
) {

    val isEffectivelyAlwaysBlocked: Boolean
        get() = scheduledBlocks == TimeBlock.ALWAYS_BLOCKED_SCHEDULE

    fun setAlwaysBlocked(always: Boolean) {
        scheduledBlocks = if (always) {
            TimeBlock.ALWAYS_BLOCKED_SCHEDULE
        } else {
            // When turning "always blocked" OFF, revert to no schedule.
            // If you had a more complex system where users could have custom schedules
            // AND an "always blocked" override, you'd need more logic here
            // to restore their previous custom schedule. For now, this is simple.
            TimeBlock.NO_SCHEDULE
        }
    }
}