package com.cowanbas.gym.ui.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cowanbas.gym.data.Exercise
import com.cowanbas.gym.data.Routine
import com.cowanbas.gym.ui.components.*
import com.cowanbas.gym.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditModal(
    routine: Routine,
    isToday: Boolean,
    isDoneToday: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (Routine) -> Unit,
    onComplete: (Routine) -> Unit,
    onUnfinish: () -> Unit = {}
) {
    var title by remember { mutableStateOf(routine.title) }
    val exercises = remember { routine.exercises.toMutableStateList() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismiss, containerColor = AppTheme.card) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Workout name", color = AppTheme.muted) },
                        textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, color = AppTheme.text),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppTheme.primary,
                            unfocusedBorderColor = AppTheme.border,
                            cursorColor = AppTheme.primary
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = AppTheme.muted)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Exercises", fontWeight = FontWeight.Normal, fontSize = 13.sp, color = AppTheme.text)
                    }
                    TextButton(onClick = { exercises.add(Exercise()) }) {
                        Icon(Icons.Default.Add, null, tint = AppTheme.text, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("Add", color = AppTheme.text, fontSize = 12.sp)
                    }
                }
                HorizontalDivider(color = AppTheme.border)
            }

            if (exercises.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("No exercises registered.", color = AppTheme.muted, fontSize = 12.sp)
                    }
                }
            } else {
                items(exercises, key = { it.id }) { ex ->
                    val idx = exercises.indexOf(ex)
                    ExerciseCardItem(
                        exercise = ex,
                        index = idx,
                        totalItems = exercises.size,
                        onChanged = { updated -> if (idx in exercises.indices) exercises[idx] = updated },
                        onDelete = { if (idx in exercises.indices) exercises.removeAt(idx) },
                        onMove = { from, to ->
                            if (from in exercises.indices && to in exercises.indices) {
                                val item = exercises.removeAt(from)
                                exercises.add(to, item)
                            }
                        }
                    )
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isToday) {
                        val hasExercises = exercises.isNotEmpty()
                        if (isDoneToday) {
                            Button(
                                onClick = { onUnfinish() },
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary),
                                border = BorderStroke(1.dp, AppTheme.border),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Close, null, tint = AppTheme.text, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Unfinish", color = AppTheme.text, fontWeight = FontWeight.Normal)
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (hasExercises) {
                                        onComplete(Routine(title.ifBlank { "Empty" }, exercises.toList()))
                                    }
                                },
                                enabled = hasExercises,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppTheme.primary,
                                    disabledContainerColor = AppTheme.primary.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Finish", color = Color.White, fontWeight = FontWeight.Normal)
                            }
                        }
                    }
                    Button(
                        onClick = { onSave(Routine(title.ifBlank { "Empty" }, exercises.toList())) },
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.hover),
                        border = BorderStroke(1.dp, AppTheme.border),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, null, tint = AppTheme.text, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save", color = AppTheme.text)
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}