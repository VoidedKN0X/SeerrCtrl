package com.vknxworks.seerrctrl.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.R

class ApiException(val code: Int, message: String) : Exception(message)

/**
 * Thin client for Seerr's /api/v1. Responses come back as org.json values so the app keeps working
 * when Seerr adds fields. Auth is Seerr's own session cookie (connect.sid), the same one the web UI uses.
 */
class SeerrApi(private val prefs: Prefs) {

    @Volatile var baseUrl: String = prefs.baseUrl.trimEnd('/')
        private set
    @Volatile private var cookie: String = prefs.sessionCookie
    /** SHA-256 of the self-signed certificate the user trusted for this server ("" = normal validation only). */
    @Volatile private var pin: String = prefs.pinnedCert
    /** Plain http to the server is only used after the user confirmed it's unencrypted. */
    @Volatile private var httpOk: Boolean = prefs.httpAcknowledged
    /** While setting up, the cookie is only kept in memory until sign-in succeeds. */
    @Volatile private var persistCookie: Boolean = true

    /** Flips to true when the server rejects our session, so the UI can send the user to sign in. */
    var sessionExpired by mutableStateOf(false)

    /** Set when the server's certificate no longer matches the one the user trusted, so the UI can offer to re-pin. */
    var certChanged by mutableStateOf(false)

    @Volatile private var clientCache: Pair<String, OkHttpClient>? = null

    /** Client for the user's own Seerr server (session cookie, optional pinned certificate). */
    val client: OkHttpClient
        get() {
            val key = "$baseUrl|$pin"
            clientCache?.let { if (it.first == key) return it.second }
            val c = buildClient(pin)
            clientCache = key to c
            return c
        }

    /** Images: the Seerr server's own URLs use [client]; anything else (TMDB, Plex, Gravatar) uses normal TLS checks and no cookie. */
    val imageCallFactory: Call.Factory = object : Call.Factory {
        override fun newCall(request: Request): Call = (if (sameServer(request.url)) client else otherClient).newCall(request)
    }

    /** Exact scheme + host + port match with the configured server. */
    fun sameServer(url: HttpUrl): Boolean {
        val base = baseUrl.toHttpUrlOrNull() ?: return false
        return url.scheme == base.scheme && url.host.equals(base.host, ignoreCase = true) && url.port == base.port
    }

    val isHttp: Boolean get() = baseUrl.startsWith("http://")
    val pinnedFingerprint: String get() = pin

    /** Loads the saved server and session (after sign-in, log out, or app start). */
    fun reload() {
        baseUrl = prefs.baseUrl.trimEnd('/')
        cookie = prefs.sessionCookie
        pin = prefs.pinnedCert
        httpOk = prefs.httpAcknowledged
        persistCookie = true
        clientCache = null
        sessionExpired = false
        certChanged = false
    }

    /** The app locked: forget the session cookie if the lock sealed it, until [reload] after unlocking. */
    fun lockDown() {
        if (!prefs.cookieSealed) return
        cookie = ""
        clientCache = null
    }

    /** Points the client at a server that isn't saved yet (setup screen). */
    fun useCandidate(base: String, pinned: String, allowHttp: Boolean) {
        baseUrl = base.trimEnd('/')
        pin = pinned
        httpOk = allowHttp
        cookie = ""
        persistCookie = false
        clientCache = null
    }

    /** Saves the candidate server and the session it just got. */
    fun commitCandidate() {
        prefs.baseUrl = baseUrl
        prefs.pinnedCert = pin
        prefs.allowInsecureTls = pin.isNotEmpty()
        prefs.httpAcknowledged = httpOk
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

    /** Trusts [fingerprint] for the saved server from now on (after the user confirmed it). */
    fun pinSavedServer(fingerprint: String) {
        prefs.pinnedCert = fingerprint
        prefs.allowInsecureTls = true
        reload()
    }

    /** The user confirmed the saved http server is unencrypted. */
    fun acknowledgeHttp() {
        prefs.httpAcknowledged = true
        httpOk = true
    }

    /** Raw request to plex.tv during Plex sign-in. https only, also across redirects. */
    suspend fun external(verb: String, url: String, headers: Map<String, String>, body: String? = null): JSONObject =
        withContext(Dispatchers.IO) {
            require(url.startsWith("https://")) { App.str(R.string.error_plex_https) }
            val req = Request.Builder().url(url).apply {
                headers.forEach { (k, v) -> header(k, v) }
                header("Accept", "application/json")
                method(verb, body?.toRequestBody(FORM) ?: if (verb == "POST") "".toRequestBody(FORM) else null)
            }.build()
            plainClient.newCall(req).execute().use { r ->
                val text = r.body?.string().orEmpty()
                if (!r.isSuccessful) throw ApiException(r.code, App.str(R.string.plex_answered_http, r.code))
                parse(text) as? JSONObject ?: JSONObject()
            }
        }

    private val plainClient by lazy {
        OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
            .addNetworkInterceptor { chain ->
                if (!chain.request().url.isHttps) throw IOException(App.str(R.string.refusing_an_unencrypted_connection, chain.request().url.host))
                chain.proceed(chain.request())
            }
            .build()
    }

    /** Third-party hosts: default certificate checks, no session cookie, and no plain http. */
    private val otherClient by lazy {
        OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
            .addNetworkInterceptor { chain ->
                if (!chain.request().url.isHttps) throw IOException(App.str(R.string.refusing_an_unencrypted_connection, chain.request().url.host))
                chain.proceed(chain.request())
            }
            .build()
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
            "INVALID_CREDENTIALS" -> App.str(R.string.wrong_username_or_password)
            "INVALID_AUTH_TOKEN" -> App.str(R.string.the_sign_in_token_was_rejected)
            "INVALID_EMAIL" -> App.str(R.string.that_email_address_isn_t_valid)
            "INVALID_URL" -> App.str(R.string.the_media_server_address_is_invalid)
            "NOT_ADMIN" -> App.str(R.string.only_an_admin_can_do_that)
            "NO_ADMIN_USER" -> App.str(R.string.this_server_has_no_admin_user_yet)
            "CONNECTION_ERROR" -> App.str(R.string.seerr_couldn_t_reach_the_media)
            "UNAUTHORIZED", "Unauthorized" -> App.str(R.string.error_no_access)
            "UNKNOWN" -> App.str(R.string.error_server_unknown)
            "SYNC_ERROR_GROUPED_FOLDERS" -> App.str(R.string.library_sync_failed_grouped_folders)
            "SYNC_ERROR_NO_LIBRARIES" -> App.str(R.string.library_sync_found_no_libraries)
            else -> msg
        }
        return friendly ?: when (code) {
            401 -> App.str(R.string.not_signed_in)
            403 -> App.str(R.string.you_don_t_have_permission_to_do_that)
            404 -> App.str(R.string.not_found)
            else -> App.str(R.string.server_answered_http, code)
        }
    }

    private fun buildClient(pin: String): OkHttpClient {
        // A network interceptor runs for every hop, so redirects to another host never get the cookie,
        // and plain http is only allowed to the configured server once the user has confirmed it.
        val guard = Interceptor { chain ->
            val req0 = chain.request()
            val same = sameServer(req0.url)
            if (!req0.url.isHttps && !(same && httpOk)) {
                throw IOException(App.str(R.string.refusing_an_unencrypted_connection, req0.url.host))
            }
            val req = if (same && cookie.isNotEmpty()) {
                req0.newBuilder().header("Cookie", "$COOKIE=$cookie").build()
            } else req0.newBuilder().removeHeader("Cookie").build()
            val resp = chain.proceed(req)
            if (same) {
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
        val b = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .addNetworkInterceptor(guard)
        val host = baseUrl.toHttpUrlOrNull()?.takeIf { it.isHttps }?.host
        if (pin.isNotEmpty() && host != null) {
            // Only the user's own server ever reaches this client with a pin; third-party hosts use [otherClient].
            val tm = Tls.PinnedTrustManager(pin) { certChanged = true }
            b.sslSocketFactory(Tls.sslContext(tm).socketFactory, tm)
                .hostnameVerifier(Tls.pinnedHostnameVerifier(host, pin))
        }
        return b.build()
    }

    companion object {
        private const val COOKIE = "connect.sid"
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private val FORM = "application/x-www-form-urlencoded".toMediaType()

        fun hasScheme(input: String): Boolean {
            val s = input.trim().lowercase()
            return s.startsWith("http://") || s.startsWith("https://")
        }

        /**
         * Adds https:// when no scheme was typed (also for LAN names and ports). Falling back to http is
         * up to the caller, and only after the user confirms it.
         */
        fun normalize(input: String): String {
            var s = input.trim().trimEnd('/')
            if (s.isEmpty()) return s
            if (!hasScheme(s)) s = "https://$s"
            return s
        }

        fun toHttp(base: String): String = "http://" + base.substringAfter("://")
    }
}
