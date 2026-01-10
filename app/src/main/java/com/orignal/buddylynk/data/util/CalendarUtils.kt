package com.orignal.buddylynk.data.util

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
// Toast removed
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility for calendar integration
 */
object CalendarUtils {
    
    /**
     * Add event to device calendar
     */
    fun addEventToCalendar(
        context: Context,
        title: String,
        description: String,
        location: String,
        date: String, // e.g. "Dec 25, 2026"
        time: String  // e.g. "7:00 PM"
    ) {
        try {
            // Parse date and time
            val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.US)
            val eventDateTime = dateTimeFormat.parse("$date $time")
            
            val startMillis = eventDateTime?.time ?: System.currentTimeMillis()
            val endMillis = startMillis + 2 * 60 * 60 * 1000 // Default 2 hours duration
            
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.DESCRIPTION, description)
                putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            // Silent
        }
    }
    
    /**
     * Create a calendar reminder intent
     */
    fun createReminderIntent(
        title: String,
        description: String,
        location: String,
        startMillis: Long,
        endMillis: Long
    ): Intent {
        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.Events.EVENT_LOCATION, location)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        }
    }
}
