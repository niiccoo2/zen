package xyz.niiccoo2.zen.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class PlaceholderAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.d("PlaceholderService", "Event: $event")
    }

    override fun onInterrupt() {
        Log.d("PlaceholderService", "Interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("PlaceholderService", "Service Connected")
    }
}
