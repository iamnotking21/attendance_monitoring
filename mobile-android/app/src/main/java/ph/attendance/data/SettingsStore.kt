package ph.attendance.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "attendance_settings")

/**
 * Device-local bookkeeping: which workspace this phone belongs to, and how far through the
 * server's change log it has read. Never replicated — this is about *this* device.
 */
data class SyncConnection(
    val serverUrl: String,
    val workspaceId: String,
    val workspaceName: String,
    /** Bearer token for this device. The server stores only a hash of it. */
    val token: String,
    /** Shown once after creating a workspace, so another device can join. */
    val joinCode: String?,
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val serverUrl = stringPreferencesKey("server_url")
        val workspaceId = stringPreferencesKey("workspace_id")
        val workspaceName = stringPreferencesKey("workspace_name")
        val token = stringPreferencesKey("token")
        val joinCode = stringPreferencesKey("join_code")
        val cursor = longPreferencesKey("cursor")
        val pushWatermark = stringPreferencesKey("push_watermark")
        val lastSyncedAt = stringPreferencesKey("last_synced_at")
    }

    val connection: Flow<SyncConnection?> = context.dataStore.data.map(::readConnection)

    val lastSyncedAt: Flow<String?> =
        context.dataStore.data.map { it[Keys.lastSyncedAt] }

    private fun readConnection(prefs: Preferences): SyncConnection? {
        val token = prefs[Keys.token] ?: return null
        val url = prefs[Keys.serverUrl] ?: return null
        return SyncConnection(
            serverUrl = url,
            workspaceId = prefs[Keys.workspaceId].orEmpty(),
            workspaceName = prefs[Keys.workspaceName].orEmpty(),
            token = token,
            joinCode = prefs[Keys.joinCode],
        )
    }

    suspend fun currentConnection(): SyncConnection? = readConnection(context.dataStore.data.first())

    suspend fun saveConnection(connection: SyncConnection) {
        context.dataStore.edit { prefs ->
            prefs[Keys.serverUrl] = connection.serverUrl
            prefs[Keys.workspaceId] = connection.workspaceId
            prefs[Keys.workspaceName] = connection.workspaceName
            prefs[Keys.token] = connection.token
            connection.joinCode?.let { prefs[Keys.joinCode] = it }
        }
    }

    /**
     * Forgets the workspace and keeps every local record.
     *
     * Disconnecting is not a delete. The attendance already taken on this device is the school's
     * data, and losing it because someone tapped the wrong button would be indefensible.
     */
    suspend fun clearConnection() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.serverUrl)
            prefs.remove(Keys.workspaceId)
            prefs.remove(Keys.workspaceName)
            prefs.remove(Keys.token)
            prefs.remove(Keys.joinCode)
            prefs.remove(Keys.cursor)
            prefs.remove(Keys.pushWatermark)
            prefs.remove(Keys.lastSyncedAt)
        }
    }

    suspend fun cursor(): Long = context.dataStore.data.first()[Keys.cursor] ?: 0L

    suspend fun saveCursor(value: Long) {
        context.dataStore.edit { it[Keys.cursor] = value }
    }

    /**
     * Everything written after this instant still needs sending. A watermark rather than a dirty
     * flag on every row: entities already carry `updatedAt`, so "what changed" is a query rather
     * than bookkeeping that can drift out of step with the data it describes.
     */
    suspend fun pushWatermark(): String =
        context.dataStore.data.first()[Keys.pushWatermark] ?: "1970-01-01T00:00:00Z"

    suspend fun savePushWatermark(value: String) {
        context.dataStore.edit { it[Keys.pushWatermark] = value }
    }

    suspend fun saveLastSyncedAt(value: String) {
        context.dataStore.edit { it[Keys.lastSyncedAt] = value }
    }
}
