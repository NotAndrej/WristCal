package com.notandrej.wristcal.data

/**
 * One occurrence of a calendar event, flattened from the platform
 * [android.provider.CalendarContract.Instances] table.
 *
 * @param id        the underlying event id ([android.provider.CalendarContract.Instances.EVENT_ID])
 * @param title     the event title, never blank
 * @param begin     start time, epoch millis
 * @param end       end time, epoch millis
 * @param allDay    whether the event is an all-day event
 * @param location  free-text location, if the event has one
 * @param description free-text notes, if the event has any
 * @param calendar  the display name of the owning calendar
 * @param color     the calendar's ARGB display colour
 */
data class EventItem(
    val id: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val location: String?,
    val description: String?,
    val calendar: String?,
    val color: Int,
)
