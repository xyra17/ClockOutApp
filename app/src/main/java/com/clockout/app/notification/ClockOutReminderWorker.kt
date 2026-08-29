package com.clockout.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.clockout.app.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ClockOutReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        createChannel()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) return Result.success()

        val expectedMillis = inputData.getLong(EXPECTED_AT, -1)
        val lead = inputData.getInt(LEAD_MINUTES, 0)
        val time = if (expectedMillis > 0) DateTimeFormatter.ofPattern("HH:mm")
            .format(Instant.ofEpochMilli(expectedMillis).atZone(ZoneId.systemDefault())) else ""
        val text = if (lead == 0) "预计下班时间到了，今天辛苦了。" else "还有 $lead 分钟到预计下班时间 $time。"
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_clockout)
            .setContentTitle("ClockOut 下班提醒")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    private fun createChannel() {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "下班提醒", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "在预计下班时间前提醒"
            }
        )
    }

    companion object {
        const val EXPECTED_AT = "expected_at"
        const val LEAD_MINUTES = "lead_minutes"
        private const val CHANNEL_ID = "departure_reminders"
        private const val NOTIFICATION_ID = 1001
    }
}
