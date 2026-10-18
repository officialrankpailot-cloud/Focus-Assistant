package com.example.schedule

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.DisciplineApp
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StudyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
        if (scheduleId == -1L) return

        val title = intent.getStringExtra(EXTRA_SCHEDULE_TITLE) ?: "Scheduled Study Session"
        val duration = intent.getIntExtra(EXTRA_SCHEDULE_DURATION, 45)
        val minutesBefore = intent.getIntExtra(EXTRA_MINUTES_BEFORE, 10)

        showStudyReminderNotification(context, scheduleId, title, duration, minutesBefore)

        // Reschedule for next day / week
        CoroutineScope(Dispatchers.IO).launch {
            val db = DisciplineApp.instance.database
            val schedule = db.studyScheduleDao().getScheduleById(scheduleId)
            if (schedule != null && schedule.isEnabled) {
                StudyReminderScheduler.scheduleReminder(context, schedule)
            }
        }
    }

    private fun showStudyReminderNotification(
        context: Context,
        scheduleId: Long,
        title: String,
        duration: Int,
        minutesBefore: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Study Schedule Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled daily study and focus sessions"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_LAUNCH_SCHEDULED_SESSION", true)
            putExtra("EXTRA_SCHEDULED_DURATION", duration)
            putExtra("EXTRA_SCHEDULED_TITLE", title)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            scheduleId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timingText = if (minutesBefore > 0) "starts in $minutesBefore minutes" else "is starting now"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.discipline_icon_1792316234799)
            .setContentTitle("📚 Study Block: $title")
            .setContentText("Your $duration-min session $timingText. Snap desk photo to begin!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your $duration-minute study session \"$title\" $timingText.\nClear your workspace, prepare your study notes, and verify your desk to activate Focus Mode.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.discipline_icon_1792316234799,
                "Open & Snap Desk",
                pendingIntent
            )
            .build()

        notificationManager.notify((NOTIFICATION_BASE_ID + scheduleId).toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "study_schedule_reminders"
        const val NOTIFICATION_BASE_ID = 2000
        const val ACTION_TRIGGER_REMINDER = "com.example.disciplinefocus.TRIGGER_STUDY_REMINDER"
        const val EXTRA_SCHEDULE_ID = "EXTRA_SCHEDULE_ID"
        const val EXTRA_SCHEDULE_TITLE = "EXTRA_SCHEDULE_TITLE"
        const val EXTRA_SCHEDULE_DURATION = "EXTRA_SCHEDULE_DURATION"
        const val EXTRA_MINUTES_BEFORE = "EXTRA_MINUTES_BEFORE"
    }
}
