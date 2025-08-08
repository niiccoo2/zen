package xyz.niiccoo2.zen.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient // For computed properties not to be serialized
import java.time.LocalTime

@Serializable
data class TimeBlock(
    val startTime: @Serializable(with = LocalTimeSerializer::class) LocalTime,
    val endTime: @Serializable(with = LocalTimeSerializer::class) LocalTime
) {
    companion object {
        val ALWAYS_BLOCKED_SCHEDULE: List<TimeBlock> = listOf(
            TimeBlock(startTime = LocalTime.MIN, endTime = LocalTime.MAX)
        )

        val NO_SCHEDULE: List<TimeBlock> = emptyList()
    }
}

@Serializable
data class BlockedAppSettings(
    val packageName: String?,
    var scheduledBlocks: List<TimeBlock> = TimeBlock.NO_SCHEDULE,
    var isOnBreak: Boolean = false
) {

    val isEffectivelyAlwaysBlocked: Boolean
        get() = scheduledBlocks == TimeBlock.ALWAYS_BLOCKED_SCHEDULE

    fun setAlwaysBlocked(always: Boolean) {
        scheduledBlocks = if (always) {
            TimeBlock.ALWAYS_BLOCKED_SCHEDULE
        } else {
            TimeBlock.NO_SCHEDULE
        }
    }
}