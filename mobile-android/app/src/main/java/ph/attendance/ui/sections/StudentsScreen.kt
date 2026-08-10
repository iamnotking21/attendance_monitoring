package ph.attendance.ui.sections

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import ph.attendance.domain.Gender
import ph.attendance.domain.Student
import ph.attendance.ui.AppViewModel
import ph.attendance.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsScreen(viewModel: AppViewModel, sectionId: String, onBack: () -> Unit) {
    val students by remember(sectionId) { viewModel.repository.observeStudents(sectionId) }
        .collectAsStateWithLifecycle(emptyList())
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val section = sections.firstOrNull { it.id == sectionId }

    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Student?>(null) }
    var creating by remember { mutableStateOf(false) }
    var showQrFor by remember { mutableStateOf<Student?>(null) }
    var pendingDelete by remember { mutableStateOf<Student?>(null) }

    val visible = remember(students, query) {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) students
        else students.filter {
            it.studentNumber.lowercase().contains(needle) || it.displayName.lowercase().contains(needle)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(section?.name ?: "Students") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add student") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search by name or student number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (visible.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.PersonAdd,
                    title = if (query.isBlank()) "No students yet" else "No matches",
                    description = if (query.isBlank()) {
                        "Add the students in this section. Each one gets a QR code to scan."
                    } else {
                        "No student in this section matches that search."
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visible, key = { it.id }) { student ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(student.displayName, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "${student.studentNumber} · ${if (student.gender == Gender.MALE) "Boy" else "Girl"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { showQrFor = student }) {
                                    Icon(Icons.Filled.QrCode2, contentDescription = "QR code for ${student.displayName}")
                                }
                                IconButton(onClick = { editing = student }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit ${student.displayName}")
                                }
                                IconButton(onClick = { pendingDelete = student }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove ${student.displayName}")
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
        StudentDialog(
            student = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { number, last, first, middle, gender ->
                viewModel.launch {
                    viewModel.repository.saveStudent(
                        existingId = editing?.id,
                        sectionId = sectionId,
                        studentNumber = number,
                        lastName = last,
                        firstName = first,
                        middleName = middle,
                        gender = gender,
                    )
                        .onSuccess { viewModel.say(if (editing == null) "Added $last, $first." else "Student updated.") }
                        .onFailure { viewModel.say(it.message ?: "Could not save that student.") }
                }
                creating = false
                editing = null
            },
        )
    }

    showQrFor?.let { student ->
        val bitmap = remember(student.studentNumber) { QrCodes.render(student.studentNumber) }
        AlertDialog(
            onDismissRequest = { showQrFor = null },
            title = { Text(student.displayName) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "QR code encoding student number ${student.studentNumber}",
                            modifier = Modifier.size(240.dp),
                        )
                    } else {
                        Text("Could not generate that QR code.")
                    }
                    Text(
                        student.studentNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showQrFor = null }) { Text("Close") } },
        )
    }

    pendingDelete?.let { student ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove ${student.displayName}?") },
            text = {
                Text(
                    "They leave the roster and future attendance. Records already taken are kept, " +
                        "so past reports stay accurate.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.launch {
                        viewModel.repository.archiveStudent(student.id)
                        viewModel.say("Removed ${student.displayName}.")
                    }
                    pendingDelete = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StudentDialog(
    student: Student?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Gender) -> Unit,
) {
    var number by remember { mutableStateOf(student?.studentNumber.orEmpty()) }
    var last by remember { mutableStateOf(student?.lastName.orEmpty()) }
    var first by remember { mutableStateOf(student?.firstName.orEmpty()) }
    var middle by remember { mutableStateOf(student?.middleName.orEmpty()) }
    var gender by remember { mutableStateOf(student?.gender ?: Gender.MALE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (student == null) "Add student" else "Edit student") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = last,
                    onValueChange = { last = it.take(60) },
                    label = { Text("Last name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = first,
                    onValueChange = { first = it.take(60) },
                    label = { Text("First name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = middle,
                    onValueChange = { middle = it.take(60) },
                    label = { Text("Middle name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it.take(32) },
                    label = { Text("Student number") },
                    supportingText = { Text("Letters, digits, dots, hyphens, underscores") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = gender == Gender.MALE,
                        onClick = { gender = Gender.MALE },
                        label = { Text("Boy") },
                    )
                    FilterChip(
                        selected = gender == Gender.FEMALE,
                        onClick = { gender = Gender.FEMALE },
                        label = { Text("Girl") },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(number, last, first, middle, gender) },
                enabled = number.isNotBlank() && last.isNotBlank() && first.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
