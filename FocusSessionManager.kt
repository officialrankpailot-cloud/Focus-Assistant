package com.example.focus

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.DisciplineApp
import com.example.data.model.FocusSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BlockedEvent(
    val appName: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

object FocusSessionManager {

    private val managerScope = CoroutineScope(Dispatchers.Main)
    private var timerJob: Job? = null

    private val _isFocusActive = MutableStateFlow(false)
    val isFocusActive = _isFocusActive.asStateFlow()

    private val _timeRemainingSeconds = MutableStateFlow(0)
    val timeRemainingSeconds = _timeRemainingSeconds.asStateFlow()

    private val _totalDurationMinutes = MutableStateFlow(25)
    val totalDurationMinutes = _totalDurationMinutes.asStateFlow()

    private val _deskPhotoUri = MutableStateFlow<String?>(null)
    val deskPhotoUri = _deskPhotoUri.asStateFlow()

    private val _currentSessionDistractions = MutableStateFlow(0)
    val currentSessionDistractions = _currentSessionDistractions.asStateFlow()

    private val _lastBlockedEvent = MutableStateFlow<BlockedEvent?>(null)
    val lastBlockedEvent = _lastBlockedEvent.asStateFlow()

    private val _blockAlertEvent = MutableSharedFlow<BlockedEvent>(extraBufferCapacity = 1)
    val blockAlertEvent = _blockAlertEvent.asSharedFlow()

    val isAdultBlockerEnabled = MutableStateFlow(true)
    val blockFullYouTube = MutableStateFlow(false)

    // Cached packages for fast accessibility lookups
    private val blockedPackages = mutableSetOf(
        "com.facebook.katana",
        "com.facebook.lite",
        "com.instagram.android",
        "com.google.android.youtube",
        "com.zhiliaoapp.musically",
        "com.twitter.android",
        "com.reddit.frontpage",
        "com.snapchat.android"
    )

    private val whitelistedPackages = mutableSetOf(
        "com.whatsapp",
        "com.google.android.gm",
        "com.google.android.dialer",
        "com.android.phone",
        "com.android.dialer",
        "com.samsung.android.dialer",
        "com.google.android.apps.messaging",
        "com.google.android.apps.docs"
    )

    private val appDisplayNames = mutableMapOf(
        "com.facebook.katana" to "Facebook",
        "com.facebook.lite" to "Facebook Lite",
        "com.instagram.android" to "Instagram",
        "com.google.android.youtube" to "YouTube Reels",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.twitter.android" to "X (Twitter)",
        "com.reddit.frontpage" to "Reddit",
        "com.snapchat.android" to "Snapchat",
        "com.whatsapp" to "WhatsApp",
        "com.google.android.gm" to "Gmail"
    )

    fun isBlockedPackage(pkg: String): Boolean = blockedPackages.contains(pkg)

    fun isWhitelisted(pkg: String): Boolean = whitelistedPackages.contains(pkg)

    fun getAppDisplayName(pkg: String): String = appDisplayNames[pkg] ?: pkg

    fun updateBlockedPackages(packages: Set<String>) {
        blockedPackages.clear()
        blockedPackages.addAll(packages)
    }

    fun updateWhitelistedPackages(packages: Set<String>) {
        whitelistedPackages.clear()
        whitelistedPackages.addAll(packages)
    }

    fun setDeskPhotoUri(uriString: String?) {
        _deskPhotoUri.value = uriString
    }

    fun saveDeskPhotoBitmap(context: Context, bitmap: Bitmap): String {
        val filename = "desk_photo_${System.currentTimeMillis()}.jpg"
        val dir = File(context.filesDir, "desk_photos")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        val path = file.absolutePath
        _deskPhotoUri.value = path
        return path
    }

    fun startFocusSession(durationMinutes: Int) {
        _totalDurationMinutes.value = durationMinutes
        _timeRemainingSeconds.value = durationMinutes * 60
        _currentSessionDistractions.value = 0
        _isFocusActive.value = true

        if (FocusAmbientAudioPlayer.autoPlayOnFocusStart.value) {
            FocusAmbientAudioPlayer.play(FocusAmbientAudioPlayer.currentSoundscape.value)
        }

        timerJob?.cancel()
        timerJob = managerScope.launch {
            while (_timeRemainingSeconds.value > 0 && _isFocusActive.value) {
                delay(1000L)
                _timeRemainingSeconds.value -= 1
            }
            if (_isFocusActive.value && _timeRemainingSeconds.value <= 0) {
                completeFocusSession(completedSuccessfully = true)
            }
        }
    }

    fun cancelFocusSession() {
        timerJob?.cancel()
        completeFocusSession(completedSuccessfully = false)
    }

    private val _badgeUnlockEvent = MutableSharedFlow<com.example.data.model.Badge>(extraBufferCapacity = 1)
    val badgeUnlockEvent = _badgeUnlockEvent.asSharedFlow()

    private val _goalCompletedEvent = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val goalCompletedEvent = _goalCompletedEvent.asSharedFlow()

    private fun completeFocusSession(completedSuccessfully: Boolean) {
        val totalSecs = _totalDurationMinutes.value * 60
        val elapsedMinutes = ((totalSecs - _timeRemainingSeconds.value) / 60).coerceAtLeast(1)
        val sessionDuration = if (completedSuccessfully) _totalDurationMinutes.value else elapsedMinutes

        val session = FocusSessionEntity(
            startTime = System.currentTimeMillis() - (sessionDuration * 60 * 1000L),
            endTime = System.currentTimeMillis(),
            durationMinutes = sessionDuration,
            deskPhotoUri = _deskPhotoUri.value,
            distractionsBlocked = _currentSessionDistractions.value,
            completedSuccessfully = completedSuccessfully,
            dateFormatted = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date())
        )

        _isFocusActive.value = false
        _timeRemainingSeconds.value = 0
        FocusAmbientAudioPlayer.stop()

        // Persist session to Room and update study goal & streak
        managerScope.launch(Dispatchers.IO) {
            val db = DisciplineApp.instance.database
            db.focusDao().insertSession(session)

            val currentStats = db.disciplineStatsDao().getStatsSync()
            if (currentStats != null) {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val isNewDay = currentStats.lastGoalDate != todayStr
                val baseTodayMinutes = if (isNewDay) 0 else currentStats.todayFocusMinutes
                val newTodayMinutes = baseTodayMinutes + sessionDuration

                var newStreakDays = currentStats.streakDays
                var newBestStreak = currentStats.bestStreakDays
                var goalJustCompleted = false
                var newTodayGoalCompleted = currentStats.todayGoalCompleted && !isNewDay

                val unlockedSet = currentStats.getUnlockedList().toMutableSet()
                val newlyEarnedBadges = mutableListOf<com.example.data.model.Badge>()

                if (newTodayMinutes >= currentStats.dailyGoalMinutes && !newTodayGoalCompleted) {
                    newTodayGoalCompleted = true
                    goalJustCompleted = true
                    newStreakDays += 1
                    if (newStreakDays > newBestStreak) {
                        newBestStreak = newStreakDays
                    }

                    // Check for newly unlocked badges
                    com.example.data.model.BadgeCatalog.ALL_BADGES.forEach { badge ->
                        if (newStreakDays >= badge.requiredStreakDays && !unlockedSet.contains(badge.id)) {
                            unlockedSet.add(badge.id)
                            newlyEarnedBadges.add(badge)
                        }
                    }
                }

                val updatedStats = currentStats.copy(
                    todayFocusMinutes = newTodayMinutes,
                    todayGoalCompleted = newTodayGoalCompleted,
                    lastGoalDate = if (newTodayGoalCompleted) todayStr else currentStats.lastGoalDate,
                    streakDays = newStreakDays,
                    bestStreakDays = newBestStreak,
                    totalFocusMinutes = currentStats.totalFocusMinutes + sessionDuration,
                    totalSessions = currentStats.totalSessions + 1,
                    totalDistractionsBlocked = currentStats.totalDistractionsBlocked + session.distractionsBlocked,
                    unlockedBadgeIds = unlockedSet.joinToString(",")
                )
                db.disciplineStatsDao().saveStats(updatedStats)

                if (goalJustCompleted) {
                    _goalCompletedEvent.tryEmit(newStreakDays)
                }
                newlyEarnedBadges.forEach { badge ->
                    _badgeUnlockEvent.tryEmit(badge)
                }
            }
        }
    }

    fun registerDistractionBlocked(appName: String, reason: String) {
        _currentSessionDistractions.value += 1
        val event = BlockedEvent(appName, reason)
        _lastBlockedEvent.value = event
        _blockAlertEvent.tryEmit(event)

        managerScope.launch(Dispatchers.IO) {
            DisciplineApp.instance.database.disciplineStatsDao().incrementDistractionsBlocked()
        }
    }
}
