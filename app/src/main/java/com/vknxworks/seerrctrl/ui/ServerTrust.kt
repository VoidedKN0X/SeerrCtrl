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
import androidx.compose.ui.res.stringResource
import com.vknxworks.seerrctrl.R

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
fun CertDialog(host: String, fingerprint: String, changed: Boolean, onTrust: () -> Unit, onCancel: () -> Unit, cancelLabel: String = stringResource(R.string.lock_cancel)) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Outlined.GppMaybe, null) },
        title = { Text(if (changed) stringResource(R.string.certificate_changed) else stringResource(R.string.trust_this_certificate)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (changed) stringResource(R.string.cert_changed_body, host)
                    else stringResource(R.string.cert_new_body, host),
                    style = MaterialTheme.typography.bodyMedium,
                )
                SelectionContainer {
                    Text(Tls.pretty(fingerprint), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onTrust) { Text(stringResource(R.string.trust)) } },
        dismissButton = { TextButton(onCancel) { Text(cancelLabel) } },
    )
}

@Composable
fun HttpWarningDialog(host: String, fallback: Boolean, onContinue: () -> Unit, onCancel: () -> Unit, cancelLabel: String = stringResource(R.string.lock_cancel)) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Outlined.NoEncryption, null) },
        title = { Text(stringResource(R.string.not_encrypted)) },
        text = {
            Text(
                if (fallback) stringResource(R.string.http_warning_fallback, host)
                else stringResource(R.string.http_warning),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = { TextButton(onContinue) { Text(if (fallback) stringResource(R.string.use_http) else stringResource(R.string.continue)) } },
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
            onCancel = { dismissed = true; onLogout() }, cancelLabel = stringResource(R.string.log_out),
        )
        (needsPin || api.certChanged) && offered != null -> CertDialog(
            host, offered!!, changed = api.certChanged,
            onTrust = {
                api.pinSavedServer(offered!!)
                offered = null
                scope.launch { try { App.session.refresh() } catch (_: Exception) {} }
            },
            onCancel = { dismissed = true },
            cancelLabel = stringResource(R.string.not_now),
        )
    }
}
