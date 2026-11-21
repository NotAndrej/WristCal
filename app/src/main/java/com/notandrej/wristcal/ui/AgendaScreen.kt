package com.notandrej.wristcal.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.notandrej.wristcal.data.CalendarRepository
import com.notandrej.wristcal.data.EventItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private suspend fun loadEvents(context: Context): List<EventItem> =
    withContext(Dispatchers.IO) { CalendarRepository.upcoming(context) }

/**
 * The whole app: an agenda list that drills into a single event. [resumeTick]
 * is bumped by the host activity on every resume so the agenda re-reads the
 * provider when the user comes back from the watch face.
 */
@Composable
fun AgendaScreen(resumeTick: StateFlow<Int>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var events by remember { mutableStateOf<List<EventItem>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<EventItem?>(null) }

    val tick by resumeTick.collectAsState()

    LaunchedEffect(tick) {
        events = loadEvents(context)
        loaded = true
    }

    val refresh: () -> Unit = {
        scope.launch {
            events = loadEvents(context)
            loaded = true
        }
    }

    val current = selected
    if (current != null) {
        EventDetail(event = current, onBack = { selected = null })
    } else {
        AgendaList(
            events = events,
            loaded = loaded,
            onSelect = { selected = it },
            onRefresh = refresh,
        )
    }
}

@Composable
private fun AgendaList(
    events: List<EventItem>,
    loaded: Boolean,
    onSelect: (EventItem) -> Unit,
    onRefresh: () -> Unit,
) {
    val state = rememberScalingLazyListState()
    val grouped = remember(events) { groupByDay(events) }

    Box(Modifier.fillMaxSize()) {
        ScalingLazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            item(0) {
                Text(
                    text = "Upcoming",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                )
            }

            if (loaded && events.isEmpty()) {
                item(1) { EmptyState() }
            }

            grouped.forEachIndexed { dayIndex, (day, dayEvents) ->
                item(key = "day-$day") { DayHeader(day) }
                items(
                    count = dayEvents.size,
                    key = { i -> "${dayIndex}-${dayEvents[i].id}-${dayEvents[i].begin}" },
                ) { i ->
                    EventChip(event = dayEvents[i], onSelect = onSelect)
                }
            }

            item(key = "refresh") {
                Chip(
                    onClick = onRefresh,
                    label = { Text("Refresh") },
                    secondaryLabel = { Text("Re-read calendar") },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
        }

        TimeText(modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun DayHeader(day: LocalDate) {
    Text(
        text = dayLabel(day),
        style = MaterialTheme.typography.title3,
        color = MaterialTheme.colors.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun EventChip(event: EventItem, onSelect: (EventItem) -> Unit) {
    Chip(
        onClick = { onSelect(event) },
        label = {
            Text(
                text = event.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        secondaryLabel = {
            Text(
                text = timeLabel(event),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (event.color != 0) Color(event.color) else MaterialTheme.colors.primary
                    ),
            )
        },
        colors = ChipDefaults.secondaryChipColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.DateRange,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colors.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "No upcoming events",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.title3,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Turn on Calendar sync in system settings, then tap Refresh.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun EventDetail(event: EventItem, onBack: () -> Unit) {
    BackHandler { onBack() }
    val state = rememberScalingLazyListState()

    Box(Modifier.fillMaxSize()) {
        ScalingLazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(0) {
                Chip(
                    onClick = onBack,
                    label = { Text("Back") },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item(1) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.title2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                )
            }

            item(2) { DetailRow(Icons.Filled.Schedule, whenLabel(event)) }

            event.location?.let { location ->
                item(3) { DetailRow(Icons.Filled.Place, location) }
            }

            event.calendar?.let { calendar ->
                item(4) {
                    Text(
                        text = calendar,
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            event.description?.let { description ->
                item(5) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        TimeText(modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun DetailRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colors.primary,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---- formatting helpers ----------------------------------------------------

private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

private fun zone(): ZoneId = ZoneId.systemDefault()

private fun startDate(event: EventItem): LocalDate =
    Instant.ofEpochMilli(event.begin).atZone(zone()).toLocalDate()

private fun groupByDay(events: List<EventItem>): List<Pair<LocalDate, List<EventItem>>> {
    val byDay = LinkedHashMap<LocalDate, MutableList<EventItem>>()
    for (event in events) {
        byDay.getOrPut(startDate(event)) { mutableListOf() }.add(event)
    }
    return byDay.entries.map { it.key to it.value }
}

private fun dayLabel(day: LocalDate): String = when (day) {
    LocalDate.now() -> "Today"
    LocalDate.now().plusDays(1) -> "Tomorrow"
    else -> day.format(dayFormat)
}

private fun timeLabel(event: EventItem): String {
    if (event.allDay) return "All day"
    val z = zone()
    val begin = Instant.ofEpochMilli(event.begin).atZone(z)
    val end = Instant.ofEpochMilli(event.end).atZone(z)
    return "${begin.format(timeFormat)} - ${end.format(timeFormat)}"
}

private fun whenLabel(event: EventItem): String {
    val day = dayLabel(startDate(event))
    return if (event.allDay) "$day / All day" else "$day / ${timeLabel(event)}"
}
