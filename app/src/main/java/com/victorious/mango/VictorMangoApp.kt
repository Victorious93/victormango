package com.victorious.mango

import android.app.Application
import com.victorious.mango.ui.theme.fleet.FleetThemeClient

/** Owns the read-only VictorLaunch fleet theme client for the whole process. */
class VictorMangoApp : Application() {
    lateinit var fleetTheme: FleetThemeClient
        private set

    override fun onCreate() {
        super.onCreate()
        fleetTheme = FleetThemeClient(this).also { it.start() }
    }
}
