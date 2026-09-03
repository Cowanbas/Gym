package com.cowanbas.gym.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cowanbas.gym.ui.theme.AppTheme

@Composable
fun NumberInputField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
    onChanged: (String) -> Unit
) {
    var text by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        if (value != text) {
            val currentNum = text.replace(',', '.').toDoubleOrNull() ?: 0.0
            val externalNum = value.replace(',', '.').toDoubleOrNull() ?: 0.0
            if (currentNum != externalNum) {
                text = value
            }
        }
    }

    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            Text(label, fontSize = 12.sp, color = AppTheme.muted)
            Spacer(Modifier.height(2.dp))
        }
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }
                text = filtered
                onChanged(filtered)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
            ),
            textStyle = TextStyle(fontSize = 14.sp, color = AppTheme.text, textAlign = TextAlign.Center),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.primary,
                unfocusedBorderColor = AppTheme.border,
                cursorColor = AppTheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}