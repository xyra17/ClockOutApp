package com.clockout.app

import android.app.Application
import com.clockout.app.data.ClockOutDatabase
import com.clockout.app.data.ClockOutRepository
import com.clockout.app.data.SettingsStore
import com.clockout.app.notification.ReminderScheduler

class ClockOutApplication : Application() {
    lateinit var repository: ClockOutRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = ClockOutRepository(
            ClockOutDatabase.get(this).workDayDao(),
            SettingsStore(this),
            ReminderScheduler(this),
        )
    }
}
