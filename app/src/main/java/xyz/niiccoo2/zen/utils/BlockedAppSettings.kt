package xyz.niiccoo2.zen.utils

import kotlinx.serialization.Serializable

@Serializable // Make it serializable for JSON
data class BlockedAppSettings(
    val packageName: String?,
    var isAlwaysBlocked: Boolean = false,
    var isOnBreak: Boolean = false
)