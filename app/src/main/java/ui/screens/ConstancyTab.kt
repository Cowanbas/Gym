package com.cowanbas.gym.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cowanbas.gym.data.*
import com.cowanbas.gym.ui.modals.DayDetailCard
import com.cowanbas.gym.ui.modals.HistoryEditModal
import com.cowanbas.gym.ui.modals.RoutinePickerBottomSheet
import com.cowanbas.gym.ui.theme.AppTheme
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import java.time.format.TextStyle as JavaTextStyle

@Composable
fun ConstancyTab(
    history: SnapshotStateMap<String, WorkoutHistory>,
    routines: Map<String, Routine>,
    templates: List<WorkoutTemplate>,
    onSaveWorkout: (WorkoutHistory) -> Unit,
    onDeleteWorkout: (String) -> Unit
) {
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var weeklyMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDateKey by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<WorkoutHistory?>(null) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }
    var showRoutinePickerForDate by remember { mutableStateOf<String?>(null) }

    val streak by remember(history) {
        derivedStateOf {
            var count = 0
            var date = LocalDate.now()
            if (!history.containsKey(fmt(date))) {
                date = date.minusDays(1)
            }
            while (true) {
                val dayValue = date.dayOfWeek.value
                val isWeekend = dayValue == 6 || dayValue == 7
                val hasWorkout = history.containsKey(fmt(date))

                if (hasWorkout) {
                    count++
                    date = date.minusDays(1)
                } else {
                    if (isWeekend) {
                        date = date.minusDays(1)
                    } else {
                        break
                    }
                }
            }
            count
        }
    }

    val monthCount by remember(history, selectedMonth) {
        derivedStateOf {
            val prefix = String.format(Locale.US, "%d-%02d", selectedMonth.year, selectedMonth.monthValue)
            history.keys.count { it.startsWith(prefix) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(Icons.Default.LocalFireDepartment, streak.toString(), "Current streak", Modifier.weight(1f))
                MetricCard(Icons.Default.CalendarToday, monthCount.toString(), "Monthly workouts", Modifier.weight(1f))
            }
        }

        item {
            SectionCard(Icons.Default.BarChart, "Consistency (16 Weeks)") {
                Box(Modifier.height(100.dp).fillMaxWidth()) { HeatmapGrid(history) }
            }
        }

        item {
            SectionCard(Icons.AutoMirrored.Filled.ShowChart, "Weeks of the month") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { weeklyMonth = weeklyMonth.minusMonths(1) }) {
                            Icon(Icons.Default.ChevronLeft, "Previous month", tint = AppTheme.text)
                        }
                        Text(
                            "${weeklyMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.ENGLISH).uppercase()} ${weeklyMonth.year}",
                            fontWeight = FontWeight.Normal, fontSize = 12.sp, color = AppTheme.text
                        )
                        IconButton(onClick = { weeklyMonth = weeklyMonth.plusMonths(1) }) {
                            Icon(Icons.Default.ChevronRight, "Next month", tint = AppTheme.text)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.height(110.dp).fillMaxWidth()) { WeeklyBarChart(history, weeklyMonth) }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTheme.card),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AppTheme.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedMonth = selectedMonth.minusMonths(1) }) {
                            Icon(Icons.Default.ChevronLeft, "Previous month", tint = AppTheme.text)
                        }
                        Text(
                            "${selectedMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.ENGLISH).uppercase()} ${selectedMonth.year}",
                            fontWeight = FontWeight.Normal, fontSize = 13.sp, color = AppTheme.text
                        )
                        IconButton(onClick = { selectedMonth = selectedMonth.plusMonths(1) }) {
                            Icon(Icons.Default.ChevronRight, "Next month", tint = AppTheme.text)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    CalendarGrid(
                        selectedMonth = selectedMonth,
                        history = history,
                        selectedDateKey = selectedDateKey,
                        onDateSelected = { selectedDateKey = it }
                    )
                }
            }
        }

        selectedDateKey?.let { dateKey ->
            item {
                DayDetailCard(
                    dateKey = dateKey,
                    historyItem = history[dateKey],
                    onClose = { selectedDateKey = null },
                    onEdit = { editing = it },
                    onAdd = { showRoutinePickerForDate = dateKey },
                    onDelete = { confirmDelete = dateKey }
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    showRoutinePickerForDate?.let { dateKey ->
        RoutinePickerBottomSheet(
            templates = templates,
            currentRoutines = routines,
            dateKey = dateKey,
            onDismiss = { showRoutinePickerForDate = null },
            onSelectRoutine = { routine ->
                showRoutinePickerForDate = null
                editing = WorkoutHistory(
                    date = dateKey,
                    routineKey = "",
                    routineTitle = routine.title,
                    exercises = routine.exercises.map { it.copy(id = System.nanoTime().toString() + "_" + it.id) }
                )
            },
            onSelectDefault = {
                showRoutinePickerForDate = null
                val date = LocalDate.parse(dateKey, DATE_FMT)
                val key = routineKeyForDate(date)
                val base = routines[key]
                editing = WorkoutHistory(
                    date = dateKey,
                    routineKey = key,
                    routineTitle = base?.title ?: "Empty",
                    exercises = base?.exercises?.map { it.copy(id = System.nanoTime().toString() + "_" + it.id) } ?: emptyList()
                )
            },
            onSelectEmpty = {
                showRoutinePickerForDate = null
                editing = WorkoutHistory(
                    date = dateKey,
                    routineKey = "",
                    routineTitle = "Workout",
                    exercises = emptyList()
                )
            }
        )
    }

    editing?.let { item ->
        HistoryEditModal(
            item = item,
            onDismiss = { editing = null },
            onSave = { savedItem ->
                onSaveWorkout(savedItem)
                weeklyMonth = YearMonth.from(LocalDate.parse(savedItem.date, DATE_FMT))
                editing = null
            }
        )
    }

    confirmDelete?.let { dateKey ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            containerColor = AppTheme.card,
            title = { Text("Delete workout?", color = AppTheme.text) },
            text = { Text("The record for $dateKey will be permanently removed.", color = AppTheme.muted) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteWorkout(dateKey)
                    confirmDelete = null
                }) { Text("Delete", color = AppTheme.text) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel", color = AppTheme.muted) }
            }
        )
    }
}

@Composable
fun SectionCard(icon: ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.card),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, AppTheme.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = AppTheme.text, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(title, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = AppTheme.text)
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun MetricCard(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.card),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, AppTheme.border),
        modifier = modifier
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(AppTheme.hover, RoundedCornerShape(8.dp))
                    .border(1.dp, AppTheme.border, RoundedCornerShape(8.dp))
                    .padding(6.dp)
            ) {
                Icon(icon, null, tint = AppTheme.text, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.Normal, color = AppTheme.text)
                Text(label, fontSize = 10.sp, color = AppTheme.muted)
            }
        }
    }
}

@Composable
fun HeatmapGrid(history: Map<String, WorkoutHistory>) {
    val now = remember { LocalDate.now() }
    val currentMonday = remember(now) {
        now.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    }
    val startMonday = remember(currentMonday) { currentMonday.minusWeeks(15) }

    val weeks = remember(startMonday) {
        (0..15).map { weekIndex ->
            val weekStart = startMonday.plusWeeks(weekIndex.toLong())
            val daysMap = (0..6).associate { dayOffset ->
                val date = weekStart.plusDays(dayOffset.toLong())
                date.dayOfWeek.value to date
            }
            listOf(daysMap[7], daysMap[6], daysMap[5], daysMap[4], daysMap[3], daysMap[2], daysMap[1])
        }
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            weeks.forEach { colDays ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    colDays.forEach { date ->
                        if (date != null) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (history.containsKey(fmt(date))) AppTheme.primary else AppTheme.hover,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        } else {
                            Spacer(modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyBarChart(history: Map<String, WorkoutHistory>, monthlyRef: YearMonth) {
    val weeksData by remember(monthlyRef, history) {
        derivedStateOf {
            val firstDayOfMonth = monthlyRef.atDay(1)
            val lastDayOfMonth = monthlyRef.atEndOfMonth()
            var currentMonday = firstDayOfMonth.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))

            val list = mutableListOf<Triple<String, Int, Boolean>>()
            var weekNum = 1
            val today = LocalDate.now()

            while (currentMonday <= lastDayOfMonth) {
                val weekSunday = currentMonday.plusDays(6)
                val count = (0..6).count { dayOffset ->
                    history.containsKey(fmt(currentMonday.plusDays(dayOffset.toLong())))
                }

                val isCurrentWeek = !today.isBefore(currentMonday) && !today.isAfter(weekSunday)
                val label = if (isCurrentWeek) "Current" else "Wk $weekNum"

                list.add(Triple(label, count, isCurrentWeek))
                currentMonday = currentMonday.plusWeeks(1)
                weekNum++
            }
            list
        }
    }

    if (weeksData.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No data", fontSize = 12.sp, color = AppTheme.muted)
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Bottom
    ) {
        weeksData.forEach { (label, count, isCurrent) ->
            val heightPct = (count / 7f).coerceIn(0.08f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxHeight()
            ) {
                Text("${count}d", fontSize = 10.sp, color = AppTheme.text, fontWeight = FontWeight.Normal)
                Spacer(Modifier.height(4.dp))

                Box(
                    modifier = Modifier.width(22.dp).height(55.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height((55 * heightPct).dp)
                            .background(
                                if (isCurrent) AppTheme.primary else AppTheme.muted.copy(alpha = 0.4f),
                                RoundedCornerShape(4.dp)
                            )
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    label,
                    fontSize = 10.sp,
                    color = if (isCurrent) AppTheme.text else AppTheme.muted,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun CalendarGrid(
    selectedMonth: YearMonth,
    history: Map<String, WorkoutHistory>,
    selectedDateKey: String?,
    onDateSelected: (String) -> Unit
) {
    val firstDay = remember(selectedMonth) { selectedMonth.atDay(1) }
    val daysInMonth = remember(selectedMonth) { selectedMonth.lengthOfMonth() }
    val startingWeekday = remember(firstDay) { (firstDay.dayOfWeek.value - 1 + 7) % 7 }
    val weekHeaders = remember { listOf("M", "T", "W", "T", "F", "S", "S") }
    val todayStr = remember { fmt(LocalDate.now()) }

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekHeaders.forEach { h ->
                Text(h, fontSize = 11.sp, fontWeight = FontWeight.Normal, color = AppTheme.muted,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(4.dp))

        val rows = (startingWeekday + daysInMonth + 6) / 7
        for (r in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val dayNum = (r * 7 + c) - startingWeekday + 1
                    if (dayNum in 1..daysInMonth) {
                        val dateKey = remember(selectedMonth, dayNum) { fmt(selectedMonth.atDay(dayNum)) }
                        val hasWorkout = history.containsKey(dateKey)
                        val isSelected = selectedDateKey == dateKey
                        val isToday = dateKey == todayStr
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .clickable { onDateSelected(dateKey) }
                                .background(if (hasWorkout) AppTheme.primary else AppTheme.hover)
                                .then(
                                    when {
                                        isSelected -> Modifier.border(2.dp, AppTheme.muted, CircleShape)
                                        isToday -> Modifier.border(1.dp, AppTheme.text, CircleShape)
                                        else -> Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$dayNum", fontSize = 11.sp, fontWeight = FontWeight.Normal, color = Color.White)
                        }
                    } else {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}