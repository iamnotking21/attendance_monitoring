package ph.attendance.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import ph.attendance.data.ScanOutcome
import ph.attendance.domain.AttendanceStatus
import ph.attendance.ui.AppViewModel
import ph.attendance.ui.components.StatusChip

/** A badge held to the lens decodes many times a second; one accepted scan per badge is enough. */
private const val REPEAT_SUPPRESSION_MS = 2_500L

private data class FeedEntry(
    val id: Long,
    val name: String,
    val detail: String,
    val status: AttendanceStatus?,
)

@Composable
fun ScannerScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var feed by remember { mutableStateOf(listOf<FeedEntry>()) }
    var manual by remember { mutableStateOf("") }

    val lastScan = remember { mutableStateOf("" to 0L) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCamera = granted
        permissionDenied = !granted
    }

    fun submit(payload: String, bypassSuppression: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!bypassSuppression &&
            payload == lastScan.value.first &&
            now - lastScan.value.second < REPEAT_SUPPRESSION_MS
        ) {
            return
        }
        lastScan.value = payload to now

        viewModel.launch {
            val entry = when (val outcome = viewModel.repository.recordScan(payload)) {
                is ScanOutcome.Recorded -> FeedEntry(
                    id = now,
                    name = outcome.student.displayName,
                    detail = outcome.records.joinToString { it.scheduleTitle },
                    status = outcome.records.firstOrNull()?.status,
                )
                is ScanOutcome.Duplicate -> FeedEntry(now, outcome.student.displayName, "Already recorded today", null)
                is ScanOutcome.Closed -> FeedEntry(now, outcome.student.displayName, "No attendance window is open", null)
                is ScanOutcome.Unknown -> FeedEntry(now, "Unknown student", "${outcome.studentNumber} is not on any roster", null)
                is ScanOutcome.Malformed -> FeedEntry(now, "Unrecognised code", outcome.reason, null)
            }
            feed = (listOf(entry) + feed).take(25)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "Scan",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "Hold a student's QR code to the camera. The open window decides present or late.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .padding(bottom = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (hasCamera) {
                CameraPreview(onBarcode = { submit(it) })
            } else {
                ElevatedCard(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null)
                        Text(
                            if (permissionDenied) {
                                "Camera access was denied. Grant it in system settings, or type student numbers below."
                            } else {
                                "The camera stays off until you start it, and the video never leaves this device."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Start camera")
                        }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = manual,
                onValueChange = { manual = it.take(32) },
                label = { Text("Student number") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    val value = manual.trim()
                    if (value.isNotEmpty()) {
                        // Typing it twice is a deliberate act, so it bypasses repeat suppression.
                        submit(value, bypassSuppression = true)
                        manual = ""
                    }
                },
                enabled = manual.isNotBlank(),
            ) { Text("Record") }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(feed, key = { it.id }) { entry ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically { -it / 2 } + fadeIn(),
                    exit = fadeOut(),
                ) {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    entry.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            entry.status?.let { StatusChip(it) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * CameraX preview with ML Kit barcode analysis.
 *
 * The analyser runs on its own single-thread executor and keeps only the latest frame, so a slow
 * decode drops frames instead of building a backlog the operator would feel as lag.
 */
@Composable
private fun CameraPreview(onBarcode: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember { QrAnalyzer(onBarcode) }

    DisposableEffect(Unit) {
        onDispose {
            analyzer.close()
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { viewContext ->
            val previewView = PreviewView(viewContext).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val providerFuture = ProcessCameraProvider.getInstance(viewContext)
            providerFuture.addListener({
                val provider = providerFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor, analyzer) }

                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                }
            }, ContextCompat.getMainExecutor(viewContext))

            previewView
        },
    )
}
