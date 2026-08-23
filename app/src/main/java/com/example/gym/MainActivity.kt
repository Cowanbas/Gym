package com.example.gym

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.activity.enableEdgeToEdge
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            GymTheme {
                MainHomeScreen()
            }
        }
    }
}

object AppTheme {
    val bg = Color(0xFF161616)
    val card = Color(0xFF161616)
    val border = Color(0xFF2B2B2B)
    val hover = Color(0xFF222222)
    val text = Color(0xFFFFFFFF)
    val muted = Color(0xFF9E9E9E)
    val primary = Color(0xFF757575)
}

@Composable
fun GymTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = AppTheme.bg,
            surface = AppTheme.card,
            primary = AppTheme.primary,
            onBackground = AppTheme.text,
            onSurface = AppTheme.text
        ),
        content = content
    )
}

data class Exercise(
    val id: String = System.nanoTime().toString(),
    val name: String = "",
    val sets: Int = 0,
    val reps: Int = 0,
    val weight: Double = 0.0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("sets", sets); put("reps", reps); put("weight", weight)
    }

    companion object {
        fun fromJson(json: JSONObject) = Exercise(
            id = json.optString("id", System.nanoTime().toString()),
            name = json.optString("name", ""),
            sets = json.optInt("sets", 0),
            reps = json.optInt("reps", 0),
            weight = json.optDouble("weight", 0.0)
        )
    }
}

data class Routine(
    val title: String = "Workout",
    val exercises: List<Exercise> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("title", title)
        put("exercises", JSONArray().also { arr -> exercises.forEach { arr.put(it.toJson()) } })
    }

    companion object {
        fun fromJson(json: JSONObject): Routine {
            val arr = json.optJSONArray("exercises")
            val list = if (arr != null) List(arr.length()) { Exercise.fromJson(arr.getJSONObject(it)) } else emptyList()
            return Routine(json.optString("title", "Workout"), list)
        }
    }
}

data class WorkoutTemplate(
    val name: String,
    val routines: Map<String, Routine>
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        val obj = JSONObject()
        routines.forEach { (k, v) -> obj.put(k, v.toJson()) }
        put("routines", obj)
    }

    companion object {
        fun fromJson(json: JSONObject): WorkoutTemplate {
            val name = json.optString("name", "Template A")
            val routinesObj = json.optJSONObject("routines") ?: JSONObject()
            val map = buildMap {
                routinesObj.keys().forEach { k -> put(k, Routine.fromJson(routinesObj.getJSONObject(k))) }
            }
            return WorkoutTemplate(name, map.ifEmpty { Store.defaultRoutines() })
        }
    }
}

data class WorkoutHistory(
    val date: String,
    val routineKey: String = "",
    val routineTitle: String = "Workout",
    val exercises: List<Exercise> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("date", date)
        put("routineKey", routineKey)
        put("routineTitle", routineTitle)
        put("exercises", JSONArray().also { arr -> exercises.forEach { arr.put(it.toJson()) } })
    }

    companion object {
        fun fromJson(json: JSONObject): WorkoutHistory {
            val arr = json.optJSONArray("exercises")
            val list = if (arr != null) List(arr.length()) { Exercise.fromJson(arr.getJSONObject(it)) } else emptyList()
            return WorkoutHistory(
                date = json.optString("date", ""),
                routineKey = json.optString("routineKey", ""),
                routineTitle = json.optString("routineTitle", "Workout"),
                exercises = list
            )
        }
    }
}

object Store {
    private const val PREFS = "gym_store"
    private const val KEY_ROUTINES = "routines"
    private const val KEY_HISTORY = "history"
    private const val KEY_TEMPLATES = "templates"
    private const val KEY_ACTIVE_TEMPLATE = "active_template"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    suspend fun loadRoutines(ctx: Context): Map<String, Routine>? = withContext(Dispatchers.IO) {
        val raw = prefs(ctx).getString(KEY_ROUTINES, null) ?: return@withContext null
        runCatching {
            val obj = JSONObject(raw)
            buildMap { obj.keys().forEach { k -> put(k, Routine.fromJson(obj.getJSONObject(k))) } }
        }.getOrNull()
    }

    suspend fun loadTemplates(ctx: Context): List<WorkoutTemplate>? = withContext(Dispatchers.IO) {
        val raw = prefs(ctx).getString(KEY_TEMPLATES, null) ?: return@withContext null
        runCatching {
            val arr = JSONArray(raw)
            List(arr.length()) { WorkoutTemplate.fromJson(arr.getJSONObject(it)) }.ifEmpty { null }
        }.getOrNull()
    }

    suspend fun loadActiveTemplateName(ctx: Context): String = withContext(Dispatchers.IO) {
        prefs(ctx).getString(KEY_ACTIVE_TEMPLATE, "Template A") ?: "Template A"
    }

    suspend fun loadHistory(ctx: Context): Map<String, WorkoutHistory> = withContext(Dispatchers.IO) {
        val raw = prefs(ctx).getString(KEY_HISTORY, null) ?: return@withContext emptyMap()
        runCatching {
            val obj = JSONObject(raw)
            buildMap { obj.keys().forEach { k -> put(k, WorkoutHistory.fromJson(obj.getJSONObject(k))) } }
        }.getOrDefault(emptyMap())
    }

    suspend fun saveRoutines(ctx: Context, routines: Map<String, Routine>) = withContext(Dispatchers.IO) {
        val obj = JSONObject()
        routines.forEach { (k, v) -> obj.put(k, v.toJson()) }
        prefs(ctx).edit { putString(KEY_ROUTINES, obj.toString()) }
    }

    suspend fun saveTemplates(ctx: Context, templates: List<WorkoutTemplate>) = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        templates.forEach { arr.put(it.toJson()) }
        prefs(ctx).edit { putString(KEY_TEMPLATES, arr.toString()) }
    }

    suspend fun saveActiveTemplateName(ctx: Context, name: String) = withContext(Dispatchers.IO) {
        prefs(ctx).edit { putString(KEY_ACTIVE_TEMPLATE, name) }
    }

    suspend fun saveHistory(ctx: Context, history: Map<String, WorkoutHistory>) = withContext(Dispatchers.IO) {
        val obj = JSONObject()
        history.forEach { (k, v) -> obj.put(k, v.toJson()) }
        prefs(ctx).edit { putString(KEY_HISTORY, obj.toString()) }
    }

    @Suppress("SpellCheckingInspection")
    fun defaultRoutines(): Map<String, Routine> = mapOf(
        "monday" to Routine("Empty", emptyList()),
        "tuesday" to Routine("Empty", emptyList()),
        "wednesday" to Routine("Empty", emptyList()),
        "thursday" to Routine("Empty", emptyList()),
        "friday" to Routine("Empty", emptyList()),
        "saturday" to Routine("Empty", emptyList()),
        "sunday" to Routine("Empty", emptyList())
    )

    fun defaultTemplates(): List<WorkoutTemplate> = listOf(
        WorkoutTemplate("Template A", defaultRoutines()),
        WorkoutTemplate("Template B", defaultRoutines())
    )
}

val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
fun fmt(date: LocalDate): String = date.format(DATE_FMT)

@Suppress("SpellCheckingInspection")
val DAY_KEYS = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

data class WeekDay(val key: String, val name: String, val short: String)

@Suppress("SpellCheckingInspection")
val WEEK_DAYS = listOf(
    WeekDay("monday", "Monday", "Mon"),
    WeekDay("tuesday", "Tuesday", "Tue"),
    WeekDay("wednesday", "Wednesday", "Wed"),
    WeekDay("thursday", "Thursday", "Thu"),
    WeekDay("friday", "Friday", "Fri"),
    WeekDay("saturday", "Saturday", "Sat"),
    WeekDay("sunday", "Sunday", "Sun")
)

fun routineKeyForDate(date: LocalDate): String = DAY_KEYS[date.dayOfWeek.value - 1]

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainHomeScreen() {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 2 })

    val routines = remember { mutableStateMapOf<String, Routine>() }
    val templates = remember { mutableStateListOf<WorkoutTemplate>() }
    var activeTemplateName by remember { mutableStateOf("Template A") }
    val history = remember { mutableStateMapOf<String, WorkoutHistory>() }
    var loaded by remember { mutableStateOf(false) }

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

    val todayKey = routineKeyForDate(LocalDate.now())
    val todayStr = fmt(LocalDate.now())

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
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                            }
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
                    }
                }
            }
        }
    }
}

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
    onCompleteToday: (String, Routine) -> Unit
) {
    var editingKey by remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showCreateModal by remember { mutableStateOf(false) }
    var showCopyModal by remember { mutableStateOf(false) }
    var editingTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var showSettingsModal by remember { mutableStateOf(false) }
    var showTemplatesModal by remember { mutableStateOf(false) }

    val todayShort = WEEK_DAYS.first { it.key == todayKey }.short
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
                                text = { Text("Settings", color = AppTheme.text, fontWeight = FontWeight.Normal) },
                                onClick = {
                                    menuExpanded = false
                                    showSettingsModal = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Templates", color = AppTheme.text, fontWeight = FontWeight.Normal) },
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
        val isToday = key == todayKey
        RoutineEditModal(
            routine = routine,
            isToday = isToday,
            onDismiss = { editingKey = null },
            onSave = { updated ->
                onRoutineUpdated(key, updated)
                editingKey = null
            },
            onComplete = { updated ->
                onRoutineUpdated(key, updated)
                onCompleteToday(key, updated)
                editingKey = null
            }
        )
    }

    if (showSettingsModal) {
        SettingsModal(onDismiss = { showSettingsModal = false })
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsModal(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val prefs = context.getSharedPreferences("gym_store", Context.MODE_PRIVATE)
                    val allEntries = prefs.all
                    val jsonObject = JSONObject()
                    allEntries.forEach { (key, value) ->
                        if (value != null) {
                            jsonObject.put(key, value)
                        }
                    }
                    outputStream.write(jsonObject.toString().toByteArray())
                    outputStream.flush()
                }
                android.widget.Toast.makeText(context, "Settings exported successfully", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Export failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                val jsonStr = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                } ?: throw Exception("O arquivo está vazio ou não pôde ser lido.")

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
                android.widget.Toast.makeText(context, "Settings imported successfully", android.widget.Toast.LENGTH_SHORT).show()

                var ctx = context
                while (ctx is android.content.ContextWrapper) {
                    if (ctx is android.app.Activity) {
                        ctx.recreate()
                        break
                    }
                    ctx = ctx.baseContext
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Import failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = AppTheme.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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

            Card(
                onClick = {
                    exportLauncher.launch("gym_settings_${LocalDate.now()}.json")
                },
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

            Card(
                onClick = {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                },
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
        }
    }
}

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

            items(templates) { template ->
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
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
                            Text(
                                template.name,
                                color = AppTheme.text,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        IconButton(
                            onClick = {
                                onDismiss()
                                onEditTemplateClick(template)
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Configure template", tint = AppTheme.muted, modifier = Modifier.size(16.dp))
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
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp),
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
                items(templates) { template ->
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
        mutableStateMapOf<String, Routine>().apply {
            putAll(template.routines)
        }
    }
    var editingDayKey by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismiss, containerColor = AppTheme.card) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp),
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
            onDismiss = { editingDayKey = null },
            onSave = { updated ->
                routinesMap[dayKey] = updated
                editingDayKey = null
            },
            onComplete = { updated ->
                routinesMap[dayKey] = updated
                editingDayKey = null
            }
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
        mutableStateMapOf<String, Routine>().apply {
            putAll(Store.defaultRoutines())
        }
    }
    var editingDayKey by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismiss, containerColor = AppTheme.card) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp),
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
            onDismiss = { editingDayKey = null },
            onSave = { updated ->
                routinesMap[dayKey] = updated
                editingDayKey = null
            },
            onComplete = { updated ->
                routinesMap[dayKey] = updated
                editingDayKey = null
            }
        )
    }
}

@Composable
fun LazyItemScope.ExerciseCardItem(
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

    Card(
        colors = CardDefaults.cardColors(containerColor = AppTheme.hover),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, AppTheme.border),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .animateItem()
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumberInputField("Sets", exercise.sets.toString(), Modifier.weight(1f)) {
                        onChanged(exercise.copy(sets = it.toIntOrNull() ?: 0))
                    }
                    NumberInputField("Reps", exercise.reps.toString(), Modifier.weight(1f)) {
                        onChanged(exercise.copy(reps = it.toIntOrNull() ?: 0))
                    }
                    NumberInputField("Weight (kg)", trimNumber(exercise.weight), Modifier.weight(1f), decimal = true) {
                        onChanged(exercise.copy(weight = it.replace(',', '.').toDoubleOrNull() ?: 0.0))
                    }
                }
            }
        }
    }
}

fun trimNumber(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

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
        Text(label, fontSize = 12.sp, color = AppTheme.muted)
        Spacer(Modifier.height(2.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditModal(
    routine: Routine,
    isToday: Boolean,
    onDismiss: () -> Unit,
    onSave: (Routine) -> Unit,
    onComplete: (Routine) -> Unit
) {
    var title by remember { mutableStateOf(routine.title) }
    val exercises = remember { routine.exercises.toMutableStateList() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismiss, containerColor = AppTheme.card) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp),
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
                        Icon(Icons.Default.FitnessCenter, null, tint = AppTheme.text, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
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
                    if (isToday) {
                        Button(
                            onClick = { onComplete(Routine(title.ifBlank { "Empty" }, exercises.toList())) },
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Finish", color = Color.White, fontWeight = FontWeight.Normal)
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

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
                    onAdd = {
                        showRoutinePickerForDate = dateKey
                    },
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
                    exercises = routine.exercises.map { it.copy(id = System.nanoTime().toString() + it.id) }
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
                    exercises = base?.exercises?.map { it.copy(id = System.nanoTime().toString() + it.id) } ?: emptyList()
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
    val date = LocalDate.parse(dateKey, DATE_FMT)
    val defaultKey = routineKeyForDate(date)
    val defaultRoutine = currentRoutines[defaultKey]
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismiss, containerColor = AppTheme.card) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.80f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add Workout on $dateKey", fontSize = 16.sp, fontWeight = FontWeight.Normal, color = AppTheme.text)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = AppTheme.muted)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Choose a workout or create one..", fontSize = 12.sp, color = AppTheme.muted)
                Spacer(Modifier.height(8.dp))

                Card(
                    onClick = { onSelectDefault() },
                    colors = CardDefaults.cardColors(containerColor = AppTheme.hover),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AppTheme.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Today, null, tint = AppTheme.text, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Default Day Workout", fontWeight = FontWeight.Normal, fontSize = 13.sp, color = AppTheme.text)
                            Text(defaultRoutine?.title ?: "(empty)", fontSize = 11.sp, color = AppTheme.muted)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = AppTheme.muted, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(6.dp))

                Card(
                    onClick = { onSelectEmpty() },
                    colors = CardDefaults.cardColors(containerColor = AppTheme.hover),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AppTheme.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, null, tint = AppTheme.text, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Blank Workout", fontWeight = FontWeight.Normal, fontSize = 13.sp, color = AppTheme.text)
                            Text("Create manually from scratch", fontSize = 11.sp, color = AppTheme.muted)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = AppTheme.muted, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = AppTheme.border)
                Spacer(Modifier.height(4.dp))
                Text("Or choose a ready-made template.", fontSize = 11.sp, fontWeight = FontWeight.Normal, color = AppTheme.muted)
            }

            items(templates) { template ->
                Spacer(Modifier.height(4.dp))
                Text(template.name, fontSize = 12.sp, fontWeight = FontWeight.Normal, color = AppTheme.text)
                Spacer(Modifier.height(4.dp))
                DAY_KEYS.forEach { dayKey ->
                    val routine = template.routines[dayKey] ?: return@forEach
                    val dayName = WEEK_DAYS.find { it.key == dayKey }?.name ?: dayKey
                    Card(
                        onClick = { onSelectRoutine(routine) },
                        colors = CardDefaults.cardColors(containerColor = AppTheme.hover),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AppTheme.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dayName, fontSize = 11.sp, color = AppTheme.muted)
                                Text(routine.title, fontSize = 13.sp, fontWeight = FontWeight.Normal, color = AppTheme.text)
                                Text("${routine.exercises.size} exercises", fontSize = 10.sp, color = AppTheme.muted)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = AppTheme.muted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
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
    val startMonday = remember(currentMonday) {
        currentMonday.minusWeeks(15)
    }

    val weeks = remember(startMonday) {
        (0..15).map { weekIndex ->
            val weekStart = startMonday.plusWeeks(weekIndex.toLong())
            val daysMap = (0..6).associate { dayOffset ->
                val date = weekStart.plusDays(dayOffset.toLong())
                date.dayOfWeek.value to date
            }
            listOf(
                daysMap[7],
                daysMap[6],
                daysMap[5],
                daysMap[4],
                daysMap[3],
                daysMap[2],
                daysMap[1]
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
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
    val weeksData by remember(monthlyRef) {
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
                    val d = currentMonday.plusDays(dayOffset.toLong())
                    history.containsKey(fmt(d))
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
                    modifier = Modifier
                        .width(22.dp)
                        .height(55.dp),
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
    val firstDay = selectedMonth.atDay(1)
    val daysInMonth = selectedMonth.lengthOfMonth()
    val startingWeekday = (firstDay.dayOfWeek.value - 1 + 7) % 7
    val weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S")
    val today = fmt(LocalDate.now())

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
                        val dateKey = fmt(selectedMonth.atDay(dayNum))
                        val hasWorkout = history.containsKey(dateKey)
                        val isSelected = selectedDateKey == dateKey
                        val isToday = dateKey == today
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
                            Text("$dayNum", fontSize = 11.sp, fontWeight = FontWeight.Normal,
                                color = Color.White)
                        }
                    } else {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

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
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Day $dateKey", fontWeight = FontWeight.Normal, fontSize = 12.sp, color = AppTheme.text)
                IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = AppTheme.muted, modifier = Modifier.size(15.dp))
                }
            }
            HorizontalDivider(color = AppTheme.border)
            Spacer(Modifier.height(8.dp))

            if (historyItem == null) {
                Text("No workout registered on this day.", color = AppTheme.muted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add workout on this day", color = Color.White, fontWeight = FontWeight.Normal)
                }
            } else {
                Text(historyItem.routineTitle, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = AppTheme.text)
                Spacer(Modifier.height(6.dp))
                historyItem.exercises.forEach { ex ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ex.name.ifBlank { "Exercise" }, fontSize = 11.sp, color = AppTheme.text)
                        val wStr = trimNumber(ex.weight)
                        val formattedWeight = if (wStr.length == 1 && wStr[0].isDigit()) "0$wStr" else wStr
                        Text("${ex.sets}x${ex.reps} • ${formattedWeight}kg", fontSize = 11.sp, color = AppTheme.muted)
                    }
                }
                if (historyItem.exercises.isEmpty()) {
                    Text("No exercises in this record.", fontSize = 11.sp, color = AppTheme.muted)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onEdit(historyItem) },
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.hover),
                        border = BorderStroke(1.dp, AppTheme.border),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, null, tint = AppTheme.text, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit", color = AppTheme.text)
                    }
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.hover),
                        border = BorderStroke(1.dp, AppTheme.border),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, null, tint = AppTheme.text, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete", color = AppTheme.text)
                    }
                }
            }
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
    val exercises = remember { item.exercises.toMutableStateList() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismiss, containerColor = AppTheme.card) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Workout on ${item.date}", fontSize = 11.sp, color = AppTheme.muted)
                Spacer(Modifier.height(4.dp))
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
                        Icon(Icons.Default.FitnessCenter, null, tint = AppTheme.text, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
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

            item {
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = {
                        onSave(item.copy(routineTitle = title.ifBlank { "Empty" }, exercises = exercises.toList()))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save record", color = Color.White, fontWeight = FontWeight.Normal)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}