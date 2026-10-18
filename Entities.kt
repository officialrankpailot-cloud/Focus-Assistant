package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val deskPhotoUri: String?,
    val distractionsBlocked: Int = 0,
    val completedSuccessfully: Boolean = true,
    val dateFormatted: String
)

@Entity(tableName = "alarm_settings")
data class AlarmSetting(
    @PrimaryKey
    val id: Int = 1,
    val hour: Int = 6,
    val minute: Int = 30,
    val isEnabled: Boolean = true,
    val requiredPhrase: String = "Ami ghum theke uthechi",
    val secondaryPhrase: String = "আমি ঘুম থেকে উঠেছি",
    val vibrateContinuously: Boolean = true,
    val soundEnabled: Boolean = true,
    val repeatDaily: Boolean = true
)

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean,
    val isWhitelisted: Boolean,
    val category: String, // "Social Media", "Video Reels", "Essential", "Productivity"
    val isEssential: Boolean = false,
    val iconTintHex: Long = 0xFF6366F1
)

@Entity(tableName = "discipline_stats")
data class DisciplineStats(
    @PrimaryKey
    val id: Int = 1,
    val streakDays: Int = 4,
    val bestStreakDays: Int = 7,
    val dailyGoalMinutes: Int = 50,
    val todayFocusMinutes: Int = 45,
    val todayGoalCompleted: Boolean = false,
    val lastGoalDate: String = "",
    val totalFocusMinutes: Int = 185,
    val totalSessions: Int = 6,
    val totalDistractionsBlocked: Int = 23,
    val voiceAlarmsCleared: Int = 5,
    val unlockedBadgeIds: String = "badge_1,badge_3"
) {
    fun isBadgeUnlocked(badgeId: String): Boolean {
        return unlockedBadgeIds.split(",").map { it.trim() }.contains(badgeId)
    }

    fun getUnlockedList(): List<String> {
        return unlockedBadgeIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

@Entity(tableName = "study_schedules")
data class StudyScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startHour: Int,
    val startMinute: Int,
    val durationMinutes: Int = 45,
    val daysOfWeek: String = "Mon,Tue,Wed,Thu,Fri",
    val isEnabled: Boolean = true,
    val reminderMinutesBefore: Int = 10,
    val category: String = "Deep Work",
    val colorHex: Long = 0xFF06B6D4
) {
    fun formattedStartTime(): String {
        val period = if (startHour >= 12) "PM" else "AM"
        val h = when {
            startHour == 0 -> 12
            startHour > 12 -> startHour - 12
            else -> startHour
        }
        return String.format("%02d:%02d %s", h, startMinute, period)
    }

    fun formattedEndTime(): String {
        val totalStartMinutes = startHour * 60 + startMinute
        val endTotal = (totalStartMinutes + durationMinutes) % (24 * 60)
        val endH = endTotal / 60
        val endM = endTotal % 60
        val period = if (endH >= 12) "PM" else "AM"
        val h = when {
            endH == 0 -> 12
            endH > 12 -> endH - 12
            else -> endH
        }
        return String.format("%02d:%02d %s", h, endM, period)
    }
}
