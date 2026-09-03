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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
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
import com.cowanbas.gym.data.Routine
import com.cowanbas.gym.data.Store
import com.cowanbas.gym.data.WEEK_DAYS
import com.cowanbas.gym.data.WorkoutTemplate
import com.cowanbas.gym.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesBottomSheetModal(
    templates: List<WorkoutTemplate>,
    activeTemplateName: String,
    onDismiss: () -> Unit,
    onSelectTemplate: (String) -> Unit,
    onCreateClick: () -> Unit,
    onCopyClick: () -> Unit,
    onEditTemplateClick: (WorkoutTemplate) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = AppTheme.card
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Workout templates", fontSize = 16.sp, fontWeight = FontWeight.Normal, color = AppTheme.text)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = AppTheme.muted)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            items(templates, key = { it.name }) { template ->
                Card(
                    onClick = {
                        onDismiss()
                        onSelectTemplate(template.name)
                    },
                    colors = CardDefaults.cardColors(containerColor = AppTheme.hover),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AppTheme.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (template.name == activeTemplateName) {
                                Icon(Icons.Default.Check, null, tint = AppTheme.text, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(template.name, color = AppTheme.text, fontWeight = FontWeight.Normal)
                        }
                        IconButton(
                            onClick = {
                                onDismiss()
                                onEditTemplateClick(template)
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Configure", tint = AppTheme.muted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onCreateClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Create new", color = Color.White, fontWeight = FontWeight.Normal)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onCopyClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy existing", color = Color.White, fontWeight = FontWeight.Normal)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyTemplateModal(
    templates: List<WorkoutTemplate>,
    onDismiss: () -> Unit,
    onCopy: (String, Map<String, Routine>) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var newName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismiss, containerColor = AppTheme.card) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Copy existing template", fontSize = 16.sp, fontWeight = FontWeight.Normal, color = AppTheme.text)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = AppTheme.muted)
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (selectedTemplate == null) {
                    Text("Select a workout or create a template.", fontSize = 12.sp, color = AppTheme.muted)
                }
            }

            if (selectedTemplate == null) {
                items(templates, key = { it.name }) { template ->
                    Card(
                        onClick = {
                            selectedTemplate = template
                            newName = "${template.name} (Copy)"
                            errorMessage = null
                        },
                        colors = CardDefaults.cardColors(containerColor = AppTheme.hover),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AppTheme.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(template.name, fontWeight = FontWeight.Normal, fontSize = 13.sp, color = AppTheme.text)
                                Text("${template.routines.size} days configured", fontSize = 11.sp, color = AppTheme.muted)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = AppTheme.muted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            } else {
                item {
                    Text("Source template: ${selectedTemplate!!.name}", fontSize = 12.sp, fontWeight = FontWeight.Normal, color = AppTheme.primary)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("New Template name", color = AppTheme.muted) },
                        textStyle = TextStyle(fontSize = 14.sp, color = AppTheme.text),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppTheme.primary,
                            unfocusedBorderColor = AppTheme.border,
                            cursorColor = AppTheme.primary
                        )
                    )
                    if (errorMessage != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(errorMessage!!, fontSize = 11.sp, color = Color(0xFFFF5252))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { selectedTemplate = null },
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.hover),
                            border = BorderStroke(1.dp, AppTheme.border),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back", color = AppTheme.text)
                        }
                        Button(
                            onClick = {
                                val name = newName.trim().ifBlank { "${selectedTemplate!!.name} (Copy)" }
                                val nameExists = templates.any { it.name.equals(name, ignoreCase = true) }
                                if (nameExists) {
                                    errorMessage = "A template with this name already exists."
                                } else {
                                    val copiedRoutines = selectedTemplate!!.routines.mapValues { (_, routine) ->
                                        Routine(
                                            title = routine.title,
                                            exercises = routine.exercises.map { ex ->
                                                ex.copy(id = System.nanoTime().toString() + "_" + ex.id)
                                            }
                                        )
                                    }
                                    onCopy(name, copiedRoutines)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save Copy", color = Color.White, fontWeight = FontWeight.Normal)
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
fun EditTemplateModal(
    template: WorkoutTemplate,
    templates: List<WorkoutTemplate>,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (WorkoutTemplate) -> Unit,
    onDelete: () -> Unit
) {
    var templateName by remember { mutableStateOf(template.name) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val routinesMap = remember {
        mutableStateMapOf<String, Routine>().apply { putAll(template.routines) }
    }
    var editingDayKey by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismiss, containerColor = AppTheme.card) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Edit template", fontSize = 16.sp, fontWeight = FontWeight.Normal, color = AppTheme.text)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = AppTheme.muted)
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = templateName,
                    onValueChange = {
                        templateName = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Template Name", color = AppTheme.muted) },
                    textStyle = TextStyle(fontSize = 14.sp, color = AppTheme.text),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.primary,
                        unfocusedBorderColor = AppTheme.border,
                        cursorColor = AppTheme.primary
                    )
                )
                if (errorMessage != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(errorMessage!!, fontSize = 11.sp, color = Color(0xFFFF5252))
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text("Template workouts tap to edit or delete exercises", fontSize = 12.sp, color = AppTheme.muted, fontWeight = FontWeight.Normal)
            }

            items(WEEK_DAYS, key = { it.key }) { day ->
                val routine = routinesMap[day.key] ?: Routine("Empty")
                Card(
                    onClick = { editingDayKey = day.key },
                    colors = CardDefaults.cardColors(containerColor = AppTheme.hover),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AppTheme.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(day.name, fontSize = 11.sp, color = AppTheme.muted, fontWeight = FontWeight.Normal)
                            Text(routine.title, fontSize = 13.sp, color = AppTheme.text, fontWeight = FontWeight.Normal)
                            Text("${routine.exercises.size} exercises", fontSize = 10.sp, color = AppTheme.muted)
                        }
                        Icon(Icons.Default.Edit, null, tint = AppTheme.muted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            item {
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canDelete) {
                        Button(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.hover),
                            border = BorderStroke(1.dp, AppTheme.border),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, null, tint = AppTheme.text, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete", color = AppTheme.text)
                        }
                    }
                    Button(
                        onClick = {
                            val name = templateName.trim().ifBlank { template.name }
                            val nameExists = templates.any { it.name.equals(name, ignoreCase = true) && it.name != template.name }
                            if (nameExists) {
                                errorMessage = "Another template with this name already exists."
                            } else {
                                onSave(WorkoutTemplate(name, routinesMap.toMap()))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save", color = Color.White, fontWeight = FontWeight.Normal)
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    editingDayKey?.let { dayKey ->
        val routine = routinesMap[dayKey] ?: Routine("Empty")
        RoutineEditModal(
            routine = routine,
            isToday = false,
            isDoneToday = false,
            onDismiss = { editingDayKey = null },
            onSave = { updated ->
                routinesMap[dayKey] = updated
                editingDayKey = null
            },
            onComplete = { updated ->
                routinesMap[dayKey] = updated
                editingDayKey = null
            },
            onUnfinish = {}
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = AppTheme.card,
            title = { Text("Delete template?", color = AppTheme.text) },
            text = { Text("The template '${template.name}' will be permanently removed.", color = AppTheme.muted) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Delete", color = AppTheme.text) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = AppTheme.muted) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTemplateModal(
    templates: List<WorkoutTemplate>,
    onDismiss: () -> Unit,
    onSave: (String, Map<String, Routine>) -> Unit
) {
    var templateName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val routinesMap = remember {
        mutableStateMapOf<String, Routine>().apply { putAll(Store.defaultRoutines()) }
    }
    var editingDayKey by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismiss, containerColor = AppTheme.card) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create new template", fontSize = 16.sp, fontWeight = FontWeight.Normal, color = AppTheme.text)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = AppTheme.muted)
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = templateName,
                    onValueChange = {
                        templateName = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Template name", color = AppTheme.muted) },
                    textStyle = TextStyle(fontSize = 14.sp, color = AppTheme.text),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.primary,
                        unfocusedBorderColor = AppTheme.border,
                        cursorColor = AppTheme.primary
                    )
                )
                if (errorMessage != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(errorMessage!!, fontSize = 11.sp, color = Color(0xFFFF5252))
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text("Define the workouts for the days of the week.", fontSize = 12.sp, color = AppTheme.muted, fontWeight = FontWeight.Normal)
            }

            items(WEEK_DAYS, key = { it.key }) { day ->
                val routine = routinesMap[day.key] ?: Routine("Empty")
                Card(
                    onClick = { editingDayKey = day.key },
                    colors = CardDefaults.cardColors(containerColor = AppTheme.hover),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AppTheme.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(day.name, fontSize = 11.sp, color = AppTheme.muted, fontWeight = FontWeight.Normal)
                            Text(routine.title, fontSize = 13.sp, color = AppTheme.text, fontWeight = FontWeight.Normal)
                            Text("${routine.exercises.size} exercises", fontSize = 10.sp, color = AppTheme.muted)
                        }
                        Icon(Icons.Default.Edit, null, tint = AppTheme.muted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            item {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val name = templateName.trim().ifBlank { "New Template" }
                        val nameExists = templates.any { it.name.equals(name, ignoreCase = true) }
                        if (nameExists) {
                            errorMessage = "A template with this name already exists."
                        } else {
                            onSave(name, routinesMap.toMap())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save and apply", color = Color.White, fontWeight = FontWeight.Normal)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    editingDayKey?.let { dayKey ->
        val routine = routinesMap[dayKey] ?: Routine("Empty")
        RoutineEditModal(
            routine = routine,
            isToday = false,
            isDoneToday = false,
            onDismiss = { editingDayKey = null },
            onSave = { updated ->
                routinesMap[dayKey] = updated
                editingDayKey = null
            },
            onComplete = { updated ->
                routinesMap[dayKey] = updated
                editingDayKey = null
            },
            onUnfinish = {}
        )
    }
}