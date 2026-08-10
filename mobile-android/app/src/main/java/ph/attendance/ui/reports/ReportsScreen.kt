package ph.attendance.ui.reports

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import kotlinx.coroutines.flow.flowOf
import ph.attendance.domain.Clocks
import ph.attendance.domain.addDays
import ph.attendance.domain.formatDateShort
import ph.attendance.domain.formatRate
import ph.attendance.domain.monthRange
import ph.attendance.domain.summariseStudents
import ph.attendance.domain.toCsv
import ph.attendance.domain.toSafeFilename
import ph.attendance.ui.AppViewModel
import ph.attendance.ui.components.EmptyState
import ph.attendance.ui.components.SectionPicker
import ph.attendance.ui.theme.LocalStatusColors

@Composable
fun ReportsScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val selected = viewModel.resolveSelected(sections)
    val sectionId = selected?.id

    var range by remember { mutableStateOf(monthRange(Clocks.today())) }

    val students by remember(sectionId) {
        if (sectionId == null) flowOf(emptyList()) else viewModel.repository.observeStudents(sectionId)
    }.collectAsStateWithLifecycle(emptyList())

    val records by remember(sectionId, range) {
        if (sectionId == null) flowOf(emptyList())
        else viewModel.repository.observeRecordsBetween(sectionId, range.first, range.second)
    }.collectAsStateWithLifecycle(emptyList())

    val summaries = remember(students, records, range) {
        summariseStudents(students, records, range.first, range.second)
    }
    val statusColors = LocalStatusColors.current

    if (sections.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Insights,
            title = "Nothing to report on yet",
            description = "Create a section and record some attendance first.",
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(Modifier.padding(top = 16.dp)) {
                Text("Reports", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Late counts as attending — a punctuality problem, not an absence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { SectionPicker(sections, selected, viewModel::selectSection) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { range = monthRange(Clocks.today()) },
                    label = { Text("This month") },
                )
                AssistChip(
                    onClick = { range = addDays(Clocks.today(), -6) to Clocks.today() },
                    label = { Text("Last 7 days") },
                )
                AssistChip(
                    onClick = { range = addDays(Clocks.today(), -29) to Clocks.today() },
                    label = { Text("Last 30 days") },
                )
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${formatDateShort(range.first)} – ${formatDateShort(range.second)} · ${summaries.size} students",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = {
                        viewModel.launch {
                            runCatching {
                                shareCsv(context, selected?.name.orEmpty(), range, summaries)
                            }.onFailure { viewModel.say(it.message ?: "Could not build that export.") }
                        }
                    },
                    enabled = summaries.isNotEmpty(),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, Modifier.padding(end = 6.dp))
                    Text("Export CSV")
                }
            }
        }

        items(summaries, key = { it.student.id }) { summary ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(summary.displayName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                summary.student.studentNumber,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            if (summary.sessions == 0) "—" else formatRate(summary.rate),
                            style = MaterialTheme.typography.titleMedium,
                            color = when {
                                summary.sessions == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                                summary.rate >= 0.9f -> statusColors.present
                                summary.rate >= 0.75f -> statusColors.late
                                else -> statusColors.absent
                            },
                        )
                    }
                    Text(
                        "Present ${summary.counts.present} · Late ${summary.counts.late} · Absent ${summary.counts.absent}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        item { Column(Modifier.padding(24.dp)) {} }
    }
}

/**
 * Writes the CSV into the app's cache and hands it to the system share sheet.
 *
 * Through a FileProvider rather than to external storage, so the app needs no storage permission
 * and nothing leaves the sandbox unless the user picks a destination.
 */
/**
 * Written as an escape rather than a literal character: a byte-order mark sitting invisibly in a
 * source file is impossible to review and trips Android lint.
 */
private const val BOM = "\uFEFF"

private fun shareCsv(
    context: android.content.Context,
    sectionName: String,
    range: Pair<String, String>,
    summaries: List<ph.attendance.domain.StudentSummary>,
) {
    val rows = mutableListOf<List<String?>>(
        listOf("Student number", "Name", "Gender", "Present", "Late", "Absent", "Sessions", "Attendance rate"),
    )
    summaries.forEach { summary ->
        rows += listOf(
            summary.student.studentNumber,
            summary.displayName,
            if (summary.student.gender == ph.attendance.domain.Gender.MALE) "Boy" else "Girl",
            summary.counts.present.toString(),
            summary.counts.late.toString(),
            summary.counts.absent.toString(),
            summary.sessions.toString(),
            formatRate(summary.rate),
        )
    }

    val exports = File(context.cacheDir, "exports").apply { mkdirs() }
    val name = "attendance-${toSafeFilename(sectionName, "section")}-${range.first}-to-${range.second}.csv"
    val file = File(exports, name)

    // A BOM, so Excel opens it as UTF-8 rather than mangling every accented name. Written as an
    // escape rather than a literal character, which would sit invisibly in the source file.
    file.writeText(BOM + toCsv(rows))

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share attendance report"))
}
