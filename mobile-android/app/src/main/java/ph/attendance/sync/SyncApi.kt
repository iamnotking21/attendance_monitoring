package ph.attendance.sync

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Why the request failed matters more than that it did: "no network" and "this token was revoked"
 * call for completely different responses from the app.
 */
sealed interface SyncFailure {
    /** The device is offline, or the request never reached the server. */
    data class Offline(override val message: String) : SyncFailure

    /** The deployment has no database configured, so it is local-only. */
    data class Unavailable(override val message: String) : SyncFailure

    /** The token is no longer valid; this device must reconnect. */
    data class Unauthorized(override val message: String) : SyncFailure

    data class Rejected(override val message: String) : SyncFailure

    val message: String
}

class SyncException(val failure: SyncFailure) : Exception(failure.message)

/**
 * Talks to the same endpoints the web client uses.
 *
 * Cleartext is refused: the app's network security config allows HTTPS only, so a token cannot be
 * sent over a plain-HTTP LAN address by accident.
 */
class SyncApi(private val baseUrlProvider: () -> String) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun isConfigured(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url("/api/sync/status"))
                .header("Cache-Control", "no-store")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching false
                val body = response.body?.string() ?: return@runCatching false
                json.decodeFromString<SyncStatusResponse>(body).configured
            }
        }.getOrDefault(false)
    }

    suspend fun createWorkspace(name: String): WorkspaceResponse =
        post("/api/workspace", json.encodeToString(CreateWorkspaceRequest(name)), token = null)

    suspend fun joinWorkspace(joinCode: String): WorkspaceResponse =
        post("/api/workspace/join", json.encodeToString(JoinWorkspaceRequest(joinCode)), token = null)

    suspend fun pull(token: String, since: Long, limit: Int = 500): PullResponse =
        post("/api/sync/pull", json.encodeToString(PullRequest(since, limit)), token)

    suspend fun push(token: String, changes: ChangeSet): PushResponse =
        post("/api/sync/push", json.encodeToString(PushRequest(changes)), token)

    private suspend inline fun <reified T> post(path: String, body: String, token: String?): T =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder()
                .url(url(path))
                .header("Content-Type", "application/json")
                .header("Cache-Control", "no-store")
                .post(body.toRequestBody(jsonMedia))

            if (token != null) builder.header("Authorization", "Bearer $token")

            val response = try {
                client.newCall(builder.build()).execute()
            } catch (error: IOException) {
                throw SyncException(
                    SyncFailure.Offline("Could not reach the server. Your work is saved on this device."),
                )
            }

            response.use {
                val payload = it.body?.string().orEmpty()

                if (it.isSuccessful) {
                    return@withContext json.decodeFromString<T>(payload)
                }

                val message = runCatching { json.decodeFromString<ApiError>(payload).error }
                    .getOrDefault("Sync failed.")

                throw SyncException(
                    when (it.code) {
                        401 -> SyncFailure.Unauthorized(message)
                        503 -> SyncFailure.Unavailable(message)
                        else -> SyncFailure.Rejected(message)
                    },
                )
            }
        }

    private fun url(path: String): String = baseUrlProvider().trimEnd('/') + path
}
