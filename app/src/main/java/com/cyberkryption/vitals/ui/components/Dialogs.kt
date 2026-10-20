package com.cyberkryption.vitals.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.theme.VitalsType

/** Square, outlined dialog with corner marks, in the system's visual language. */
@Composable
fun VitalsDialog(
    title: String,
    onDismissRequest: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String? = null,
    onDismiss: () -> Unit = onDismissRequest,
    confirmColor: Color = Vitals.colors.accent700,
    properties: DialogProperties = DialogProperties(),
    body: @Composable ColumnScope.() -> Unit,
) {
    val c = Vitals.colors
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        Column(
            modifier = modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .registrationMarks(c.mark)
                .background(c.bg)
                .border(1.dp, c.divider)
                .padding(start = 20.dp, top = 20.dp, end = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = VitalsType.section, modifier = Modifier.padding(end = 8.dp))
            Column(
                modifier = Modifier.padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = body,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            ) {
                if (dismissText != null) TextAction(dismissText, onDismiss, color = c.text)
                TextAction(confirmText, onConfirm, color = confirmColor)
            }
        }
    }
}

@Composable
fun DialogText(text: String) {
    Text(text, style = VitalsType.bodyMedium, color = Vitals.colors.text(0.85f))
}
