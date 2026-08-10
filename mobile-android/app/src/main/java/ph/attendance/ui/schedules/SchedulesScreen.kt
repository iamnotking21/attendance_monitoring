package ph.attendance.ui.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.flowOf
import ph.attendance.data.scheduleDraft
import ph.attendance.domain.Clocks
import ph.attendance.domain.Schedule
import ph.attendance.domain.ScheduleWindowState
import ph.attendance.domain.formatTime12
import ph.attendance.domain.windowStateAt
import ph.attendance.ui.AppViewModel
import ph.attendance.ui.components.EmptyState
import ph.attendance.ui.components.SectionPicker

@Composable
fun SchedulesScreen(viewModel: AppViewModel) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val selected = viewModel.resolveSelected(sections)
    val sectionId = selected?.id

    val schedules by remember(sectionId) {
        if (sectionId == null) flowOf(emptyList()) else viewModel.repository.observeSchedules(sectionId)
    }.collectAsStateWithLifecycle(emptyList())

    var editing by remember { mutableStateOf<Schedule?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Schedule?>(null) }

    Scaffold(
        floatingActionButton = {
            if (sectionId != null) {
                ExtendedFloatingActionButton(
                    onClick = { creating = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New schedule") },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Text(
                "Schedules",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            Text(
                "Each schedule opens a present window, then a late window. A scan outside both records nothing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            if (sections.isNotEmpty()) {
                SectionPicker(sections, selected, viewModel::selectSection)
            }

            if (schedules.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.CalendarMonth,
                    title = if (sections.isEmpty()) "No sections yet" else "No schedules for this section",
                    description = if (sections.isEmpty()) {
                        "Schedules belong to a section, so create one first."
                    } else {
                        "Add one so the scanner knows when to accept attendance."
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                val atMinutes = Clocks.minutesOfDay()
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(schedules, key = { it.id }) { schedule ->
                        ScheduleCard(
                            schedule = schedule,
                            state = windowStateAt(schedule, atMinutes),
                            onEdit = { editing = schedule },
                            onDelete = { pendingDelete = schedule },
                        )
                    }
                    item { Box(Modifier.padding(40.dp)) }
                }
            }
        }
    }

    if ((creating || editing != null) && sectionId != null) {
        ScheduleDialog(
            schedule = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { title, venue, ps, pe, ls, le ->
                viewModel.launch {
                    viewModel.repository
                        .saveSchedule(editing?.id, sectionId, scheduleDraft(title, venue, ps, pe, ls, le))
                        .onSuccess { viewModel.say(if (editing == null) "Created $title." else "Schedule updated.") }
                        .onFailure { viewModel.say(it.message ?: "Could not save that schedule.") }
                }
                creating = false
                editing = null
            },
        )
    }

    pendingDelete?.let { schedule ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove ${schedule.title}?") },
            text = { Text("It stops accepting scans. Attendance already recorded against it is kept.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.launch {
                        viewModel.repository.archiveSchedule(schedule.id)
                        viewModel.say("Removed ${schedule.title}.")
                    }
                    pendingDelete = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ScheduleCard(
    schedule: Schedule,
    state: ScheduleWindowState,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(schedule.title, style = MaterialTheme.typography.titleMedium)
                    if (schedule.venue.isNotBlank()) {
                        Text(
                            schedule.venue,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                AssistChip(onClick = {}, enabled = false, label = { Text(state.describe()) })
            }

            Text(
                "Present ${formatTime12(schedule.present.start)} – ${formatTime12(schedule.present.end)}",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                "Late    ${formatTime12(schedule.late.start)} – ${formatTime12(schedule.late.end)}",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit ${schedule.title}")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove ${schedule.title}")
                }
            }
        }
    }
}

private fun ScheduleWindowState.describe(): String = when (this) {
    ScheduleWindowState.BEFORE -> "Not started"
    ScheduleWindowState.PRESENT -> "Taking attendance"
    ScheduleWindowState.GAP -> "Between windows"
    ScheduleWindowState.LATE -> "Late arrivals only"
    ScheduleWindowState.CLOSED -> "Closed"
}

@Composable
private fun ScheduleDialog(
    schedule: Schedule?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit,
) {
    var title by remember { mutableStateOf(schedule?.title.orEmpty()) }
    var venue by remember { mutableStateOf(schedule?.venue.orEmpty()) }
    var presentStart by remember { mutableStateOf(schedule?.present?.start ?: "07:00") }
    var presentEnd by remember { mutableStateOf(schedule?.present?.end ?: "07:30") }
    var lateStart by remember { mutableStateOf(schedule?.late?.start ?: "07:30") }
    var lateEnd by remember { mutableStateOf(schedule?.late?.end ?: "08:00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (schedule == null) "New schedule" else "Edit schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    label = { Text("Title") },
                    placeholder = { Text("Morning Assembly") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it.take(80) },
                    label = { Text("Venue (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeField("Present from", presentStart, Modifier.weight(1f)) {
                        presentStart = it
                    }
                    TimeField("Present to", presentEnd, Modifier.weight(1f)) {
                        presentEnd = it
                        // Butting the late window against the present one is what people almost
                        // always want; they can still pull it apart to leave a gap.
                        lateStart = it
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeField("Late from", lateStart, Modifier.weight(1f)) { lateStart = it }
                    TimeField("Late to", lateEnd, Modifier.weight(1f)) { lateEnd = it }
                }
                Text(
                    "Everyone not scanned by the late cut-off is marked absent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, venue, presentStart, presentEnd, lateStart, lateEnd) },
                enabled = title.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun TimeField(label: String, value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(5)) },
        label = { Text(label) },
        placeholder = { Text("07:00") },
        singleLine = true,
        modifier = modifier,
    )
}
