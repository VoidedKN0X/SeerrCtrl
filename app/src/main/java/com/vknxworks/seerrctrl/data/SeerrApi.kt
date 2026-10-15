package com.vknxworks.seerrctrl.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class ApiException(val code: Int, message: String) : Exception(message)

/**
 * Thin client for Seerr's /api/v1. Responses come back as org.json values so the app keeps working
 * when Seerr adds fields. Auth is Seerr's own session cookie (connect.sid), the same one the web UI uses.
 */
class SeerrApi(private val prefs: Prefs) {

    @Volatile var baseUrl: String = prefs.baseUrl.trimEnd('/')
        private set
    @Volatile private var cookie: String = prefs.sessionCookie
    @Volatile private var insecure: Boolean = prefs.allowInsecureTls
    /** While setting up, the cookie is only kept in memory until sign-in succeeds. */
    @Volatile private var persistCookie: Boolean = true

    /** Flips to true when the server rejects our session, so the UI can send the user to sign in. */
    var sessionExpired by mutableStateOf(false)

    @Volatile private var clientCache: Pair<Boolean, OkHttpClient>? = null

    val client: OkHttpClient
        get() {
            clientCache?.let { if (it.first == insecure) return it.second }
            val c = buildClient(insecure)
            clientCache = insecure to c
            return c
        }

    /** Loads the saved server and session (after sign-in, log out, or app start). */
    fun reload() {
        baseUrl = prefs.baseUrl.trimEnd('/')
        cookie = prefs.sessionCookie
        insecure = prefs.allowInsecureTls
        persistCookie = true
        clientCache = null
        sessionExpired = false
    }

    /** Points the client at a server that isn't saved yet (setup screen). */
    fun useCandidate(base: String, allowInsecure: Boolean) {
        baseUrl = base.trimEnd('/')
        insecure = allowInsecure
        cookie = ""
        persistCookie = false
        clientCache = null
    }

    /** Saves the candidate server and the session it just got. */
    fun commitCandidate() {
        prefs.baseUrl = baseUrl
        prefs.allowInsecureTls = insecure
        prefs.sessionCookie = cookie
        persistCookie = true
        sessionExpired = false
    }

    fun abs(path: String): String = if (path.startsWith("http")) path else baseUrl + (if (path.startsWith("/")) path else "/$path")

    // ---------- requests ----------

    suspend fun get(path: String, query: Map<String, Any?> = emptyMap()): Any? = call("GET", path, query, null)
    suspend fun obj(path: String, query: Map<String, Any?> = emptyMap()): JSONObject = get(path, query) as? JSONObject ?: JSONObject()
    suspend fun arr(path: String, query: Map<String, Any?> = emptyMap()): JSONArray = get(path, query) as? JSONArray ?: JSONArray()
    suspend fun post(path: String, body: Any? = JSONObject()): Any? = call("POST", path, emptyMap(), body)
    suspend fun put(path: String, body: Any? = JSONObject()): Any? = call("PUT", path, emptyMap(), body)
    suspend fun delete(path: String, body: Any? = null, query: Map<String, Any?> = emptyMap()): Any? = call("DELETE", path, query, body)

    private suspend fun call(method: String, path: String, query: Map<String, Any?>, body: Any?): Any? =
        withContext(Dispatchers.IO) {
            val url = "$baseUrl/api/v1$path".toHttpUrl().newBuilder().apply {
                query.forEach { (k, v) -> if (v != null && v.toString().isNotEmpty()) addQueryParameter(k, v.toString()) }
            }.build()
            val rb = when {
                body != null -> body.toString().toRequestBody(JSON)
                method == "GET" || method == "DELETE" -> null
                else -> "{}".toRequestBody(JSON)
            }
            val req = Request.Builder().url(url).method(method, rb).header("Accept", "application/json").build()
            client.newCall(req).execute().use { r ->
                val text = r.body?.string().orEmpty()
                if (!r.isSuccessful) {
                    if (r.code == 401 && persistCookie && cookie.isNotEmpty() && !path.startsWith("/auth/")) {
                        sessionExpired = true
                    }
                    throw ApiException(r.code, errorMessage(r.code, text))
                }
                parse(text)
            }
        }

    /** Raw GET against any URL (used for plex.tv during Plex sign-in). */
    suspend fun external(verb: String, url: String, headers: Map<String, String>, body: String? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val req = Request.Builder().url(url).apply {
                headers.forEach { (k, v) -> header(k, v) }
                header("Accept", "application/json")
                method(verb, body?.toRequestBody(FORM) ?: if (verb == "POST") "".toRequestBody(FORM) else null)
            }.build()
            plainClient.newCall(req).execute().use { r ->
                val text = r.body?.string().orEmpty()
                if (!r.isSuccessful) throw ApiException(r.code, "Plex answered HTTP ${r.code}")
                parse(text) as? JSONObject ?: JSONObject()
            }
        }

    private val plainClient by lazy {
        OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
    }

    private fun parse(text: String): Any? {
        val t = text.trim()
        if (t.isEmpty()) return null
        return try { JSONTokener(t).nextValue() } catch (_: Exception) { t }
    }

    private fun errorMessage(code: Int, text: String): String {
        val msg = try {
            val j = JSONObject(text)
            j.optString("message").ifBlank { j.optString("error") }.ifBlank { null }
        } catch (_: Exception) { null }
        val friendly = when (msg) {
            "INVALID_CREDENTIALS" -> "Wrong username or password"
            "INVALID_AUTH_TOKEN" -> "The sign-in token was rejected"
            "INVALID_EMAIL" -> "That email address isn't valid"
            "INVALID_URL" -> "The media server address is invalid"
            "NOT_ADMIN" -> "Only an admin can do that"
            "NO_ADMIN_USER" -> "This server has no admin user yet"
            "CONNECTION_ERROR" -> "Seerr couldn't reach the media server"
            "UNAUTHORIZED", "Unauthorized" -> "You don't have access to this server"
            "UNKNOWN" -> "Something went wrong on the server"
            "SYNC_ERROR_GROUPED_FOLDERS" -> "Library sync failed: grouped folders aren't supported"
            "SYNC_ERROR_NO_LIBRARIES" -> "Library sync found no libraries"
            else -> msg
        }
        return friendly ?: when (code) {
            401 -> "Not signed in"
            403 -> "You don't have permission to do that"
            404 -> "Not found"
            else -> "Server answered HTTP $code"
        }
    }

    private fun buildClient(insecure: Boolean): OkHttpClient {
        val b = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val orig = chain.request()
                val sameServer = baseUrl.isNotEmpty() && orig.url.toString().startsWith(baseUrl)
                val req = if (sameServer && cookie.isNotEmpty()) {
                    orig.newBuilder().header("Cookie", "$COOKIE=$cookie").build()
                } else orig
                val resp = chain.proceed(req)
                if (sameServer) {
                    resp.headers("Set-Cookie").firstOrNull { it.startsWith("$COOKIE=") }?.let { sc ->
                        val v = sc.substringAfter("=").substringBefore(";")
                        if (v.isNotEmpty() && v != cookie) {
                            cookie = v
                            if (persistCookie) prefs.sessionCookie = v
                        }
                    }
                }
                resp
            }
        if (insecure) {
            val trustAll = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
            val ctx = SSLContext.getInstance("TLS").apply { init(null, arrayOf<TrustManager>(trustAll), SecureRandom()) }
            b.sslSocketFactory(ctx.socketFactory, trustAll).hostnameVerifier { _, _ -> true }
        }
        return b.build()
    }

    companion object {
        private const val COOKIE = "connect.sid"
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private val FORM = "application/x-www-form-urlencoded".toMediaType()

        /** Turns "seerr.home.lan:5055" into "http://seerr.home.lan:5055"; https is assumed for bare domains. */
        fun normalize(input: String): String {
            var s = input.trim().trimEnd('/')
            if (s.isEmpty()) return s
            if (!s.startsWith("http://") && !s.startsWith("https://")) {
                val host = s.substringBefore('/').substringBefore(':')
                val local = host.matches(Regex("""\d+\.\d+\.\d+\.\d+""")) || !host.contains('.') ||
                    host.endsWith(".lan") || host.endsWith(".local") || host.endsWith(".home") || s.contains(':')
                s = (if (local) "http://" else "https://") + s
            }
            return s
        }
    }
}
