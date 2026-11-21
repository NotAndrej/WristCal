package com.notandrej.wristcal.data

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import java.util.Calendar

/**
 * Reads upcoming events from the on-watch calendar provider.
 *
 * Wear OS keeps a local copy of the paired phone's calendars in the standard
 * [CalendarContract] provider (authority `com.android.calendar`), so a
 * standalone watch app only ever needs [android.Manifest.permission.READ_CALENDAR]
 * and a normal provider query - no network and no Google account on the watch.
 */
object CalendarRepository {

    private val PROJECTION = arrayOf(
        CalendarContract.Instances.EVENT_ID,            // 0
        CalendarContract.Instances.TITLE,               // 1
        CalendarContract.Instances.BEGIN,               // 2
        CalendarContract.Instances.END,                 // 3
        CalendarContract.Instances.ALL_DAY,             // 4
        CalendarContract.Instances.EVENT_LOCATION,      // 5
        CalendarContract.Instances.DESCRIPTION,         // 6
        CalendarContract.Instances.CALENDAR_DISPLAY_NAME, // 7
        CalendarContract.Instances.DISPLAY_COLOR,       // 8
    )

    /**
     * All non-cancelled event occurrences between the start of today and
     * [days] days from now, ordered by start time.
     *
     * Returns an empty list (never throws) when the provider is missing or the
     * caller lacks permission, so the UI can simply show its empty state.
     */
    fun upcoming(context: Context, days: Int = 90): List<EventItem> {
        val startOfToday = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = startOfToday + days.toLong() * DAY_MILLIS

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().apply {
            ContentUris.appendId(this, startOfToday)
            ContentUris.appendId(this, end)
        }.build()

        val selection = "${CalendarContract.Instances.STATUS} != ${CalendarContract.Instances.STATUS_CANCELED}"

        val out = ArrayList<EventItem>()
        runCatching {
            context.contentResolver
                .query(uri, PROJECTION, selection, null, "${CalendarContract.Instances.BEGIN} ASC")
                ?.use { c ->
                    while (c.moveToNext()) {
                        out += EventItem(
                            id = c.getLong(0),
                            title = c.getStringOrNull(1)?.trim().takeUnless { it.isNullOrEmpty() }
                                ?: "(no title)",
                            begin = c.getLong(2),
                            end = c.getLong(3),
                            allDay = c.getIntOrNull(4) == 1,
                            location = c.getStringOrNull(5)?.trim().takeUnless { it.isNullOrEmpty() },
                            description = c.getStringOrNull(6)?.trim().takeUnless { it.isNullOrEmpty() },
                            calendar = c.getStringOrNull(7),
                            color = c.getIntOrNull(8) ?: 0,
                        )
                    }
                }
        }
        return out
    }

    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
}
