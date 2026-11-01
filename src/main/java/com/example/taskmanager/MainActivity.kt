package com.example.taskmanager

import com.example.taskmanager.data.*
import com.example.taskmanager.ui.common.*
import com.example.taskmanager.ui.task.*
import com.example.taskmanager.ui.memo.NotesScreen
import com.example.taskmanager.ui.focus.FocusScreen
import com.example.taskmanager.ui.focus.DeskCatPet
import com.example.taskmanager.ui.pet.PetScreen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max




class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TaskApp() }
    }
}

@Composable
fun TaskApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("task_manager", 0) }
    var tasks by remember { mutableStateOf(loadTasks(prefs.getString("tasks", null))) }
    var focusHistory by remember { mutableStateOf(loadFocusSessions(prefs.getString("focus_sessions", null))) }
    var memos by remember { mutableStateOf(loadMemos(prefs.getString("memos", null), prefs.getString("notes", null))) }
    var tab by remember { mutableIntStateOf(0) }
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }
    var durationMinutes by remember { mutableIntStateOf(prefs.getInt("focus_duration_min", 25)) }
    var remainingSeconds by remember { mutableIntStateOf(prefs.getInt("focus_remaining_sec", durationMinutes * 60)) }
    var focusEnd by remember { mutableLongStateOf(prefs.getLong("focus_end", 0L)) }
    var selectedFocusTaskId by remember { mutableStateOf<Long?>(prefs.getLong("focus_task_id", -1L).takeIf { it >= 0L }) }
    var selectedFocusStepId by remember { mutableStateOf<Long?>(prefs.getLong("focus_step_id", -1L).takeIf { it >= 0L }) }
    var activeFocusTaskId by remember { mutableStateOf<Long?>(prefs.getLong("active_focus_task_id", -1L).takeIf { it >= 0L }) }
    var activeFocusStepId by remember { mutableStateOf<Long?>(prefs.getLong("active_focus_step_id", -1L).takeIf { it >= 0L }) }
    var focusSessionStarted by remember { mutableStateOf(prefs.getBoolean("focus_session_started", false)) }
    var activeDuration by remember { mutableIntStateOf(prefs.getInt("focus_active_min", durationMinutes)) }
    var running by remember { mutableStateOf(focusEnd > System.currentTimeMillis()) }
    var sessions by remember { mutableIntStateOf(prefs.getInt("sessions", 0)) }
    var totalMinutes by remember { mutableIntStateOf(prefs.getInt("total_focus_minutes", 0)) }
    var streak by remember { mutableIntStateOf(prefs.getInt("streak", 0)) }
    var lastFocusDate by remember { mutableStateOf(prefs.getString("last_focus_date", "") ?: "") }
    var treatCount by remember { mutableIntStateOf(prefs.getInt("food_treats", prefs.getInt("seeds", 0))) }
    var canCount by remember { mutableIntStateOf(prefs.getInt("food_cans", 0)) }
    var salmonCount by remember { mutableIntStateOf(prefs.getInt("food_salmon", 0)) }
    var pets by remember { mutableStateOf(prefs.getStringSet("pets", emptySet())?.toSet() ?: emptySet()) }
    var companion by remember { mutableStateOf(prefs.getString("companion", null)?.takeIf { it in pets } ?: pets.firstOrNull() ?: "🐱") }
    var petHunger by remember { mutableStateOf(loadPetHunger(prefs.getString("pet_hunger", null))) }
    var showAdd by remember { mutableStateOf(false) }
    var editingTaskId by remember { mutableStateOf<Long?>(null) }
    var lastPrize by remember { mutableStateOf<FocusPrize?>(null) }

    fun saveTasks() { prefs.edit().putString("tasks", tasksJson(tasks)).apply() }
    fun saveMemos(value: List<MemoNote>) { memos = value; prefs.edit().putString("memos", memosJson(value)).apply() }
    fun savePetHunger(value: Map<String, PetHunger>) {
        petHunger = value
        prefs.edit().putString("pet_hunger", petHungerJson(value)).apply()
    }
    fun awardFocus(duration: Int) {
        val prize = prizeFor(duration)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayText = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(yesterday.time)
        streak = when (lastFocusDate) { today -> streak; yesterdayText -> streak + 1; else -> 1 }
        lastFocusDate = today
        sessions += 1
        totalMinutes += duration
        when (prize.foodKey) {
            "treat" -> treatCount += prize.amount
            "can" -> canCount += prize.amount
            "salmon" -> salmonCount += prize.amount
        }
        pets = pets + prize.pet
        if (pets.size == 1) {
            companion = prize.pet
            prefs.edit().putString("companion", companion).apply()
        }
        lastPrize = prize
        val completedAt = System.currentTimeMillis()
        val focusedTask = activeFocusTaskId?.let { taskId -> tasks.firstOrNull { it.id == taskId } }
        val focusedStep = focusedTask?.steps?.firstOrNull { it.id == activeFocusStepId }
        focusHistory = (listOf(FocusSession(completedAt, activeFocusTaskId, activeFocusStepId, focusedTask?.title, focusedStep?.title, duration, completedAt)) + focusHistory).take(300)
        activeFocusTaskId?.let { taskId ->
            tasks = tasks.map { task ->
                if (task.id == taskId) task.copy(
                    focusMinutes = task.focusMinutes + duration,
                    steps = task.steps.map { step -> if (step.id == activeFocusStepId) step.copy(focusMinutes = step.focusMinutes + duration) else step }
                ) else task
            }
            saveTasks()
        }
        prefs.edit()
            .putInt("sessions", sessions)
            .putInt("total_focus_minutes", totalMinutes)
            .putInt("streak", streak)
            .putString("last_focus_date", lastFocusDate)
            .putInt("food_treats", treatCount)
            .putInt("food_cans", canCount)
            .putInt("food_salmon", salmonCount)
            .putStringSet("pets", pets)
            .putString("focus_sessions", focusSessionsJson(focusHistory))
            .apply()
    }

    LaunchedEffect(pets) {
        val now = System.currentTimeMillis()
        val updated = petHunger.toMutableMap()
        pets.forEach { pet ->
            val saved = updated[pet]
            updated[pet] = if (saved == null) PetHunger(100f, now) else decayPetHunger(saved, now)
        }
        if (updated != petHunger) savePetHunger(updated)
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            val now = System.currentTimeMillis()
            savePetHunger(petHunger.mapValues { (_, status) -> decayPetHunger(status, now) })
        }
    }
    fun completeFocus() {
        remainingSeconds = 0
        running = false
        focusEnd = 0L
        awardFocus(activeDuration)
        activeFocusTaskId = null
        activeFocusStepId = null
        focusSessionStarted = false
        prefs.edit()
            .putInt("focus_remaining_sec", 0)
            .putLong("focus_end", 0L)
            .putLong("active_focus_task_id", -1L)
            .putLong("active_focus_step_id", -1L)
            .putBoolean("focus_session_started", false)
            .apply()
    }

    LaunchedEffect(running, focusEnd) {
        while (running) {
            remainingSeconds = max(0, ((focusEnd - System.currentTimeMillis()) / 1000L).toInt())
            if (remainingSeconds == 0) {
                completeFocus()
                break
            }
            delay(500)
        }
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = Teal, onPrimary = Color.White, background = Canvas, surface = Color.White, onSurface = Ink)) {
        Scaffold(
            containerColor = Canvas,
            bottomBar = {
                if (selectedTaskId == null) {
                NavigationBar(containerColor = Color.White) {
                    listOf("计划" to Icons.Default.CheckCircle, "备忘" to Icons.Default.EditNote, "专注" to Icons.Default.Timer, "宠物" to Icons.Default.Pets).forEachIndexed { index, pair ->
                        NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Icon(pair.second, null) }, label = { Text(pair.first) })
                    }
                }
                }
            },
            floatingActionButton = { if (tab == 0 && selectedTaskId == null) FloatingActionButton(onClick = { editingTaskId = null; showAdd = true }, containerColor = Teal, contentColor = Color.White) { Icon(Icons.Default.Add, "新增任务") } }
        ) { padding ->
            BackHandler(enabled = selectedTaskId != null) { selectedTaskId = null }
            when (tab) {
                0 -> {
                    val selectedTask = tasks.firstOrNull { it.id == selectedTaskId }
                    if (selectedTask != null) {
                        TaskBreakdownScreen(
                            task = selectedTask,
                            modifier = Modifier.padding(padding),
                            onBack = { selectedTaskId = null },
                            onToggleParent = {
                                val markDone = !selectedTask.done
                                tasks = tasks.map { if (it.id == selectedTask.id) it.copy(done = markDone, steps = if (markDone) it.steps.map { step -> step.copy(done = true) } else it.steps.map { step -> step.copy(done = false) }) else it }
                                saveTasks()
                            },
                            onToggleStep = { stepIndex ->
                                tasks = tasks.map { t ->
                                    if (t.id == selectedTask.id) {
                                        val updatedSteps = t.steps.mapIndexed { i, s -> if (i == stepIndex) s.copy(done = !s.done) else s }
                                        t.copy(steps = updatedSteps, done = updatedSteps.isNotEmpty() && updatedSteps.all { it.done })
                                    } else t
                                }
                                saveTasks()
                            },
                            onEditStep = { stepIndex, title ->
                                tasks = tasks.map { t ->
                                    if (t.id == selectedTask.id) t.copy(steps = t.steps.mapIndexed { i, step -> if (i == stepIndex) step.copy(title = title) else step }) else t
                                }
                                saveTasks()
                            },
                            onAddStep = { title -> tasks = tasks.map { if (it.id == selectedTask.id) it.copy(done = false, steps = it.steps + StepItem(title)) else it }; saveTasks() },
                            onDeleteStep = { stepIndex -> tasks = tasks.map { if (it.id == selectedTask.id) it.copy(steps = it.steps.filterIndexed { i, _ -> i != stepIndex }, done = false) else it }; saveTasks() }
                        )
                    } else {
                        selectedTaskId = null
                        PlanScreen(tasks = tasks, focusHistory = focusHistory, modifier = Modifier.padding(padding), onToggle = { task ->
                            val markDone = !task.done
                            tasks = tasks.map { if (it.id == task.id) it.copy(done = markDone, steps = if (markDone) it.steps.map { step -> step.copy(done = true) } else it.steps.map { step -> step.copy(done = false) }) else it }
                            saveTasks()
                        }, onDelete = { task -> tasks = tasks.filterNot { it.id == task.id }; saveTasks() }, onEditTask = { task -> editingTaskId = task.id; showAdd = true }, onMoveTask = { taskId, targetId ->
                            val activePositions = tasks.indices.filter { !tasks[it].done }
                            val activeTasks = activePositions.map { tasks[it] }
                            val from = activeTasks.indexOfFirst { it.id == taskId }
                            val to = activeTasks.indexOfFirst { it.id == targetId }
                            if (from >= 0 && to in activeTasks.indices) {
                                val reordered = activeTasks.toMutableList().apply { val temp = this[from]; this[from] = this[to]; this[to] = temp }
                                var activeIndex = 0
                                tasks = tasks.map { task -> if (task.done) task else reordered[activeIndex++] }
                                saveTasks()
                            }
                        }, onBreakdownTask = { selectedTaskId = it })
                    }
                }
                1 -> NotesScreen(memos, Modifier.padding(padding)) { saveMemos(it) }
                2 -> FocusScreen(
                    durationMinutes = durationMinutes,
                    seconds = remainingSeconds,
                    running = running,
                    sessions = sessions,
                    tasks = tasks,
                    selectedTaskId = selectedFocusTaskId,
                    selectedStepId = selectedFocusStepId,
                    canChangeTask = !focusSessionStarted,
                    onSelectTask = { taskId, stepId ->
                        selectedFocusTaskId = taskId
                        selectedFocusStepId = stepId
                        prefs.edit().putLong("focus_task_id", taskId ?: -1L).putLong("focus_step_id", stepId ?: -1L).apply()
                    },
                    companion = companion,
                    prize = lastPrize,
                    modifier = Modifier.padding(padding),
                    onDurationChange = { minutes ->
                        durationMinutes = minutes
                        remainingSeconds = minutes * 60
                        activeDuration = minutes
                        prefs.edit().putInt("focus_duration_min", minutes).putInt("focus_remaining_sec", remainingSeconds).putInt("focus_active_min", minutes).apply()
                    },
                    onStart = {
                        if (remainingSeconds <= 0) remainingSeconds = durationMinutes * 60
                        activeDuration = durationMinutes
                        if (activeFocusTaskId == null) {
                            activeFocusTaskId = selectedFocusTaskId
                            activeFocusStepId = selectedFocusStepId
                        }
                        focusSessionStarted = true
                        focusEnd = System.currentTimeMillis() + remainingSeconds * 1000L
                        running = true
                        lastPrize = null
                        prefs.edit().putLong("focus_end", focusEnd).putLong("active_focus_task_id", activeFocusTaskId ?: -1L).putLong("active_focus_step_id", activeFocusStepId ?: -1L).putBoolean("focus_session_started", true).putInt("focus_active_min", activeDuration).putInt("focus_remaining_sec", remainingSeconds).apply()
                    },
                    onPause = {
                        remainingSeconds = max(0, ((focusEnd - System.currentTimeMillis()) / 1000L).toInt())
                        running = false
                        focusEnd = 0L
                        prefs.edit().putLong("focus_end", 0L).putInt("focus_remaining_sec", remainingSeconds).apply()
                    },
                    onReset = {
                        running = false
                        focusEnd = 0L
                        remainingSeconds = durationMinutes * 60
                        activeDuration = durationMinutes
                        activeFocusTaskId = null
                        activeFocusStepId = null
                        focusSessionStarted = false
                        lastPrize = null
                        prefs.edit().putLong("focus_end", 0L).putLong("active_focus_task_id", -1L).putLong("active_focus_step_id", -1L).putBoolean("focus_session_started", false).putInt("focus_remaining_sec", remainingSeconds).putInt("focus_active_min", durationMinutes).apply()
                    }
                )
                else -> PetScreen(
                    pets = pets,
                    companion = companion,
                    hunger = petHunger[companion]?.value ?: 100f,
                    treats = treatCount,
                    cans = canCount,
                    salmon = salmonCount,
                    sessions = sessions,
                    totalMinutes = totalMinutes,
                    modifier = Modifier.padding(padding),
                    onFeed = { foodKey ->
                        val now = System.currentTimeMillis()
                        val current = decayPetHunger(petHunger[companion] ?: PetHunger(100f, now), now)
                        val fullnessAdded = when (foodKey) { "treat" -> 18f; "can" -> 32f; else -> 48f }
                        savePetHunger(petHunger + (companion to PetHunger((current.value + fullnessAdded).coerceAtMost(100f), now)))
                        when (foodKey) {
                            "treat" -> { treatCount -= 1; prefs.edit().putInt("food_treats", treatCount).apply() }
                            "can" -> { canCount -= 1; prefs.edit().putInt("food_cans", canCount).apply() }
                            "salmon" -> { salmonCount -= 1; prefs.edit().putInt("food_salmon", salmonCount).apply() }
                        }
                    }
                )
            }
            if (showAdd) AddTaskDialog(initialTask = tasks.firstOrNull { it.id == editingTaskId }, onDismiss = { showAdd = false; editingTaskId = null }, onSave = { title, due, priority, stepNames ->
                val editingTask = tasks.firstOrNull { it.id == editingTaskId }
                if (editingTask == null) {
                    tasks = tasks + TaskItem(System.currentTimeMillis(), title, due, priority, false, stepNames.map { StepItem(it) })
                } else {
                    tasks = tasks.map { task ->
                        if (task.id != editingTask.id) task else task.copy(
                            title = title,
                            due = due,
                            priority = priority,
                            steps = stepNames.mapIndexed { index, stepTitle -> editingTask.steps.getOrNull(index)?.copy(title = stepTitle) ?: StepItem(stepTitle) }
                        )
                    }
                }
                saveTasks(); showAdd = false; editingTaskId = null
            })
        }
    }
}

