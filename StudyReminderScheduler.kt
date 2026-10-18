package com.example.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.StudyScheduleEntity
import java.util.Calendar

object StudyReminderScheduler {

    fun scheduleReminder(context: Context, schedule: StudyScheduleEntity) {
        if (!schedule.isEnabled) {
            cancelReminder(context, schedule.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = StudyReminderReceiver.ACTION_TRIGGER_REMINDER
            putExtra(StudyReminderReceiver.EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(StudyReminderReceiver.EXTRA_SCHEDULE_TITLE, schedule.title)
            putExtra(StudyReminderReceiver.EXTRA_SCHEDULE_DURATION, schedule.durationMinutes)
            putExtra(StudyReminderReceiver.EXTRA_MINUTES_BEFORE, schedule.reminderMinutesBefore)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (StudyReminderReceiver.NOTIFICATION_BASE_ID + schedule.id).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = computeNextTriggerTime(
            startHour = schedule.startHour,
            startMinute = schedule.startMinute,
            minutesBefore = schedule.reminderMinutesBefore,
            daysOfWeekStr = schedule.daysOfWeek
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d("StudyScheduler", "Scheduled reminder for '${schedule.title}' at $triggerTime")
        } catch (e: SecurityException) {
            Log.e("StudyScheduler", "Alarm permission not granted: ${e.message}")
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context, scheduleId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = StudyReminderReceiver.ACTION_TRIGGER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (StudyReminderReceiver.NOTIFICATION_BASE_ID + scheduleId).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun computeNextTriggerTime(
        startHour: Int,
        startMinute: Int,
        minutesBefore: Int,
        daysOfWeekStr: String
    ): Long {
        val activeDays = parseDaysOfWeek(daysOfWeekStr)
        val now = Calendar.getInstance()

        // Check the next 8 days for the closest matching valid day and time
        for (dayOffset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, startMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MINUTE, -minutesBefore)
            }

            val calDayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            if (activeDays.contains(calDayOfWeek) && candidate.after(now)) {
                return candidate.timeInMillis
            }
        }

        // Default fallback: 1 hour from now
        return System.currentTimeMillis() + 3600000L
    }

    private fun parseDaysOfWeek(daysStr: String): Set<Int> {
        val set = mutableSetOf<Int>()
        val tokens = daysStr.split(",").map { it.trim().lowercase() }
        for (t in tokens) {
            when (t) {
                "sun" -> set.add(Calendar.SUNDAY)
                "mon" -> set.add(Calendar.MONDAY)
                "tue" -> set.add(Calendar.TUESDAY)
                "wed" -> set.add(Calendar.WEDNESDAY)
                "thu" -> set.add(Calendar.THURSDAY)
                "fri" -> set.add(Calendar.FRIDAY)
                "sat" -> set.add(Calendar.SATURDAY)
            }
        }
        if (set.isEmpty()) {
            // Default: All 7 days
            return setOf(
                Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
                Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
            )
        }
        return set
    }
}
