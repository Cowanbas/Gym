package com.cowanbas.gym.ui.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cowanbas.gym.data.Routine
import com.cowanbas.gym.data.WorkoutHistory
import com.cowanbas.gym.data.WorkoutTemplate
import com.cowanbas.gym.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailCard(
    dateKey: String,
    historyItem: WorkoutHistory?,
    onClose: () -> Unit,
    onEdit: (WorkoutHistory) -> Unit,
    onAdd: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.card),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, AppTheme.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Workout details: $dateKey", fontSize = 14.sp, fontWeight = FontWeight.Normal, color = AppTheme.text)
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = AppTheme.muted)
                }
            }
            Spacer(Modifier.height(8.dp))
            if (historyItem != null) {
                Text(historyItem.routineTitle, fontSize = 13.sp, fontWeight = FontWeight.Normal, color = AppTheme.primary)
                Spacer(Modifier.height(4.dp))
                Text("${historyItem.exercises.size} exercises recorded", fontSize = 11.sp, color = AppTheme.muted)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onEdit(historyItem) },
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit", color = Color.White)
                    }
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.hover),
                        border = BorderStroke(1.dp, AppTheme.border),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, null, tint = AppTheme.text, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", color = AppTheme.text)
                    }
                }
            } else {
                Text("No workout registered for this day.", fontSize = 12.sp, color = AppTheme.muted)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add workout", color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinePickerBottomSheet(
    templates: List<WorkoutTemplate>,
    currentRoutines: Map<String, Routine>,
    dateKey: String,
    onDismiss: () -> Unit,
    onSelectRoutine: (Routine) -> Unit,
    onSelectDefault: () -> Unit,
    onSelectEmpty: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismiss, containerColor = AppTheme.card) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Workout for $dateKey", fontSize = 16.sp, fontWeight = FontWeight.Normal, color = AppTheme.text)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = AppTheme.muted) }
                }
                Spacer(Modifier.height(4.dp))
            }
            item {
                Card(
                    onClick = onSelectDefault,
                    colors = CardDefaults.cardColors(containerColor = AppTheme.hover),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AppTheme.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Day default routine", fontWeight = FontWeight.Normal, color = AppTheme.text)
                        Text("Based on day of the week configuration", fontSize = 11.sp, color = AppTheme.muted)
                    }
                }
            }
            item {
                Card(
                    onClick = onSelectEmpty,
                    colors = CardDefaults.cardColors(containerColor = AppTheme.hover),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AppTheme.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Blank workout", fontWeight = FontWeight.Normal, color = AppTheme.text)
                        Text("Start from scratch", fontSize = 11.sp, color = AppTheme.muted)
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)); Text("Templates", fontSize = 12.sp, color = AppTheme.muted); Spacer(Modifier.height(4.dp)) }

            val activeTemplate = templates.firstOrNull()
            if (activeTemplate != null) {
                items(activeTemplate.routines.entries.toList()) { entry ->
                    Card(
                        onClick = { onSelectRoutine(entry.value) },
                        colors = CardDefaults.cardColors(containerColor = AppTheme.hover),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AppTheme.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.value.title, fontWeight = FontWeight.Normal, color = AppTheme.text)
                                Text("${entry.value.exercises.size} exercises", fontSize = 11.sp, color = AppTheme.muted)
                            }
                            Icon(Icons.Default.Check, null, tint = AppTheme.muted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryEditModal(
    item: WorkoutHistory,
    onDismiss: () -> Unit,
    onSave: (WorkoutHistory) -> Unit
) {
    var title by remember { mutableStateOf(item.routineTitle) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismiss, containerColor = AppTheme.card) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Workout Record", fontSize = 16.sp, fontWeight = FontWeight.Normal, color = AppTheme.text)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = AppTheme.muted) }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Workout Title", color = AppTheme.muted) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { onSave(item.copy(routineTitle = title)) },
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes", color = Color.White)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}