package com.vknxworks.seerrctrl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material.icons.outlined.NoEncryption
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.data.Tls
import kotlinx.coroutines.launch
import java.security.cert.CertificateException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLPeerUnverifiedException

/** True when a failure is about the certificate (untrusted, wrong name, pin changed), not about reaching the server. */
fun isCertError(e: Throwable): Boolean {
    var t: Throwable? = e
    while (t != null) {
        if (t is CertificateException || t is SSLPeerUnverifiedException) return true
        val m = t.message.orEmpty()
        if (m.contains("Trust anchor", ignoreCase = true) || m.contains("certificate", ignoreCase = true)) return true
        t = t.cause
    }
    return false
}

/** True when https didn't work because nothing answered, or the port doesn't speak TLS (a plain http server). */
fun isNoHttps(e: Throwable): Boolean =
    !isCertError(e) && e !is java.net.UnknownHostException && (e is java.io.IOException || e is SSLException)

@Composable
fun CertDialog(host: String, fingerprint: String, changed: Boolean, onTrust: () -> Unit, onCancel: () -> Unit, cancelLabel: String = "Cancel") {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Outlined.GppMaybe, null) },
        title = { Text(if (changed) "Certificate changed" else "Trust this certificate?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (changed) "$host now shows a different certificate than the one you trusted. If you didn't renew it yourself, " +
                        "someone may be intercepting the connection. Only trust it if the fingerprint matches your server's."
                    else "$host uses a certificate your phone doesn't recognise. Check that this SHA-256 fingerprint matches your " +
                        "server's certificate. SeerrCtrl will only accept this exact certificate from now on.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                SelectionContainer {
                    Text(Tls.pretty(fingerprint), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onTrust) { Text("Trust") } },
        dismissButton = { TextButton(onCancel) { Text(cancelLabel) } },
    )
}

@Composable
fun HttpWarningDialog(host: String, fallback: Boolean, onContinue: () -> Unit, onCancel: () -> Unit, cancelLabel: String = "Cancel") {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Outlined.NoEncryption, null) },
        title = { Text("Not encrypted (HTTP)") },
        text = {
            Text(
                (if (fallback) "$host didn't answer over https. " else "") +
                    "Over plain http, your password and session are sent unencrypted, so anyone on the same network can read them. " +
                    "Only continue on a network you trust, or put Seerr behind https.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = { TextButton(onContinue) { Text(if (fallback) "Use http" else "Continue") } },
        dismissButton = { TextButton(onCancel) { Text(cancelLabel) } },
    )
}

/**
 * Prompts for the saved server: confirm an existing http address once, pin the certificate for users who had
 * "Allow self-signed certificate" on before pinning existed, and re-pin after the certificate changed.
 */
@Composable
fun ServerTrustPrompts(onLogout: () -> Unit) {
    val api = App.api
    val scope = rememberCoroutineScope()
    val host = remember(api.baseUrl) { api.baseUrl.substringAfter("://").substringBefore('/') }
    var offered by remember { mutableStateOf<String?>(null) }
    var dismissed by remember { mutableStateOf(false) }
    val needsPin = App.prefs.allowInsecureTls && api.pinnedFingerprint.isEmpty() && !api.isHttp
    var httpAcked by remember { mutableStateOf(App.prefs.httpAcknowledged) }
    val needsHttpOk = api.isHttp && !httpAcked

    LaunchedEffect(needsPin, api.certChanged) {
        if (needsPin || api.certChanged) {
            offered = try { Tls.sha256(Tls.fetchLeaf(api.baseUrl)) } catch (_: Exception) { null }
        }
    }

    when {
        dismissed -> Unit
        needsHttpOk -> HttpWarningDialog(
            host, fallback = false,
            onContinue = {
                api.acknowledgeHttp()
                httpAcked = true
                scope.launch { try { App.session.refresh() } catch (_: Exception) {} }
            },
            onCancel = { dismissed = true; onLogout() }, cancelLabel = "Log out",
        )
        (needsPin || api.certChanged) && offered != null -> CertDialog(
            host, offered!!, changed = api.certChanged,
            onTrust = {
                api.pinSavedServer(offered!!)
                offered = null
                scope.launch { try { App.session.refresh() } catch (_: Exception) {} }
            },
            onCancel = { dismissed = true },
            cancelLabel = "Not now",
        )
    }
}
