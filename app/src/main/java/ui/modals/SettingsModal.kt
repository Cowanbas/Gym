package com.cowanbas.gym.ui.modals

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.cowanbas.gym.ui.components.SegmentedButton
import com.cowanbas.gym.ui.theme.LocalAdvancedMode
import com.cowanbas.gym.ui.theme.LocalWeightUnit
import com.cowanbas.gym.ui.theme.AppTheme
import org.json.JSONObject
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsModal(
    onDismiss: () -> Unit,
    onAdvancedChange: (Boolean) -> Unit,
    onUnitChange: (String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val prefs = context.getSharedPreferences("gym_store", Context.MODE_PRIVATE)
                    val jsonObject = JSONObject()
                    prefs.all.forEach { (key, value) ->
                        if (value != null) jsonObject.put(key, value)
                    }
                    outputStream.write(jsonObject.toString().toByteArray())
                    outputStream.flush()
                }
                Toast.makeText(context, "Settings exported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val jsonStr = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                } ?: throw Exception("Empty file")

                val jsonObject = JSONObject(jsonStr)
                val prefs = context.getSharedPreferences("gym_store", Context.MODE_PRIVATE)
                prefs.edit {
                    clear()
                    jsonObject.keys().forEach { key ->
                        when (val value = jsonObject.get(key)) {
                            is String -> putString(key, value)
                            is Int -> putInt(key, value)
                            is Boolean -> putBoolean(key, value)
                            is Float -> putFloat(key, value)
                            is Long -> putLong(key, value)
                            else -> putString(key, value.toString())
                        }
                    }
                }
                Toast.makeText(context, "Settings imported successfully", Toast.LENGTH_SHORT).show()

                var ctx = context
                while (ctx is android.content.ContextWrapper) {
                    if (ctx is android.app.Activity) {
                        ctx.recreate()
                        break
                    }
                    ctx = ctx.baseContext
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = AppTheme.card
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Settings", fontSize = 16.sp, fontWeight = FontWeight.Normal, color = AppTheme.text)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = AppTheme.muted)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Data management", fontSize = 12.sp, fontWeight = FontWeight.Normal, color = AppTheme.muted)
            }

            item {
                Card(
                    onClick = { exportLauncher.launch("gym_settings_${LocalDate.now()}.json") },
                    colors = CardDefaults.cardColors(containerColor = AppTheme.hover),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AppTheme.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Upload, null, tint = AppTheme.text, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Export settings", fontWeight = FontWeight.Normal, fontSize = 13.sp, color = AppTheme.text)
                            Text("Save routines, templates, and history.", fontSize = 11.sp, color = AppTheme.muted)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = AppTheme.muted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            item {
                Card(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    colors = CardDefaults.cardColors(containerColor = AppTheme.hover),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AppTheme.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Download, null, tint = AppTheme.text, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Import settings", fontWeight = FontWeight.Normal, fontSize = 13.sp, color = AppTheme.text)
                            Text("Load routines, templates, and history.", fontSize = 11.sp, color = AppTheme.muted)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = AppTheme.muted, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                Text("Advanced", fontSize = 12.sp, fontWeight = FontWeight.Normal, color = AppTheme.muted)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Detailed set tracking", fontSize = 14.sp, color = AppTheme.text)
                        Text("Enable individual reps and weights per set", fontSize = 11.sp, color = AppTheme.muted)
                    }
                    Switch(
                        checked = LocalAdvancedMode.current,
                        onCheckedChange = { onAdvancedChange(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AppTheme.text,
                            checkedTrackColor = AppTheme.primary,
                            uncheckedThumbColor = AppTheme.muted,
                            uncheckedTrackColor = AppTheme.hover
                        )
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Weight unit", fontSize = 14.sp, color = AppTheme.text)
                Text("Select unit for exercise weights", fontSize = 11.sp, color = AppTheme.muted)
                Spacer(Modifier.height(6.dp))
                SegmentedButton(
                    options = listOf("KG", "LB"),
                    selected = if (LocalWeightUnit.current == "kg") "KG" else "LB"
                ) {
                    onUnitChange(if (it == "KG") "kg" else "lb")
                }
                Spacer(Modifier.height(32.dp))
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "©Cowanbas all rights reserved.",
                        fontSize = 11.sp,
                        color = AppTheme.muted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}