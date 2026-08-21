package com.cartalk.auto

import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class CarTalkCarAppService : CarAppService() {

    override fun createHostValidator(): HostValidator {
        // Debug / debuggable installs may allow all hosts for local testing.
        // Release builds must use the Play/OEM host allowlist sample.
        val debuggable =
            (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return if (debuggable) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }
    }

    override fun onCreateSession(): Session {
        return CarTalkSession()
    }
}

class CarTalkSession : Session() {
    override fun onCreateScreen(intent: android.content.Intent): Screen {
        return MainCarScreen(carContext)
    }
}
