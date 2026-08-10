package ph.attendance.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ph.attendance.data.SyncConnection
import ph.attendance.sync.SyncEngine
import ph.attendance.ui.AppViewModel

@Composable
fun SyncScreen(viewModel: AppViewModel) {
    val connection by viewModel.settings.connection.collectAsStateWithLifecycle(null)
    val lastSyncedAt by viewModel.settings.lastSyncedAt.collectAsStateWithLifecycle(null)

    var serverUrl by remember { mutableStateOf("https://") }
    var workspaceName by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var confirmDisconnect by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Sync", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Attendance works entirely offline. Connecting a workspace also keeps several devices in step.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    connection?.let { "Workspace: ${it.workspaceName}" } ?: "This device only",
                    style = MaterialTheme.typography.titleMedium,
                )
                lastSyncedAt?.let {
                    Text(
                        "Last synced $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (connection == null) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connect to a server", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "The address of your Attendance Monitoring deployment. HTTPS only — a token " +
                            "must never travel over plain HTTP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("Server URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = workspaceName,
                        onValueChange = { workspaceName = it.take(80) },
                        label = { Text("New workspace name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        enabled = !busy && workspaceName.isNotBlank() && serverUrl.startsWith("https://"),
                        onClick = {
                            busy = true
                            viewModel.launch {
                                runCatching {
                                    val url = serverUrl.trim().trimEnd('/')
                                    // Saved first so the API client, which reads the address from
                                    // settings, can reach the right server on the very first call.
                                    viewModel.settings.saveConnection(
                                        SyncConnection(url, "", workspaceName, "pending", null),
                                    )
                                    val response = viewModel.api.createWorkspace(workspaceName)
                                    viewModel.settings.saveConnection(
                                        SyncConnection(
                                            serverUrl = url,
                                            workspaceId = response.workspaceId,
                                            workspaceName = response.name,
                                            token = response.token,
                                            joinCode = response.joinCode,
                                        ),
                                    )
                                    report(viewModel, viewModel.syncEngine.syncNow())
                                }.onFailure {
                                    viewModel.settings.clearConnection()
                                    viewModel.say(it.message ?: "Could not create that workspace.")
                                }
                                busy = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Create workspace") }

                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = { joinCode = it.take(32) },
                        label = { Text("Or join with a code") },
                        placeholder = { Text("ABCD-EFGH-JKLM") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        enabled = !busy && joinCode.trim().length >= 8 && serverUrl.startsWith("https://"),
                        onClick = {
                            busy = true
                            viewModel.launch {
                                runCatching {
                                    val url = serverUrl.trim().trimEnd('/')
                                    viewModel.settings.saveConnection(
                                        SyncConnection(url, "", "", "pending", null),
                                    )
                                    val response = viewModel.api.joinWorkspace(joinCode.trim())
                                    viewModel.settings.saveConnection(
                                        SyncConnection(
                                            serverUrl = url,
                                            workspaceId = response.workspaceId,
                                            workspaceName = response.name,
                                            token = response.token,
                                            joinCode = null,
                                        ),
                                    )
                                    report(viewModel, viewModel.syncEngine.syncNow())
                                }.onFailure {
                                    viewModel.settings.clearConnection()
                                    viewModel.say(it.message ?: "Could not join that workspace.")
                                }
                                busy = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Join workspace") }
                }
            }
        } else {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connected", style = MaterialTheme.typography.titleMedium)
                    Text(
                        connection!!.serverUrl,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = !busy,
                            onClick = {
                                busy = true
                                viewModel.launch {
                                    report(viewModel, viewModel.syncEngine.syncNow())
                                    busy = false
                                }
                            },
                        ) { Text(if (busy) "Syncing…" else "Sync now") }
                        OutlinedButton(enabled = !busy, onClick = { confirmDisconnect = true }) {
                            Text("Disconnect")
                        }
                    }
                }
            }

            connection!!.joinCode?.let { code ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Join code", style = MaterialTheme.typography.titleMedium)
                        Text(
                            code,
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            "Enter this on another device to join the same workspace. Anyone with the " +
                                "code gets full access, so share it the way you would a key.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("How it behaves offline", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Scanning, editing, and reporting all read and write this device's own database, " +
                        "so they work with no network at all. Sync runs in the background and simply " +
                        "has nothing to do until a connection returns.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "When two devices change the same section, student, or schedule while both are " +
                        "offline, the most recent edit wins once they reconnect. Attendance records " +
                        "never conflict: they are only added, and one student holds one record per " +
                        "schedule per day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (confirmDisconnect) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text("Disconnect this device?") },
            text = {
                Text(
                    "It stops sending and receiving changes. Every section, student, schedule, and " +
                        "attendance record already on this device is kept.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.launch {
                        viewModel.settings.clearConnection()
                        viewModel.say("Disconnected. Everything on this device was kept.")
                    }
                    confirmDisconnect = false
                }) { Text("Disconnect") }
            },
            dismissButton = { TextButton(onClick = { confirmDisconnect = false }) { Text("Cancel") } },
        )
    }
}

/**
 * A first sync that fails is not a detail to swallow: connecting and then silently sending
 * nothing looks exactly like working, and the operator only finds out when the second device
 * shows an empty roster.
 */
private fun report(viewModel: AppViewModel, result: SyncEngine.Result) {
    when (result) {
        is SyncEngine.Result.Success ->
            viewModel.say("Sent ${result.outcome.pushed}, received ${result.outcome.pulled}.")
        is SyncEngine.Result.Failure ->
            viewModel.say(result.failure.message)
    }
}
