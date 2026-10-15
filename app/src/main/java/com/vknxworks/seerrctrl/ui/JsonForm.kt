package com.vknxworks.seerrctrl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vknxworks.seerrctrl.data.deepCopy
import com.vknxworks.seerrctrl.data.humanize
import org.json.JSONArray
import org.json.JSONObject

/** How to show the fields of a settings object. Keys can be dotted paths ("options.webhookUrl"). */
data class FormSpec(
    val labels: Map<String, String> = emptyMap(),
    val help: Map<String, String> = emptyMap(),
    val hide: Set<String> = emptySet(),
    val readOnly: Set<String> = emptySet(),
    /** Fields whose value can be null: "int", "string" or "bool". */
    val types: Map<String, String> = emptyMap(),
    val choices: Map<String, List<Pair<String, String>>> = emptyMap(),
    val multiline: Set<String> = emptySet(),
    /** Fields listed here come first, in this order. */
    val order: List<String> = emptyList(),
)

/** An editable copy of a JSON object. Edits bump [version] so Compose redraws. */
class FormState(initial: JSONObject) {
    var obj: JSONObject = initial.deepCopy()
        private set
    var version by mutableIntStateOf(0)
        private set
    var dirty by mutableStateOf(false)
        private set

    fun reset(o: JSONObject) { obj = o.deepCopy(); version++; dirty = false }

    fun get(path: String): Any? {
        var cur: Any? = obj
        for (p in path.split('.')) cur = (cur as? JSONObject)?.opt(p)
        return if (cur == JSONObject.NULL) null else cur
    }

    fun set(path: String, value: Any?) {
        val parts = path.split('.')
        var cur = obj
        for (p in parts.dropLast(1)) {
            cur = cur.optJSONObject(p) ?: JSONObject().also { cur.put(p, it) }
        }
        cur.put(parts.last(), value ?: JSONObject.NULL)
        version++; dirty = true
    }

    fun saved() { dirty = false }
}

private val SECRET = Regex("(?i)(password|apikey|api_key|token|secret|key$|accesstoken|webhookurl|authheader)")

/** Renders every field of [state] (or of the nested object at [prefix]) as inputs. */
@Composable
fun JsonFields(state: FormState, spec: FormSpec = FormSpec(), prefix: String = "") {
    state.version // subscribe
    val o = (if (prefix.isEmpty()) state.obj else state.get(prefix) as? JSONObject) ?: return
    val keys = o.keys().asSequence().toList()
    val ordered = spec.order.mapNotNull { k -> k.removePrefix(if (prefix.isEmpty()) "" else "$prefix.").takeIf { it in keys && !it.contains('.') } } +
        keys.filter { k -> (if (prefix.isEmpty()) k else "$prefix.$k") !in spec.order }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ordered.forEach { k ->
            val path = if (prefix.isEmpty()) k else "$prefix.$k"
            if (path in spec.hide || k in spec.hide) return@forEach
            JsonField(state, spec, path, k, o.opt(k))
        }
    }
}

@Composable
private fun JsonField(state: FormState, spec: FormSpec, path: String, key: String, raw: Any?) {
    val label = spec.labels[path] ?: spec.labels[key] ?: humanize(key)
    val help = spec.help[path] ?: spec.help[key]
    val ro = path in spec.readOnly || key in spec.readOnly
    val forced = spec.types[path] ?: spec.types[key]
    val value = if (raw == JSONObject.NULL) null else raw
    val choices = spec.choices[path] ?: spec.choices[key]

    when {
        choices != null -> {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Dropdown(label, choices, value?.toString() ?: "", enabled = !ro) { v ->
                    val asInt = (value is Number || forced == "int") && v.toIntOrNull() != null
                    state.set(path, if (asInt) v.toInt() else v)
                }
                help?.let { HelpText(it) }
            }
        }
        value is Boolean || forced == "bool" -> {
            val b = value as? Boolean ?: false
            ListItem(
                headlineContent = { Text(label) },
                supportingContent = help?.let { { Text(it) } },
                trailingContent = { Switch(b, { state.set(path, it) }, enabled = !ro) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
        value is JSONObject -> {
            Text(
                label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp),
            )
            JsonFields(state, spec, path)
        }
        value is JSONArray -> {
            val objects = (0 until value.length()).any { value.opt(it) is JSONObject || value.opt(it) is JSONArray }
            if (objects) {
                ListItem(
                    headlineContent = { Text(label) },
                    supportingContent = { Text("${value.length()} entries (edit in the web UI)") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            } else {
                val numeric = value.length() > 0 && value.opt(0) is Number || forced == "intlist"
                val text = (0 until value.length()).joinToString(", ") { value.opt(it).toString() }
                TextInput(label, help, text, ro, multiline = false, secret = false, number = false) { t ->
                    val parts = t.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    state.set(path, JSONArray(if (numeric) parts.mapNotNull { it.toLongOrNull() } else parts))
                }
            }
        }
        value is Number || forced == "int" || forced == "number" -> {
            val text = when (value) {
                null -> ""
                is Double -> if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
                else -> value.toString()
            }
            TextInput(label, help, text, ro, multiline = false, secret = false, number = true) { t ->
                state.set(path, if (t.isBlank()) null else t.toLongOrNull() ?: t.toDoubleOrNull())
            }
        }
        else -> {
            TextInput(
                label, help, value?.toString() ?: "", ro,
                multiline = path in spec.multiline || key in spec.multiline,
                secret = SECRET.containsMatchIn(key) && !key.contains("Enabled") && !key.contains("Id"),
                number = false,
            ) { state.set(path, it) }
        }
    }
}

@Composable
private fun HelpText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
}

@Composable
private fun TextInput(
    label: String, help: String?, value: String, readOnly: Boolean,
    multiline: Boolean, secret: Boolean, number: Boolean, onChange: (String) -> Unit,
) {
    var show by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        OutlinedTextField(
            value = value, onValueChange = onChange, label = { Text(label) }, readOnly = readOnly, enabled = !readOnly,
            singleLine = !multiline, minLines = if (multiline) 3 else 1,
            visualTransformation = if (secret && !show) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (secret) ({
                IconButton({ show = !show }) { Icon(if (show) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, "Show") }
            }) else null,
            keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Number else KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
        )
        help?.let { HelpText(it) }
    }
}
