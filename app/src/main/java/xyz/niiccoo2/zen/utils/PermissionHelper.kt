package xyz.niiccoo2.zen.utils

import android.app.AppOpsManager
import android.content.Context
import android.os.Process

// I tried to put all the permission functions here but it is a pain so I'll do it once I rlly
// need to

class PermissionHelper {
    /**
     * Checks if the app has the necessary permission to access usage statistics.
     *
     * @param context The application context.
     * @return True if the permission is granted, false otherwise.
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        // If the permission is allowed mode == 0
        val packageName = context.packageName
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(), // This gets the UID of your app's process
            packageName      // Use the dynamically obtained package name
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}