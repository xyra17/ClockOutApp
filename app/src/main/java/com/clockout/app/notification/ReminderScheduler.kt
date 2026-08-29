package com.clockout.app.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.clockout.app.domain.AppSettings
import com.clockout.app.domain.WorkDay
import com.clockout.app.domain.WorkTimeCalculator
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    fun schedule(day: WorkDay?, settings: AppSettings, now: Instant = Instant.now()) {
        cancel()
        if (!settings.reminderEnabled || day == null || day.actualClockOut != null || day.isRestDay) return
        val expected = WorkTimeCalculator.expectedClockOut(day, now) ?: return
        val trigger = expected.minus(Duration.ofMinutes(settings.reminderLeadMinutes.toLong()))
        if (!trigger.isAfter(now)) return
        val data = Data.Builder()
            .putLong(ClockOutReminderWorker.EXPECTED_AT, expected.toEpochMilli())
            .putInt(ClockOutReminderWorker.LEAD_MINUTES, settings.reminderLeadMinutes)
            .build()
        val request = OneTimeWorkRequestBuilder<ClockOutReminderWorker>()
            .setInitialDelay(Duration.between(now, trigger).toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel() = WorkManager.getInstance(context).cancelUniqueWork(NAME)

    companion object {
        private const val NAME = "clockout_departure_reminder"
        const val TAG = "clockout_reminder"
    }
}
