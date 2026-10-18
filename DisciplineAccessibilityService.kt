package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.MainActivity
import com.example.focus.FocusSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DisciplineAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)

    private val adultKeywords = listOf(
        "xxx", "porn", "xvideos", "xnxx", "adult", "erome", "nsfw",
        "redtube", "pornhub", "chaturbate", "onlyfans", "sex", "hentai",
        "cam4", "stripchat", "nude", "hardcore", "brazzers", "spankbang", "beeg"
    )

    private val browserPackages = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.sec.android.app.sbrowser",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.brave.browser",
        "com.duckduckgo.mobile.android"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceConnected.value = true
        Log.d("DisciplineAccessibility", "Discipline Accessibility Service Connected!")

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        info.notificationTimeout = 100
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Only enforce when Focus Mode is actively running
        if (!FocusSessionManager.isFocusActive.value) return

        val pkgName = event.packageName?.toString() ?: return

        // Never block our own app
        if (pkgName == packageName) return

        // Check if explicitly whitelisted (WhatsApp, Gmail, Phone calls, etc.)
        if (FocusSessionManager.isWhitelisted(pkgName)) {
            return
        }

        // Check if package is in blocked list
        var shouldBlock = false
        var blockReason = ""
        var appLabel = pkgName

        if (FocusSessionManager.isBlockedPackage(pkgName)) {
            // Special check for YouTube: If it's YouTube, check if Reels/Shorts is open
            if (pkgName == "com.google.android.youtube") {
                val isReelsOrShorts = isYouTubeReelsActive(rootInActiveWindow)
                if (isReelsOrShorts || FocusSessionManager.blockFullYouTube.value) {
                    shouldBlock = true
                    blockReason = "YouTube Shorts / Reels"
                    appLabel = "YouTube"
                }
            } else {
                shouldBlock = true
                blockReason = "Distracting Social App"
                appLabel = FocusSessionManager.getAppDisplayName(pkgName)
            }
        }

        // Check for adult content in browsers if adult protection is enabled
        if (!shouldBlock && FocusSessionManager.isAdultBlockerEnabled.value && browserPackages.contains(pkgName)) {
            val adultDetected = detectAdultContentInWindow(rootInActiveWindow)
            if (adultDetected) {
                shouldBlock = true
                blockReason = "Adult / Inappropriate Site"
                appLabel = "Browser"
            }
        }

        if (shouldBlock) {
            triggerBlockAction(appLabel, blockReason)
        }
    }

    private fun detectAdultContentInWindow(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return false
        try {
            val nodes = mutableListOf<AccessibilityNodeInfo>()
            nodes.add(root)

            var scanned = 0
            while (nodes.isNotEmpty() && scanned < 60) {
                val current = nodes.removeAt(0)
                scanned++

                val text = current.text?.toString()?.lowercase() ?: ""
                val desc = current.contentDescription?.toString()?.lowercase() ?: ""
                val viewId = current.viewIdResourceName?.lowercase() ?: ""

                // Address bar detection or body text matching
                for (keyword in adultKeywords) {
                    if (text.contains(keyword) || desc.contains(keyword)) {
                        return true
                    }
                }

                for (i in 0 until current.childCount) {
                    val child = current.getChild(i)
                    if (child != null) {
                        nodes.add(child)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DisciplineAccessibility", "Error scanning window: ${e.message}")
        }
        return false
    }

    private fun isYouTubeReelsActive(root: AccessibilityNodeInfo?): Boolean {
        if (root == null) return true // default to strict block in Focus mode
        try {
            val nodes = mutableListOf<AccessibilityNodeInfo>()
            nodes.add(root)
            var count = 0
            while (nodes.isNotEmpty() && count < 50) {
                val current = nodes.removeAt(0)
                count++
                val text = current.text?.toString()?.lowercase() ?: ""
                val desc = current.contentDescription?.toString()?.lowercase() ?: ""

                if (text.contains("shorts") || text.contains("reels") || desc.contains("shorts") || desc.contains("reels")) {
                    return true
                }

                for (i in 0 until current.childCount) {
                    val child = current.getChild(i)
                    if (child != null) nodes.add(child)
                }
            }
        } catch (_: Exception) {}
        return false
    }

    private fun triggerBlockAction(appName: String, reason: String) {
        // Kick user out of distracting app to Home Screen immediately
        performGlobalAction(GLOBAL_ACTION_HOME)

        serviceScope.launch {
            FocusSessionManager.registerDistractionBlocked(appName, reason)

            // Bring user back to Discipline Focus Assistant
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("EXTRA_DISTRACTION_BLOCKED", true)
                putExtra("EXTRA_BLOCKED_APP", appName)
                putExtra("EXTRA_BLOCK_REASON", reason)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {
        Log.w("DisciplineAccessibility", "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceConnected.value = false
    }

    companion object {
        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected = _isServiceConnected.asStateFlow()
    }
}
