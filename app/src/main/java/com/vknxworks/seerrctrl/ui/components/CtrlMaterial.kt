package com.vknxworks.seerrctrl.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vknxworks.seerrctrl.ui.theme.CtrlColors
import com.vknxworks.seerrctrl.ui.theme.CtrlShapes
import androidx.compose.material3.AlertDialog as M3AlertDialog
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.Card as M3Card
import androidx.compose.material3.FilterChip as M3FilterChip
import androidx.compose.material3.ModalBottomSheet as M3ModalBottomSheet
import androidx.compose.material3.OutlinedButton as M3OutlinedButton
import androidx.compose.material3.OutlinedTextField as M3OutlinedTextField

// Signal-styled stand-ins for the Material widgets the app uses, with the same parameters, so screens only
// swap an import to get the Ctrl look.

private val ButtonPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)

/** Primary button: accent fill, dark text, 16 dp corners. */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    M3Button(
        onClick, modifier, enabled,
        shape = CtrlShapes.Button,
        colors = ButtonDefaults.buttonColors(
            containerColor = CtrlColors.Accent, contentColor = CtrlColors.OnAccent,
            disabledContainerColor = CtrlColors.Raised, disabledContentColor = CtrlColors.Inactive,
        ),
        contentPadding = ButtonPadding,
        content = content,
    )
}

/** Secondary button: surface fill with a 1 dp border. */
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    M3OutlinedButton(
        onClick, modifier, enabled,
        shape = CtrlShapes.Button,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = CtrlColors.Surface, contentColor = CtrlColors.Text,
            disabledContainerColor = CtrlColors.Surface, disabledContentColor = CtrlColors.Inactive,
        ),
        border = BorderStroke(1.dp, CtrlColors.ControlOutline),
        contentPadding = ButtonPadding,
        content = content,
    )
}

/** Was Material's tonal button; in the Signal look it's the secondary button. */
@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) = OutlinedButton(onClick, modifier, enabled, content)

/** Card: surface fill, 1 dp border, 22 dp corners. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(containerColor = CtrlColors.Surface),
    content: @Composable ColumnScope.() -> Unit,
) {
    M3Card(
        modifier = modifier,
        shape = CtrlShapes.Card,
        colors = CardDefaults.cardColors(containerColor = colors.containerColor, contentColor = CtrlColors.Text),
        elevation = CardDefaults.cardElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        border = BorderStroke(1.dp, CtrlColors.Outline),
        content = content,
    )
}

/** Filter chip: 10 dp corners, accent fill when selected. */
@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    M3FilterChip(
        selected, onClick, label, modifier, enabled,
        leadingIcon = leadingIcon,
        shape = CtrlShapes.Chip,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = CtrlColors.Surface,
            labelColor = CtrlColors.Muted,
            selectedContainerColor = CtrlColors.Accent,
            selectedLabelColor = CtrlColors.OnAccent,
            selectedLeadingIconColor = CtrlColors.OnAccent,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled, selected = selected,
            borderColor = CtrlColors.ControlOutline, selectedBorderColor = CtrlColors.Accent, borderWidth = 1.dp,
        ),
    )
}

/** Text field: surface fill, 1 dp border, 16 dp corners; accent border and cursor while focused. */
@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    @Suppress("UNUSED_PARAMETER") shape: Shape = CtrlShapes.Field,
) {
    M3OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = modifier, enabled = enabled, readOnly = readOnly,
        label = label, placeholder = placeholder, leadingIcon = leadingIcon, trailingIcon = trailingIcon,
        isError = isError, visualTransformation = visualTransformation, keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions, singleLine = singleLine, maxLines = maxLines, minLines = minLines,
        shape = CtrlShapes.Field,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CtrlColors.Surface,
            unfocusedContainerColor = CtrlColors.Surface,
            disabledContainerColor = CtrlColors.Surface,
            errorContainerColor = CtrlColors.Surface,
            focusedBorderColor = CtrlColors.AccentText,
            unfocusedBorderColor = CtrlColors.ControlOutline,
            disabledBorderColor = CtrlColors.Outline,
            cursorColor = CtrlColors.AccentText,
            focusedLabelColor = CtrlColors.AccentText,
            unfocusedLabelColor = CtrlColors.Muted,
            focusedLeadingIconColor = CtrlColors.Muted,
            unfocusedLeadingIconColor = CtrlColors.Muted,
            focusedTrailingIconColor = CtrlColors.Muted,
            unfocusedTrailingIconColor = CtrlColors.Muted,
            focusedPlaceholderColor = CtrlColors.Inactive,
            unfocusedPlaceholderColor = CtrlColors.Inactive,
        ),
    )
}

/** Dialog: surface fill, 26 dp corners, 1 dp border. */
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    M3AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier.border(1.dp, CtrlColors.Outline, CtrlShapes.Dialog),
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = CtrlShapes.Dialog,
        containerColor = CtrlColors.Surface,
        iconContentColor = CtrlColors.AccentText,
        titleContentColor = CtrlColors.Text,
        textContentColor = CtrlColors.Muted,
        tonalElevation = 0.dp,
    )
}

private val SheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)

/** Bottom sheet: surface fill, 26 dp top corners, 1 dp border. */
@Composable
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    @Suppress("UNUSED_PARAMETER") containerColor: Color = CtrlColors.Surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    M3ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier.border(1.dp, CtrlColors.Outline, SheetShape),
        sheetState = sheetState,
        shape = SheetShape,
        containerColor = CtrlColors.Surface,
        contentColor = CtrlColors.Text,
        tonalElevation = 0.dp,
        content = content,
    )
}

/** Material switch replaced by [CtrlSwitch]. */
@Composable
fun Switch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) =
    CtrlSwitch(checked, onCheckedChange, modifier, enabled)

/** Material progress bar replaced by [CtrlProgress]. */
@Composable
fun LinearProgressIndicator(progress: () -> Float, modifier: Modifier = Modifier) =
    CtrlProgress(progress, modifier)
