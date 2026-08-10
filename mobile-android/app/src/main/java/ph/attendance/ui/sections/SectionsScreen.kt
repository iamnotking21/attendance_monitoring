package ph.attendance.ui.sections

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ph.attendance.domain.Section
import ph.attendance.ui.AppViewModel
import ph.attendance.ui.components.EmptyState

@Composable
fun SectionsScreen(viewModel: AppViewModel, onOpenSection: (String) -> Unit) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val counts by viewModel.repository.observeStudentCounts()
        .collectAsStateWithLifecycle(emptyMap())

    var editing by remember { mutableStateOf<Section?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Section?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New section") },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (sections.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Groups,
                    title = "No sections yet",
                    description = "A section is one class list. Students, schedules, and records all hang off it.",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            "Sections",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                    items(sections, key = { it.id }) { section ->
                        ElevatedCard(
                            Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                                .clickable { onOpenSection(section.id) },
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(section.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${counts[section.id] ?: 0} students",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { editing = section }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Rename ${section.name}")
                                }
                                IconButton(onClick = { pendingDelete = section }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove ${section.name}")
                                }
                            }
                        }
                    }
                    item { Box(Modifier.padding(40.dp)) }
                }
            }
        }
    }

    if (creating || editing != null) {
        val target = editing
        SectionDialog(
            initialName = target?.name.orEmpty(),
            title = if (target == null) "New section" else "Rename section",
            onDismiss = { creating = false; editing = null },
            onConfirm = { name ->
                viewModel.launch {
                    val result = if (target == null) {
                        viewModel.repository.createSection(name).map { }
                    } else {
                        viewModel.repository.renameSection(target.id, name)
                    }
                    result
                        .onSuccess { viewModel.say(if (target == null) "Created $name." else "Section renamed.") }
                        .onFailure { viewModel.say(it.message ?: "Could not save that section.") }
                }
                creating = false
                editing = null
            },
        )
    }

    pendingDelete?.let { section ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove ${section.name}?") },
            text = {
                Text(
                    "Its students and schedules go with it. Attendance already recorded is kept, " +
                        "so past reports stay accurate.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.launch {
                        viewModel.repository.archiveSection(section.id)
                        viewModel.say("Removed ${section.name}.")
                    }
                    pendingDelete = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SectionDialog(
    initialName: String,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(80) },
                label = { Text("Section name") },
                placeholder = { Text("Grade 11 - Rizal") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
