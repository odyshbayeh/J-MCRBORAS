package com.aftersales.app

import android.app.Application
import com.aftersales.app.data.prefs.SessionPreferences
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AfterSalesApplication : Application() {

    @Inject
    lateinit var sessionPreferences: SessionPreferences

    override fun onCreate() {
        super.onCreate()
        sessionPreferences.keepLoggedIn()
    }
}
