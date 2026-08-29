package com.clockout.app

import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clockout.app.ui.ClockOutApp
import com.clockout.app.ui.ClockOutViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestHighestRefreshRate()
        enableEdgeToEdge()
        setContent {
            ClockOutApp(viewModel<ClockOutViewModel>())
        }
    }

    override fun onResume() {
        super.onResume()
        requestHighestRefreshRate()
    }

    /**
     * Ask Android for the fastest mode available at the display's current resolution.
     * Device refresh-rate preferences, battery saver and thermal throttling can still
     * override this request, so this is deliberately a preference rather than a hack.
     */
    @Suppress("DEPRECATION")
    private fun requestHighestRefreshRate() {
        val display: Display = windowManager.defaultDisplay
        val current = display.mode
        val fastest = display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .maxByOrNull { it.refreshRate }
            ?: return
        window.attributes = window.attributes.apply {
            preferredDisplayModeId = fastest.modeId
            preferredRefreshRate = fastest.refreshRate
        }
    }
}
