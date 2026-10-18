package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AlarmSetting
import com.example.data.model.BlockedAppEntity
import com.example.data.model.DisciplineStats
import com.example.data.model.FocusSessionEntity
import com.example.data.model.StudyScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE completedSuccessfully = 1")
    suspend fun getCompletedCount(): Int
}

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarm_settings WHERE id = 1")
    fun getAlarmSetting(): Flow<AlarmSetting?>

    @Query("SELECT * FROM alarm_settings WHERE id = 1")
    suspend fun getAlarmSettingSync(): AlarmSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAlarmSetting(alarmSetting: AlarmSetting)
}

@Dao
interface BlockedAppDao {
    @Query("SELECT * FROM blocked_apps ORDER BY isBlocked DESC, appName ASC")
    fun getAllApps(): Flow<List<BlockedAppEntity>>

    @Query("SELECT packageName FROM blocked_apps WHERE isBlocked = 1 AND isWhitelisted = 0")
    suspend fun getBlockedPackageNames(): List<String>

    @Query("SELECT packageName FROM blocked_apps WHERE isWhitelisted = 1")
    suspend fun getWhitelistedPackageNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<BlockedAppEntity>)

    @Update
    suspend fun updateApp(app: BlockedAppEntity)

    @Query("UPDATE blocked_apps SET isBlocked = :isBlocked WHERE packageName = :packageName")
    suspend fun setBlockedStatus(packageName: String, isBlocked: Boolean)

    @Query("UPDATE blocked_apps SET isWhitelisted = :isWhitelisted, isBlocked = CASE WHEN :isWhitelisted = 1 THEN 0 ELSE isBlocked END WHERE packageName = :packageName")
    suspend fun setWhitelistedStatus(packageName: String, isWhitelisted: Boolean)
}

@Dao
interface DisciplineStatsDao {
    @Query("SELECT * FROM discipline_stats WHERE id = 1")
    fun getStats(): Flow<DisciplineStats?>

    @Query("SELECT * FROM discipline_stats WHERE id = 1")
    suspend fun getStatsSync(): DisciplineStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStats(stats: DisciplineStats)

    @Query("UPDATE discipline_stats SET totalDistractionsBlocked = totalDistractionsBlocked + 1 WHERE id = 1")
    suspend fun incrementDistractionsBlocked()

    @Query("UPDATE discipline_stats SET voiceAlarmsCleared = voiceAlarmsCleared + 1 WHERE id = 1")
    suspend fun incrementVoiceAlarmsCleared()

    @Query("UPDATE discipline_stats SET dailyGoalMinutes = :goalMinutes WHERE id = 1")
    suspend fun updateDailyGoal(goalMinutes: Int)
}

@Dao
interface StudyScheduleDao {
    @Query("SELECT * FROM study_schedules ORDER BY startHour ASC, startMinute ASC")
    fun getAllSchedules(): Flow<List<StudyScheduleEntity>>

    @Query("SELECT * FROM study_schedules WHERE isEnabled = 1 ORDER BY startHour ASC, startMinute ASC")
    suspend fun getActiveSchedules(): List<StudyScheduleEntity>

    @Query("SELECT * FROM study_schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): StudyScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: StudyScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: StudyScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: StudyScheduleEntity)

    @Query("UPDATE study_schedules SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setScheduleEnabled(id: Long, isEnabled: Boolean)
}
