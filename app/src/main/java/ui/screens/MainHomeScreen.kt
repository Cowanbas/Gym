package com.cowanbas.gym.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cowanbas.gym.data.*
import com.cowanbas.gym.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MainHomeScreen(
    onAdvancedChange: (Boolean) -> Unit = {},
    onUnitChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 3 })

    val routines = remember { mutableStateMapOf<String, Routine>() }
    val templates = remember { mutableStateListOf<WorkoutTemplate>() }
    var activeTemplateName by remember { mutableStateOf("Template A") }
    val history = remember { mutableStateMapOf<String, WorkoutHistory>() }
    var loaded by remember { mutableStateOf(false) }
    var stopwatchTimeInMillis by remember { mutableLongStateOf(0L) }
    var stopwatchIsRunning by remember { mutableStateOf(false) }

    LaunchedEffect(stopwatchIsRunning) {
        if (stopwatchIsRunning) {
            val startTime = System.currentTimeMillis() - stopwatchTimeInMillis
            while (stopwatchIsRunning) {
                stopwatchTimeInMillis = System.currentTimeMillis() - startTime
                delay(50.milliseconds)
            }
        }
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val loadedTemplates = Store.loadTemplates(context) ?: Store.defaultTemplates()
        val loadedActiveName = Store.loadActiveTemplateName(context)
        val loadedRoutines = Store.loadRoutines(context) ?: loadedTemplates.find { it.name == loadedActiveName }?.routines ?: Store.defaultRoutines()
        val loadedHistory = Store.loadHistory(context)

        templates.clear()
        templates.addAll(loadedTemplates)
        activeTemplateName = loadedActiveName
        routines.putAll(loadedRoutines)
        history.putAll(loadedHistory)
        loaded = true
    }

    fun persistRoutines() {
        scope.launch {
            Store.saveRoutines(context, routines.toMap())
            val idx = templates.indexOfFirst { it.name == activeTemplateName }
            if (idx >= 0) {
                templates[idx] = WorkoutTemplate(activeTemplateName, routines.toMap())
                Store.saveTemplates(context, templates.toList())
            }
        }
    }

    fun persistTemplates() = scope.launch { Store.saveTemplates(context, templates.toList()) }
    fun persistHistory() = scope.launch { Store.saveHistory(context, history.toMap()) }

    val today = remember { LocalDate.now() }
    val todayKey = remember(today) { routineKeyForDate(today) }
    val todayStr = remember(today) { fmt(today) }

    Scaffold(
        containerColor = AppTheme.card,
        bottomBar = {
            NavigationBar(
                containerColor = AppTheme.card,
                tonalElevation = 0.dp,
                modifier = Modifier.height(80.dp)
            ) {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.scrollToPage(0) } },
                    icon = { Icon(Icons.Default.Checklist, null, modifier = Modifier.size(26.dp)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppTheme.text,
                        unselectedIconColor = AppTheme.muted,
                        indicatorColor = AppTheme.hover
                    )
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.scrollToPage(1) } },
                    icon = { Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(26.dp)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppTheme.text,
                        unselectedIconColor = AppTheme.muted,
                        indicatorColor = AppTheme.hover
                    )
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 2,
                    onClick = { scope.launch { pagerState.scrollToPage(2) } },
                    icon = { Icon(Icons.Outlined.Timer, null, modifier = Modifier.size(26.dp)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AppTheme.text,
                        unselectedIconColor = AppTheme.muted,
                        indicatorColor = AppTheme.hover
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (!loaded) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppTheme.primary)
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> RoutinesTab(
                            routines = routines,
                            history = history,
                            todayKey = todayKey,
                            formattedToday = todayStr,
                            templates = templates,
                            activeTemplateName = activeTemplateName,
                            onSelectTemplate = { name ->
                                activeTemplateName = name
                                scope.launch { Store.saveActiveTemplateName(context, name) }
                                val t = templates.find { it.name == name }
                                if (t != null) {
                                    routines.clear()
                                    routines.putAll(t.routines)
                                    persistRoutines()
                                }
                            },
                            onCreateTemplate = { newName, newRoutines ->
                                val newT = WorkoutTemplate(newName, newRoutines)
                                templates.add(newT)
                                persistTemplates()
                                activeTemplateName = newName
                                scope.launch { Store.saveActiveTemplateName(context, newName) }
                                routines.clear()
                                routines.putAll(newRoutines)
                                persistRoutines()
                            },
                            onDeleteTemplate = { nameToDelete ->
                                if (templates.size > 1) {
                                    val idx = templates.indexOfFirst { it.name == nameToDelete }
                                    if (idx >= 0) {
                                        templates.removeAt(idx)
                                        persistTemplates()
                                        if (activeTemplateName == nameToDelete) {
                                            val next = templates.first()
                                            activeTemplateName = next.name
                                            scope.launch { Store.saveActiveTemplateName(context, next.name) }
                                            routines.clear()
                                            routines.putAll(next.routines)
                                            persistRoutines()
                                        }
                                    }
                                }
                            },
                            onUpdateTemplate = { oldName, updatedTemplate ->
                                val idx = templates.indexOfFirst { it.name == oldName }
                                if (idx >= 0) {
                                    templates[idx] = updatedTemplate
                                    persistTemplates()
                                    if (activeTemplateName == oldName) {
                                        activeTemplateName = updatedTemplate.name
                                        scope.launch { Store.saveActiveTemplateName(context, updatedTemplate.name) }
                                        routines.clear()
                                        routines.putAll(updatedTemplate.routines)
                                        persistRoutines()
                                    }
                                }
                            },
                            onRoutineUpdated = { key, routine ->
                                routines[key] = routine
                                persistRoutines()
                            },
                            onSwapRoutines = { fromIndex, toIndex ->
                                val keyFrom = WEEK_DAYS[fromIndex].key
                                val keyTo = WEEK_DAYS[toIndex].key
                                val temp = routines[keyFrom] ?: Routine("Empty")
                                routines[keyFrom] = routines[keyTo] ?: Routine("Empty")
                                routines[keyTo] = temp
                                persistRoutines()
                            },
                            onCompleteToday = { key, routine ->
                                history[todayStr] = WorkoutHistory(todayStr, key, routine.title, routine.exercises)
                                persistHistory()
                            },
                            onDeleteWorkout = { dateKey ->
                                history.remove(dateKey)
                                persistHistory()
                            },
                            onAdvancedChange = onAdvancedChange,
                            onUnitChange = onUnitChange
                        )
                        1 -> ConstancyTab(
                            history = history,
                            routines = routines,
                            templates = templates,
                            onSaveWorkout = { item ->
                                history[item.date] = item
                                persistHistory()
                            },
                            onDeleteWorkout = { dateKey ->
                                history.remove(dateKey)
                                persistHistory()
                            }
                        )
                        2 -> StopwatchTab(
                            timeInMillis = stopwatchTimeInMillis,
                            isRunning = stopwatchIsRunning,
                            onTimeChanged = { stopwatchTimeInMillis = it },
                            onRunningChanged = { stopwatchIsRunning = it }
                        )
                    }
                }
            }
        }
    }
}