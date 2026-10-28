package com.example.floatinginterviewcopilot

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Optional integration placeholder. This service intentionally does not auto-click Send or
 * inspect response content. Any future UI automation must be explicitly enabled by the user,
 * transparently disclosed, and tested against current app policies and UI behavior.
 */
class CopilotAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No automatic actions in the starter prototype.
    }
    override fun onInterrupt() {}
}
