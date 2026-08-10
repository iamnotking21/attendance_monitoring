package ph.attendance.ui.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.flowOf
import ph.attendance.domain.AttendanceStatus
import ph.attendance.domain.Clocks
import ph.attendance.domain.Gender
import ph.attendance.domain.buildDashboard
import ph.attendance.domain.formatDateLong
import ph.attendance.domain.formatRate
import ph.attendance.domain.ofGender
import ph.attendance.ui.AppViewModel
import ph.attendance.ui.components.EmptyState
import ph.attendance.ui.components.SectionPicker
import ph.attendance.ui.components.StatTile
import ph.attendance.ui.components.StatusChip
import ph.attendance.ui.theme.LocalStatusColors

private enum class Tab(val label: String) { PRESENT("Present"), LATE("Late"), ABSENT("Absent"), UNSCANNED("Not scanned") }

@Composable
fun DashboardScreen(viewModel: AppViewModel) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val selected = viewModel.resolveSelected(sections)
    val today = remember { Clocks.today() }

    if (sections.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Groups,
            title = "No sections yet",
            description = "Create a section and add students before attendance can be recorded.",
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    val sectionId = selected?.id
    val students by remember(sectionId) {
        if (sectionId == null) flowOf(emptyList()) else viewModel.repository.observeStudents(sectionId)
    }.collectAsStateWithLifecycle(emptyList())

    val records by remember(sectionId, today) {
        if (sectionId == null) flowOf(emptyList()) else viewModel.repository.observeRecords(sectionId, today)
    }.collectAsStateWithLifecycle(emptyList())

    val breakdown = remember(students, records) { buildDashboard(students, records, today) }
    var tab by remember { mutableStateOf(Tab.PRESENT) }
    val statusColors = LocalStatusColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Today", style = MaterialTheme.typography.headlineSmall)
                Text(
                    formatDateLong(today),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionPicker(sections, selected, viewModel::selectSection)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatTile("Present", breakdown.counts.present.toString(), statusColors.present, Modifier.weight(1f))
                StatTile("Late", breakdown.counts.late.toString(), statusColors.late, Modifier.weight(1f))
                StatTile("Absent", breakdown.counts.absent.toString(), statusColors.absent, Modifier.weight(1f))
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Attendance rate", style = MaterialTheme.typography.labelSmall)
                    Text(formatRate(breakdown.counts.rate), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "${students.size} active students · late counts as attending",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Tab.entries.forEach { entry ->
                    val count = when (entry) {
                        Tab.PRESENT -> breakdown.counts.present
                        Tab.LATE -> breakdown.counts.late
                        Tab.ABSENT -> breakdown.counts.absent
                        Tab.UNSCANNED -> breakdown.unaccountedFor.size
                    }
                    FilterChip(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        label = { Text("${entry.label} $count") },
                    )
                }
            }
        }

        val rows: List<Row> = when (tab) {
            Tab.UNSCANNED -> breakdown.unaccountedFor.map {
                Row(it.displayName, it.studentNumber, it.gender, null, "Not scanned yet")
            }
            else -> {
                val status = when (tab) {
                    Tab.PRESENT -> AttendanceStatus.PRESENT
                    Tab.LATE -> AttendanceStatus.LATE
                    else -> AttendanceStatus.ABSENT
                }
                breakdown.entries.filter { it.status == status }.map {
                    Row(it.student.displayName, it.student.studentNumber, it.student.gender, it.status, it.scheduleTitle)
                }
            }
        }

        if (rows.isEmpty()) {
            item {
                Text(
                    "Nobody in this group today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
        } else {
            // Boys and girls listed separately, as the original app's dashboard did — it is how
            // the printed forms are organised, and coordinators read it that way.
            listOf(Gender.MALE to "Boys", Gender.FEMALE to "Girls").forEach { (gender, heading) ->
                val group = rows.filter { it.gender == gender }
                item(key = "heading-$heading-${tab.name}") {
                    Text(
                        "$heading (${group.size})",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(group, key = { "${tab.name}-${it.studentNumber}-${it.detail}" }) { row ->
                    ElevatedCard(Modifier.fillMaxWidth().animateContentSize()) {
                        androidx.compose.foundation.layout.Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(row.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${row.studentNumber} · ${row.detail}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            row.status?.let { StatusChip(it) }
                        }
                    }
                }
            }
        }

        item { androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp)) }
    }
}

private data class Row(
    val name: String,
    val studentNumber: String,
    val gender: Gender,
    val status: AttendanceStatus?,
    val detail: String,
)
