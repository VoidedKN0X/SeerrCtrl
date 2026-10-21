package com.vknxworks.seerrctrl.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.data.MediaServer
import com.vknxworks.seerrctrl.data.SeerrApi
import com.vknxworks.seerrctrl.data.bool
import com.vknxworks.seerrctrl.data.int
import com.vknxworks.seerrctrl.data.intOrNull
import com.vknxworks.seerrctrl.data.jsonOf
import com.vknxworks.seerrctrl.data.long
import com.vknxworks.seerrctrl.data.str
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URLEncoder
import androidx.compose.ui.res.stringResource
import com.vknxworks.seerrctrl.R

private enum class Method(val label: String) { MediaServer(App.str(R.string.media_server)), Plex(App.str(R.string.plex)), Local(App.str(R.string.seerr_account)) }

/**
 * First start (and re-login): server address, then whichever sign-in methods the server has turned on —
 * Plex, Jellyfin/Emby (with Quick Connect), and Seerr's own accounts.
 */
@Composable
fun SetupScreen(onDone: () -> Unit, onCancel: (() -> Unit)?) {
    // Passwords and Quick Connect codes are typed here: never in screenshots, recordings or casting.
    com.vknxworks.seerrctrl.SecureWindow()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var url by remember { mutableStateOf(App.prefs.baseUrl) }
    var insecure by remember { mutableStateOf(App.prefs.allowInsecureTls) }
    var server by remember { mutableStateOf<JSONObject?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Trust prompts: a certificate to pin, or an http address to confirm.
    var certPrompt by remember { mutableStateOf<Pair<String, String>?>(null) }
    var httpPrompt by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    suspend fun finish(user: Any?) {
        App.api.commitCandidate()
        (user as? JSONObject)?.let { App.session.updateMe(it) }
        try { App.session.refresh() } catch (_: Exception) {}
        onDone()
    }

    fun host(base: String) = base.substringAfter("://").substringBefore('/')

    /** Talks to [base] with the given trust settings. Returns the failure so the caller can offer a fix. */
    suspend fun probe(base: String, pin: String, allowHttp: Boolean): Exception? {
        App.api.useCandidate(base, pin, allowHttp)
        return try {
            val pub = App.api.obj("/settings/public")
            if (!pub.has("applicationTitle") && !pub.has("initialized")) {
                error = App.str(R.string.that_doesn_t_look_like_a_seerr)
            } else if (!pub.bool("initialized", true)) {
                error = App.str(R.string.this_server_hasn_t_been_set_up_yet)
            } else server = pub
            null
        } catch (e: Exception) { e }
    }

    fun connectWith(base: String, pin: String, allowHttp: Boolean) {
        scope.launch {
            busy = true; error = null
            val e = probe(base, pin, allowHttp)
            if (e != null) {
                error = App.str(R.string.couldn_t_reach_the_server, e.message ?: e.javaClass.simpleName)
            }
            busy = false
        }
    }

    fun connect() {
        val explicit = SeerrApi.hasScheme(url)
        val base = SeerrApi.normalize(url)
        if (base.isEmpty()) { error = App.str(R.string.enter_your_seerr_address); return }
        url = base
        if (base.startsWith("http://")) {
            // The user typed http:// themselves: still say what that means before anything is sent.
            httpPrompt = base to false
            return
        }
        // Keep the certificate already trusted for this exact server (signing in again).
        val savedPin = if (base == App.prefs.baseUrl.trimEnd('/')) App.prefs.pinnedCert else ""
        scope.launch {
            busy = true; error = null
            val e = probe(base, savedPin, false)
            when {
                e == null -> Unit
                isCertError(e) && insecure -> {
                    try {
                        val fp = com.vknxworks.seerrctrl.data.Tls.sha256(com.vknxworks.seerrctrl.data.Tls.fetchLeaf(base))
                        certPrompt = base to fp
                    } catch (x: Exception) {
                        error = App.str(R.string.couldn_t_read_the_server_s, x.message ?: x.javaClass.simpleName)
                    }
                }
                isCertError(e) -> error = App.str(R.string.secure_connection_failed_if_the, e.message)
                !explicit && isNoHttps(e) -> httpPrompt = SeerrApi.toHttp(base) to true
                else -> error = App.str(R.string.couldn_t_reach_the_server, e.message ?: e.javaClass.simpleName)
            }
            busy = false
        }
    }

    certPrompt?.let { (base, fp) ->
        CertDialog(
            host(base), fp, changed = false,
            onTrust = { certPrompt = null; connectWith(base, fp, false) },
            onCancel = { certPrompt = null; error = App.str(R.string.certificate_not_trusted) },
        )
    }
    httpPrompt?.let { (base, fallback) ->
        HttpWarningDialog(
            host(base), fallback,
            onContinue = { httpPrompt = null; url = base; connectWith(base, "", true) },
            onCancel = { httpPrompt = null },
        )
    }

    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth()) {
                when {
                    server != null -> IconButton({ server = null; error = null }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) }
                    onCancel != null -> IconButton(onCancel) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.app_name), fontSize = 34.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(R.string.request_movies_and_series_from_your),
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(32.dp))

            AnimatedContent(server, label = "setup") { pub ->
                Column(Modifier.widthIn(max = 480.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (pub == null) {
                        OutlinedTextField(
                            value = url, onValueChange = { url = it; error = null },
                            label = { Text(stringResource(R.string.server_address)) },
                            placeholder = { Text("https://requests.example.com") },
                            leadingIcon = { Icon(Icons.Outlined.Dns, null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { connect() }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(insecure, { insecure = it })
                            Text(stringResource(R.string.allow_self_signed_certificate_you_ll), style = MaterialTheme.typography.bodyMedium)
                        }
                        ErrorText(error)
                        Button(::connect, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Text(stringResource(R.string.continue))
                        }
                        Text(
                            stringResource(R.string.works_with_seerr_jellyseerr_and),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        SignIn(pub, onSignedIn = { user -> scope.launch { finish(user) } }, openUrl = { openUrl(context, it) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorText(error: String?) {
    if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun SignIn(pub: JSONObject, onSignedIn: (Any?) -> Unit, openUrl: (String) -> Unit) {
    val type = MediaServer.of(pub.intOrNull("mediaServerType"))
    val msLogin = pub.bool("mediaServerLogin", true)
    val methods = buildList {
        if (msLogin && type == MediaServer.Plex) add(Method.Plex)
        if (msLogin && (type == MediaServer.Jellyfin || type == MediaServer.Emby)) add(Method.MediaServer)
        if (pub.bool("localLogin", true)) add(Method.Local)
        if (isEmpty()) add(Method.Local)
    }
    var tab by remember { mutableIntStateOf(0) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.sign_in_to, pub.str("applicationTitle") ?: "Seerr"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(App.api.baseUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (methods.size > 1) {
                PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    methods.forEachIndexed { i, m ->
                        val label = if (m == Method.MediaServer) type.label else m.label
                        Tab(i == tab, { tab = i }, text = { Text(label, maxLines = 1) })
                    }
                }
            }
            when (methods[tab.coerceIn(0, methods.lastIndex)]) {
                Method.Plex -> PlexSignIn(onSignedIn, openUrl)
                Method.MediaServer -> JellyfinSignIn(type, pub, onSignedIn, openUrl)
                Method.Local -> LocalSignIn(pub, onSignedIn)
            }
        }
    }
}

@Composable
private fun PasswordField(value: String, label: String, onChange: (String) -> Unit, onGo: () -> Unit) {
    var show by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        leadingIcon = { Icon(Icons.Outlined.Key, null) },
        trailingIcon = {
            IconButton({ show = !show }) { Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, stringResource(R.string.show_password)) }
        },
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LocalSignIn(pub: JSONObject, onSignedIn: (Any?) -> Unit) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    fun go() = scope.launch {
        busy = true; error = null
        try { onSignedIn(App.api.post("/auth/local", jsonOf("email" to email.trim(), "password" to pass))) }
        catch (e: Exception) { error = e.message }
        busy = false
    }

    OutlinedTextField(
        value = email, onValueChange = { email = it }, label = { Text(stringResource(R.string.email)) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
    PasswordField(pass, stringResource(R.string.password), { pass = it }) { go() }
    ErrorText(error)
    info?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
    Button({ go() }, enabled = !busy && email.isNotBlank() && pass.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(50.dp)) {
        if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        else { Icon(Icons.AutoMirrored.Outlined.Login, null); Spacer(Modifier.size(8.dp)); Text(stringResource(R.string.sign_in)) }
    }
    if (pub.bool("emailEnabled")) {
        TextButton(
            onClick = {
                scope.launch {
                    if (email.isBlank()) { error = App.str(R.string.enter_your_email_first); return@launch }
                    try {
                        App.api.post("/auth/reset-password", jsonOf("email" to email.trim()))
                        info = App.str(R.string.if_that_account_exists_a_reset_link)
                    } catch (e: Exception) { error = e.message }
                }
            },
        ) { Text(stringResource(R.string.forgot_password)) }
    }
}

@Composable
private fun JellyfinSignIn(type: MediaServer, pub: JSONObject, onSignedIn: (Any?) -> Unit, openUrl: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var quickCode by remember { mutableStateOf<String?>(null) }
    var quickJob by remember { mutableStateOf<Job?>(null) }
    DisposableEffect(Unit) { onDispose { quickJob?.cancel() } }

    fun go() = scope.launch {
        busy = true; error = null
        try {
            val body = jsonOf("username" to user.trim(), "password" to pass, "email" to user.trim())
            onSignedIn(App.api.post("/auth/jellyfin", body))
        } catch (e: Exception) { error = e.message }
        busy = false
    }

    fun quickConnect() {
        quickJob?.cancel()
        quickJob = scope.launch {
            error = null
            try {
                val init = App.api.post("/auth/jellyfin/quickconnect/initiate") as? JSONObject ?: error(App.str(R.string.no_code))
                val secret = init.str("secret") ?: error(App.str(R.string.no_code))
                quickCode = init.str("code")
                val deadline = System.currentTimeMillis() + 5 * 60_000
                while (System.currentTimeMillis() < deadline) {
                    delay(2000)
                    val chk = App.api.obj("/auth/jellyfin/quickconnect/check", mapOf("secret" to secret))
                    if (chk.bool("authenticated")) {
                        onSignedIn(App.api.post("/auth/jellyfin/quickconnect/authenticate", jsonOf("secret" to secret)))
                        return@launch
                    }
                }
                error = App.str(R.string.the_quick_connect_code_expired_try)
                quickCode = null
            } catch (e: Exception) {
                error = e.message; quickCode = null
            }
        }
    }

    if (quickCode != null) {
        Text(stringResource(R.string.quick_connect), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            stringResource(R.string.in_jellyfin_on_a_signed_in_device),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            quickCode!!, fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.waiting_for_approval), style = MaterialTheme.typography.bodyMedium)
        }
        ErrorText(error)
        OutlinedButton({ quickJob?.cancel(); quickCode = null }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.lock_cancel)) }
        return
    }

    OutlinedTextField(
        value = user, onValueChange = { user = it }, label = { Text(stringResource(R.string.username, type.label)) }, singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), modifier = Modifier.fillMaxWidth(),
    )
    PasswordField(pass, stringResource(R.string.password), { pass = it }) { go() }
    ErrorText(error)
    Button({ go() }, enabled = !busy && user.isNotBlank(), modifier = Modifier.fillMaxWidth().height(50.dp)) {
        if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        else { Icon(Icons.AutoMirrored.Outlined.Login, null); Spacer(Modifier.size(8.dp)); Text(stringResource(R.string.sign_in_with, type.label)) }
    }
    if (type == MediaServer.Jellyfin) {
        FilledTonalButton(::quickConnect, Modifier.fillMaxWidth()) { Text(stringResource(R.string.use_quick_connect)) }
    }
    pub.str("jellyfinForgotPasswordUrl")?.let { link ->
        TextButton({ openUrl(link) }) { Text(stringResource(R.string.forgot_password)) }
    }
}

@Composable
private fun PlexSignIn(onSignedIn: (Any?) -> Unit, openUrl: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    DisposableEffect(Unit) { onDispose { job?.cancel() } }

    val headers = mapOf(
        "X-Plex-Product" to "SeerrCtrl",
        "X-Plex-Version" to "1.0",
        "X-Plex-Client-Identifier" to App.prefs.plexClientId,
        "X-Plex-Platform" to "Android",
        "X-Plex-Device" to android.os.Build.MODEL,
        "X-Plex-Device-Name" to "SeerrCtrl (${android.os.Build.MODEL})",
    )

    fun start() {
        job?.cancel()
        job = scope.launch {
            busy = true; error = null
            try {
                val pin = App.api.external("POST", "https://plex.tv/api/v2/pins?strong=true", headers)
                // Plex PIN ids can be larger than an Int.
                val id = pin.long("id").takeIf { it > 0 } ?: error(App.str(R.string.plex_didn_t_return_a_sign_in_code))
                val code = pin.str("code")?.takeIf { c -> c.all { it.isLetterOrDigit() } } ?: error(App.str(R.string.plex_didn_t_return_a_code))
                fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
                val authUrl = "https://app.plex.tv/auth/#!?clientID=${enc(App.prefs.plexClientId)}&code=${enc(code)}" +
                    "&context%5Bdevice%5D%5Bproduct%5D=SeerrCtrl&context%5Bdevice%5D%5Bplatform%5D=Android"
                openUrl(authUrl)
                val deadline = System.currentTimeMillis() + 10 * 60_000
                while (System.currentTimeMillis() < deadline) {
                    delay(1500)
                    val p = App.api.external("GET", "https://plex.tv/api/v2/pins/$id", headers)
                    val token = p.str("authToken")
                    if (token != null) {
                        onSignedIn(App.api.post("/auth/plex", jsonOf("authToken" to token)))
                        return@launch
                    }
                }
                error = App.str(R.string.plex_sign_in_timed_out_try_again)
            } catch (e: Exception) {
                error = e.message
            }
            busy = false
        }
    }

    Text(
        stringResource(R.string.you_ll_sign_in_on_plex_tv_in_a),
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    ErrorText(error)
    Button(::start, enabled = !busy, modifier = Modifier.fillMaxWidth().height(50.dp)) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.size(10.dp)); Text(stringResource(R.string.waiting_for_plex))
        } else Text(stringResource(R.string.sign_in_with_plex))
    }
    if (busy) OutlinedButton({ job?.cancel(); busy = false }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.lock_cancel)) }
}
