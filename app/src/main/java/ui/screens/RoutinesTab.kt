package com.cowanbas.gym.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.cowanbas.gym.data.*
import com.cowanbas.gym.ui.modals.*
import com.cowanbas.gym.ui.theme.AppTheme

@Composable
fun RoutinesTab(
    routines: Map<String, Routine>,
    history: Map<String, WorkoutHistory>,
    todayKey: String,
    formattedToday: String,
    templates: List<WorkoutTemplate>,
    activeTemplateName: String,
    onSelectTemplate: (String) -> Unit,
    onCreateTemplate: (String, Map<String, Routine>) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onUpdateTemplate: (String, WorkoutTemplate) -> Unit,
    onRoutineUpdated: (String, Routine) -> Unit,
    onSwapRoutines: (Int, Int) -> Unit,
    onCompleteToday: (String, Routine) -> Unit,
    onDeleteWorkout: (String) -> Unit,
    onAdvancedChange: (Boolean) -> Unit,
    onUnitChange: (String) -> Unit
) {
    var editingKey by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showCreateModal by remember { mutableStateOf(false) }
    var showCopyModal by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var showSettingsModal by remember { mutableStateOf(false) }
    var showTemplatesModal by remember { mutableStateOf(false) }

    val todayShort = remember(todayKey) { WEEK_DAYS.first { it.key == todayKey }.short }
    val itemHeightPx = with(LocalDensity.current) { 72.dp.toPx() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Weekly workouts", fontSize = 20.sp, fontWeight = FontWeight.Normal, color = AppTheme.text)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .background(AppTheme.hover, RoundedCornerShape(20.dp))
                            .border(1.dp, AppTheme.border, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Today: $todayShort", fontSize = 12.sp, color = AppTheme.text, fontWeight = FontWeight.Normal)
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = AppTheme.text)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(AppTheme.card).border(1.dp, AppTheme.border, RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Settings", color = AppTheme.text, fontWeight = FontWeight.Normal, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                onClick = {
                                    menuExpanded = false
                                    showSettingsModal = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Templates", color = AppTheme.text, fontWeight = FontWeight.Normal, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                onClick = {
                                    menuExpanded = false
                                    showTemplatesModal = true
                                }
                            )
                        }
                    }
                }
            }
        }

        itemsIndexed(WEEK_DAYS, key = { _, day -> day.key }) { index, day ->
            val routine = routines[day.key] ?: Routine("Empty")
            val isToday = day.key == todayKey
            val isDoneToday = isToday && history.containsKey(formattedToday)

            var dragOffset by remember { mutableFloatStateOf(0f) }
            var isDragging by remember { mutableStateOf(false) }

            Card(
                onClick = { if (!isDragging) editingKey = day.key },
                colors = CardDefaults.cardColors(containerColor = AppTheme.card),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    width = if (isToday) 1.5.dp else 1.dp,
                    color = if (isToday) AppTheme.primary.copy(alpha = 0.8f) else AppTheme.border
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = dragOffset
                        scaleX = if (isDragging) 1.02f else 1f
                        scaleY = if (isDragging) 1.02f else 1f
                    }
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                color = if (isToday) AppTheme.primary else AppTheme.hover
                            )
                            .then(if (!isToday) Modifier.border(1.dp, AppTheme.border, RoundedCornerShape(8.dp)) else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            day.short.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(routine.title, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = AppTheme.text)
                            if (isToday) {
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(AppTheme.hover, RoundedCornerShape(4.dp))
                                        .border(1.dp, AppTheme.border, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text("TODAY", fontSize = 8.sp, color = AppTheme.text, fontWeight = FontWeight.Normal)
                                }
                            }
                        }
                        Text(
                            "${routine.exercises.size} ${if (routine.exercises.size == 1) "exercise" else "exercises"}",
                            fontSize = 11.sp,
                            color = AppTheme.muted
                        )
                    }
                    if (isDoneToday) {
                        Icon(Icons.Default.CheckCircle, null, tint = AppTheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                    }

                    Spacer(Modifier.width(4.dp))

                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag to reorder day workout",
                        tint = AppTheme.muted,
                        modifier = Modifier
                            .size(24.dp)
                            .pointerInput(index) {
                                detectDragGestures(
                                    onDragStart = { isDragging = true },
                                    onDragEnd = {
                                        isDragging = false
                                        val shift = kotlin.math.round(dragOffset / itemHeightPx).toInt()
                                        val targetIndex = (index + shift).coerceIn(0, WEEK_DAYS.size - 1)
                                        if (targetIndex != index) {
                                            onSwapRoutines(index, targetIndex)
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
            }
        }
    }

    editingKey?.let { key ->
        val routine = routines[key] ?: Routine("Empty")
        RoutineEditModal(
            routine = routine,
            isToday = key == todayKey,
            isDoneToday = key == todayKey && history.containsKey(formattedToday),
            onDismiss = { editingKey = null },
            onSave = { updated ->
                onRoutineUpdated(key, updated)
                editingKey = null
            },
            onComplete = { updated ->
                onRoutineUpdated(key, updated)
                onCompleteToday(key, updated)
                editingKey = null
            },
            onUnfinish = {
                onDeleteWorkout(formattedToday)
                editingKey = null
            }
        )
    }

    if (showSettingsModal) {
        SettingsModal(
            onDismiss = { showSettingsModal = false },
            onAdvancedChange = onAdvancedChange,
            onUnitChange = onUnitChange
        )
    }

    if (showTemplatesModal) {
        TemplatesBottomSheetModal(
            templates = templates,
            activeTemplateName = activeTemplateName,
            onDismiss = { showTemplatesModal = false },
            onSelectTemplate = { name -> onSelectTemplate(name) },
            onCreateClick = { showCreateModal = true },
            onCopyClick = { showCopyModal = true },
            onEditTemplateClick = { template -> editingTemplate = template }
        )
    }

    if (showCreateModal) {
        CreateTemplateModal(
            templates = templates,
            onDismiss = { showCreateModal = false },
            onSave = { name, newRoutines ->
                onCreateTemplate(name, newRoutines)
                showCreateModal = false
            }
        )
    }

    if (showCopyModal) {
        CopyTemplateModal(
            templates = templates,
            onDismiss = { showCopyModal = false },
            onCopy = { name, newRoutines ->
                onCreateTemplate(name, newRoutines)
                showCopyModal = false
            }
        )
    }

    editingTemplate?.let { template ->
        EditTemplateModal(
            template = template,
            templates = templates,
            canDelete = templates.size > 1,
            onDismiss = { editingTemplate = null },
            onSave = { updatedTemplate ->
                onUpdateTemplate(template.name, updatedTemplate)
                editingTemplate = null
            },
            onDelete = {
                onDeleteTemplate(template.name)
                editingTemplate = null
            }
        )
    }
}