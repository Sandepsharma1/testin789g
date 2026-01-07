package com.orignal.buddylynk

import android.app.Application
import android.content.Context

/**
 * BuddyLynkApplication - Application class for global app context
 * 
 * Provides static access to application context for:
 * - SharedPreferences (AppStateManager)
 * - Other system services that need context
 */
class BuddyLynkApplication : Application() {
    
    companion object {
        lateinit var appContext: Context
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }
}
