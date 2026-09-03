package com.cowanbas.gym.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.cowanbas.gym.data.Exercise
import com.cowanbas.gym.data.ExerciseSet
import com.cowanbas.gym.ui.theme.LocalAdvancedMode
import com.cowanbas.gym.ui.theme.LocalWeightUnit
import com.cowanbas.gym.ui.theme.AppTheme
import java.util.Locale

@Composable
fun ExerciseCardItem(
    exercise: Exercise,
    index: Int,
    totalItems: Int,
    onChanged: (Exercise) -> Unit,
    onDelete: () -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val itemHeightPx = with(LocalDensity.current) { 56.dp.toPx() }

    val isAdvanced = LocalAdvancedMode.current
    val unitStr = LocalWeightUnit.current

    val activeSets = remember(exercise.advancedSets, exercise.sets) {
        if (exercise.advancedSets.isEmpty() && exercise.sets > 0) {
            List(exercise.sets) { ExerciseSet(reps = exercise.reps, weight = exercise.weight) }
        } else {
            exercise.advancedSets
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.hover),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, AppTheme.border),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = dragOffset
                scaleX = if (isDragging) 1.02f else 1f
                scaleY = if (isDragging) 1.02f else 1f
            }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (expanded) {
                    OutlinedTextField(
                        value = exercise.name,
                        onValueChange = { onChanged(exercise.copy(name = it)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Exercise Name", color = AppTheme.muted, fontSize = 14.sp) },
                        textStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = AppTheme.text),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppTheme.primary,
                            unfocusedBorderColor = AppTheme.border,
                            cursorColor = AppTheme.primary
                        )
                    )
                } else {
                    Text(
                        text = exercise.name.ifBlank { "Empty" },
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = if (exercise.name.isBlank()) AppTheme.muted else AppTheme.text)
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = AppTheme.muted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = AppTheme.muted, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = AppTheme.muted,
                    modifier = Modifier
                        .size(24.dp)
                        .pointerInput(index) {
                            detectDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = {
                                    isDragging = false
                                    val shift = kotlin.math.round(dragOffset / itemHeightPx).toInt()
                                    val targetIndex = (index + shift).coerceIn(0, totalItems - 1)
                                    if (targetIndex != index) {
                                        onMove(index, targetIndex)
                                    }
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    isDragging = false
                                    dragOffset = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount.y
                                }
                            )
                        }
                )
            }
            if (expanded) {
                Spacer(Modifier.height(6.dp))
                if (isAdvanced) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Spacer(Modifier.width(60.dp))
                            Text("Reps", fontSize = 12.sp, color = AppTheme.muted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("Weight ($unitStr)", fontSize = 12.sp, color = AppTheme.muted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Spacer(Modifier.width(36.dp))
                        }

                        activeSets.forEachIndexed { sIdx, set ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Set ${String.format(Locale.US, "%02d", sIdx + 1)}", fontSize = 12.sp, color = AppTheme.muted, modifier = Modifier.width(60.dp))
                                NumberInputField("", set.reps.toString(), Modifier.weight(1f)) {
                                    val newSets = activeSets.toMutableList()
                                    newSets[sIdx] = set.copy(reps = it.toIntOrNull() ?: 0)
                                    onChanged(exercise.copy(advancedSets = newSets, sets = newSets.size))
                                }
                                Spacer(Modifier.width(8.dp))
                                NumberInputField("", trimNumber(set.weight), Modifier.weight(1f), decimal = true) {
                                    val newSets = activeSets.toMutableList()
                                    newSets[sIdx] = set.copy(weight = it.replace(',', '.').toDoubleOrNull() ?: 0.0)
                                    onChanged(exercise.copy(advancedSets = newSets, sets = newSets.size))
                                }
                                IconButton(
                                    onClick = {
                                        val newSets = activeSets.toMutableList()
                                        newSets.removeAt(sIdx)
                                        onChanged(exercise.copy(advancedSets = newSets, sets = newSets.size))
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "Delete", tint = AppTheme.muted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = {
                                val newSets = activeSets.toMutableList()
                                val last = newSets.lastOrNull() ?: ExerciseSet(reps = 0, weight = 0.0)
                                newSets.add(ExerciseSet(reps = last.reps, weight = last.weight))
                                onChanged(exercise.copy(advancedSets = newSets, sets = newSets.size))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.hover),
                            border = BorderStroke(1.dp, AppTheme.border)
                        ) {
                            Icon(Icons.Default.Add, null, tint = AppTheme.text, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add sets", color = AppTheme.text, fontWeight = FontWeight.Normal)
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NumberInputField("Sets", exercise.sets.toString(), Modifier.weight(1f)) {
                            onChanged(exercise.copy(sets = it.toIntOrNull() ?: 0))
                        }
                        NumberInputField("Reps", exercise.reps.toString(), Modifier.weight(1f)) {
                            onChanged(exercise.copy(reps = it.toIntOrNull() ?: 0))
                        }
                        NumberInputField("Weight ($unitStr)", trimNumber(exercise.weight), Modifier.weight(1f), decimal = true) {
                            onChanged(exercise.copy(weight = it.replace(',', '.').toDoubleOrNull() ?: 0.0))
                        }
                    }
                }
            }
        }
    }
}

fun trimNumber(v: Double): String {
    return if (v % 1.0 == 0.0) {
        val intVal = v.toInt()
        if (intVal in 0..9) String.format(Locale.US, "%02d", intVal) else intVal.toString()
    } else {
        v.toString()
    }
}