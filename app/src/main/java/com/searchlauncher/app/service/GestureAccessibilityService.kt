package com.searchlauncher.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class GestureAccessibilityService : AccessibilityService() {

    private var backPressCount = 0
    private var lastBackPressTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // This service works in conjunction with the overlay service
        // to detect back gestures and trigger the search UI

        // Note: The actual gesture detection is primarily handled by the OverlayService
        // This accessibility service provides additional context about the current app state
    }

    override fun onInterrupt() {
        // Handle interrupt
    }

    companion object {
        private const val DOUBLE_BACK_TIME_DELTA = 500L // milliseconds
    }
}
