package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AlarmSetting
import com.example.data.model.BlockedAppEntity
import com.example.data.model.DisciplineStats
import com.example.data.model.FocusSessionEntity
import com.example.data.model.StudyScheduleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        FocusSessionEntity::class,
        AlarmSetting::class,
        BlockedAppEntity::class,
        DisciplineStats::class,
        StudyScheduleEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class DisciplineDatabase : RoomDatabase() {
    abstract fun focusDao(): FocusDao
    abstract fun alarmDao(): AlarmDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun disciplineStatsDao(): DisciplineStatsDao
    abstract fun studyScheduleDao(): StudyScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: DisciplineDatabase? = null

        fun getDatabase(context: Context): DisciplineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DisciplineDatabase::class.java,
                    "discipline_focus_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: DisciplineDatabase) {
            val defaultApps = listOf(
                // Blocked Social & Entertainment
                BlockedAppEntity(
                    packageName = "com.facebook.katana",
                    appName = "Facebook",
                    isBlocked = true,
                    isWhitelisted = false,
                    category = "Social Media",
                    iconTintHex = 0xFF1877F2
                ),
                BlockedAppEntity(
                    packageName = "com.facebook.lite",
                    appName = "Facebook Lite",
                    isBlocked = true,
                    isWhitelisted = false,
                    category = "Social Media",
                    iconTintHex = 0xFF1877F2
                ),
                BlockedAppEntity(
                    packageName = "com.instagram.android",
                    appName = "Instagram",
                    isBlocked = true,
                    isWhitelisted = false,
                    category = "Social Media",
                    iconTintHex = 0xFFE1306C
                ),
                BlockedAppEntity(
                    packageName = "com.google.android.youtube",
                    appName = "YouTube (Reels/Shorts)",
                    isBlocked = true,
                    isWhitelisted = false,
                    category = "Video Reels",
                    iconTintHex = 0xFFFF0000
                ),
                BlockedAppEntity(
                    packageName = "com.zhiliaoapp.musically",
                    appName = "TikTok",
                    isBlocked = true,
                    isWhitelisted = false,
                    category = "Video Reels",
                    iconTintHex = 0xFF00F2FE
                ),
                BlockedAppEntity(
                    packageName = "com.twitter.android",
                    appName = "X (Twitter)",
                    isBlocked = true,
                    isWhitelisted = false,
                    category = "Social Media",
                    iconTintHex = 0xFF1DA1F2
                ),
                BlockedAppEntity(
                    packageName = "com.reddit.frontpage",
                    appName = "Reddit",
                    isBlocked = true,
                    isWhitelisted = false,
                    category = "Social Media",
                    iconTintHex = 0xFFFF4500
                ),
                BlockedAppEntity(
                    packageName = "com.snapchat.android",
                    appName = "Snapchat",
                    isBlocked = true,
                    isWhitelisted = false,
                    category = "Social Media",
                    iconTintHex = 0xFFFFFC00
                ),

                // Essential Whitelist
                BlockedAppEntity(
                    packageName = "com.whatsapp",
                    appName = "WhatsApp",
                    isBlocked = false,
                    isWhitelisted = true,
                    category = "Essential",
                    isEssential = true,
                    iconTintHex = 0xFF25D366
                ),
                BlockedAppEntity(
                    packageName = "com.google.android.gm",
                    appName = "Gmail",
                    isBlocked = false,
                    isWhitelisted = true,
                    category = "Essential",
                    isEssential = true,
                    iconTintHex = 0xFFEA4335
                ),
                BlockedAppEntity(
                    packageName = "com.google.android.dialer",
                    appName = "Phone Calls",
                    isBlocked = false,
                    isWhitelisted = true,
                    category = "Essential",
                    isEssential = true,
                    iconTintHex = 0xFF34A853
                ),
                BlockedAppEntity(
                    packageName = "com.samsung.android.dialer",
                    appName = "Samsung Phone",
                    isBlocked = false,
                    isWhitelisted = true,
                    category = "Essential",
                    isEssential = true,
                    iconTintHex = 0xFF34A853
                ),
                BlockedAppEntity(
                    packageName = "com.google.android.apps.messaging",
                    appName = "Messages",
                    isBlocked = false,
                    isWhitelisted = true,
                    category = "Essential",
                    isEssential = true,
                    iconTintHex = 0xFF1A73E8
                ),
                BlockedAppEntity(
                    packageName = "com.google.android.apps.docs",
                    appName = "Google Drive",
                    isBlocked = false,
                    isWhitelisted = true,
                    category = "Productivity",
                    iconTintHex = 0xFFFFBA00
                )
            )

            database.blockedAppDao().insertAll(defaultApps)

            database.alarmDao().saveAlarmSetting(
                AlarmSetting(
                    id = 1,
                    hour = 6,
                    minute = 30,
                    isEnabled = true,
                    requiredPhrase = "Ami ghum theke uthechi",
                    secondaryPhrase = "আমি ঘুম থেকে উঠেছি",
                    vibrateContinuously = true,
                    soundEnabled = true
                )
            )

            database.disciplineStatsDao().saveStats(
                DisciplineStats(
                    id = 1,
                    streakDays = 5,
                    bestStreakDays = 7,
                    dailyGoalMinutes = 50,
                    todayFocusMinutes = 35,
                    todayGoalCompleted = false,
                    lastGoalDate = "",
                    totalFocusMinutes = 240,
                    totalSessions = 7,
                    totalDistractionsBlocked = 28,
                    voiceAlarmsCleared = 4,
                    unlockedBadgeIds = "badge_1,badge_3"
                )
            )

            // Seed initial past session for stats gallery
            database.focusDao().insertSession(
                FocusSessionEntity(
                    id = 1,
                    startTime = System.currentTimeMillis() - 86400000L,
                    endTime = System.currentTimeMillis() - 86400000L + 2700000L,
                    durationMinutes = 45,
                    deskPhotoUri = null,
                    distractionsBlocked = 3,
                    completedSuccessfully = true,
                    dateFormatted = "Yesterday, 3:30 PM"
                )
            )

            // Seed default daily study schedule sessions
            database.studyScheduleDao().insertSchedule(
                com.example.data.model.StudyScheduleEntity(
                    id = 1,
                    title = "Morning Deep Work",
                    startHour = 9,
                    startMinute = 0,
                    durationMinutes = 45,
                    daysOfWeek = "Mon,Tue,Wed,Thu,Fri",
                    isEnabled = true,
                    reminderMinutesBefore = 10,
                    category = "Deep Work",
                    colorHex = 0xFF06B6D4
                )
            )

            database.studyScheduleDao().insertSchedule(
                com.example.data.model.StudyScheduleEntity(
                    id = 2,
                    title = "Evening Revision & Reading",
                    startHour = 19,
                    startMinute = 30,
                    durationMinutes = 60,
                    daysOfWeek = "Mon,Tue,Wed,Thu,Fri,Sat",
                    isEnabled = true,
                    reminderMinutesBefore = 15,
                    category = "Reading",
                    colorHex = 0xFF10B981
                )
            )
        }
    }
}
