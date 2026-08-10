package ph.attendance

import android.content.Context
import kotlinx.coroutines.runBlocking
import ph.attendance.data.AttendanceRepository
import ph.attendance.data.SettingsStore
import ph.attendance.data.local.AttendanceDatabase
import ph.attendance.sync.SyncApi
import ph.attendance.sync.SyncEngine

/**
 * The application's object graph, assembled once and read wherever it is needed.
 *
 * Built by hand rather than with a dependency-injection framework: the graph is a database, a
 * repository, a settings store, and a sync client. A framework would add a compiler plugin and a
 * layer of indirection to solve a problem this app does not have.
 *
 * Everything is `lazy`, so opening the app costs only what it uses — a coordinator who checks
 * today's dashboard never constructs the sync client.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: AttendanceDatabase by lazy { AttendanceDatabase.build(appContext) }

    val repository: AttendanceRepository by lazy { AttendanceRepository(database) }

    val settings: SettingsStore by lazy { SettingsStore(appContext) }

    val api: SyncApi by lazy {
        // The server address is part of the stored connection, so a school can point the app at
        // its own deployment without a rebuild.
        SyncApi { runBlocking { settings.currentConnection()?.serverUrl.orEmpty() } }
    }

    val syncEngine: SyncEngine by lazy { SyncEngine(repository, settings, api) }
}
