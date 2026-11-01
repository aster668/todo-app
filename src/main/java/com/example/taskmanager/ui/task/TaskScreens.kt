package com.example.taskmanager.ui.task

import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas as DrawCanvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.taskmanager.data.*
import com.example.taskmanager.ui.common.Canvas
import com.example.taskmanager.ui.common.*
import com.example.taskmanager.util.focusTimeLabel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PlanScreen(
    tasks: List<TaskItem>,
    focusHistory: List<FocusSession>,
    modifier: Modifier,
    onToggle: (TaskItem) -> Unit,
    onDelete: (TaskItem) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onMoveTask: (Long, Long) -> Unit,
    onBreakdownTask: (Long) -> Unit
) {
    var selectedShelf by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var dateFilter by remember { mutableStateOf("TODAY") }
    var showQuadrantView by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedActionTaskId by remember { mutableStateOf<Long?>(null) }
    var draggingTaskId by remember { mutableStateOf<Long?>(null) }
    var draggingOffset by remember { mutableStateOf(Offset.Zero) }
    var dragStartBounds by remember { mutableStateOf<Rect?>(null) }
    val paperBounds = remember { mutableStateMapOf<Long, Rect>() }
    val selectedActionTask = tasks.firstOrNull { it.id == selectedActionTaskId }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = todayPickerMillis())
    val todayKey = todayDateKey()
    val allActiveTasks = tasks.filterNot { it.done }
    val allCompletedTasks = tasks.filter { it.done }.sortedByDescending { normalizedTaskDate(it.due) }
    val activeTasks = allActiveTasks.filter { matchesDateFilter(it.due, dateFilter, todayKey) }
    val completedTasks = allCompletedTasks.filter { matchesDateFilter(it.due, dateFilter, todayKey) }
    val visibleTasks = (if (selectedShelf == 0) activeTasks else completedTasks)
        .filter { it.title.contains(query.trim(), ignoreCase = true) || it.steps.any { step -> step.title.contains(query.trim(), ignoreCase = true) } }

    Column((if (selectedActionTask != null) modifier.blur(14.dp) else modifier).fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("任务清单", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
                Text(SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(Date()), color = Color.Gray)
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text(if (selectedShelf == 1) "快速查找已完成任务" else "搜索任务或小步骤") },
            trailingIcon = if (query.isNotEmpty()) ({ IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, contentDescription = "清除搜索") } }) else null,
            shape = RoundedCornerShape(14.dp)
        )
        TabRow(selectedTabIndex = selectedShelf, modifier = Modifier.padding(top = 8.dp), containerColor = Canvas) {
            Tab(selected = selectedShelf == 0, onClick = { selectedShelf = 0 }, text = { Text("挂绳上 · ${activeTasks.size}") })
            Tab(selected = selectedShelf == 1, onClick = { selectedShelf = 1 }, text = { Text("完成签 · ${completedTasks.size}") })
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                listOf("ALL" to "全部", "TODAY" to "今天").forEach { (key, label) ->
                    FilterChip(selected = dateFilter == key, onClick = { dateFilter = key }, label = { Text(label) }, modifier = Modifier.padding(end = 6.dp))
                }
                val exactDateSelected = dateFilter.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
                FilterChip(
                    selected = exactDateSelected,
                    onClick = { showDatePicker = true },
                    label = { Text(if (exactDateSelected) taskDateLabel(dateFilter) else "选日期") },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp)) }
                )
            }
            IconButton(
                onClick = { showQuadrantView = !showQuadrantView },
                modifier = Modifier.padding(start = 4.dp).background(if (showQuadrantView) Color(0xFFE8DFF8) else Color.Transparent, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    if (showQuadrantView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                    contentDescription = if (showQuadrantView) "切换到任务卡片视图" else "按任务四象限归类",
                    tint = Teal
                )
            }
        }
        if (tasks.isEmpty()) {
            EmptyHint("还没有任务，点右下角的 + 挂上第一张任务卡")
        } else if (visibleTasks.isEmpty()) {
            EmptyHint(when {
                query.isNotBlank() -> "没有找到匹配的任务"
                dateFilter != "ALL" -> "这个日期范围里没有任务"
                selectedShelf == 0 -> "绳子上暂时没有待办，完成得真棒"
                else -> "完成的任务会串到这里"
            })
        } else {
            if (selectedShelf == 1) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "✦  签上的任务 ${visibleTasks.size}${if (query.isNotBlank()) " / ${completedTasks.size}" else ""}",
                        fontWeight = FontWeight.SemiBold,
                        color = Ink
                    )
                    Spacer(Modifier.weight(1f))
                    Text(if (query.isNotBlank()) "搜索结果" else "点“取下”可恢复", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
            Box(Modifier.weight(1f)) {
                if (showQuadrantView) {
                    QuadrantTaskBoard(
                        tasks = visibleTasks,
                        onOpenTask = { selectedActionTaskId = it },
                        onToggle = onToggle
                    )
                } else if (selectedShelf == 1) {
                    DrawCanvas(Modifier.fillMaxSize()) {
                        val x = 13.dp.toPx()
                        drawLine(Color(0xFF766D60), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 3.dp.toPx())
                    }
                }
                if (!showQuadrantView) LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 92.dp)) {
                    val columns = if (selectedShelf == 0) 2 else 1
                    items(visibleTasks.chunked(columns), key = { it.first().id }) { rowTasks ->
                        if (selectedShelf == 0) {
                            Box(Modifier.fillMaxWidth()) {
                                DrawCanvas(Modifier.matchParentSize()) {
                                    val y = 9.dp.toPx()
                                    val cord = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(0f, y)
                                        quadraticTo(size.width * 0.5f, y + 12.dp.toPx(), size.width, y)
                                    }
                                    drawPath(cord, Color(0xFFB9AA8D), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
                                }
                                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    rowTasks.forEachIndexed { index, task ->
                                        Box(Modifier.weight(1f)) {
                                            TaskPaperCard(
                                                task = task,
                                                hanging = true,
                                                dragging = draggingTaskId == task.id,
                                                dragOffset = if (draggingTaskId == task.id) draggingOffset else Offset.Zero,
                                                supportsDrag = selectedShelf == 0 && !showQuadrantView,
                                                onBounds = { paperBounds[task.id] = it },
                                                onDragStart = {
                                                    draggingTaskId = task.id
                                                    draggingOffset = Offset.Zero
                                                    dragStartBounds = paperBounds[task.id]
                                                },
                                                onDrag = { delta -> draggingOffset += delta },
                                                onDragEnd = {
                                                    val startBounds = dragStartBounds
                                                    val draggedId = draggingTaskId
                                                    if (startBounds != null && draggedId != null && draggingOffset != Offset.Zero) {
                                                        val movedCenter = startBounds.center + draggingOffset
                                                        val target = visibleTasks.asSequence()
                                                            .filter { it.id != draggedId }
                                                            .mapNotNull { candidate -> paperBounds[candidate.id]?.let { candidate to it.center } }
                                                            .minByOrNull { (_, center) ->
                                                                val dx = center.x - movedCenter.x
                                                                val dy = center.y - movedCenter.y
                                                                dx * dx + dy * dy
                                                            }?.first
                                                        if (target != null) onMoveTask(draggedId, target.id)
                                                    }
                                                    draggingTaskId = null
                                                    draggingOffset = Offset.Zero
                                                    dragStartBounds = null
                                                },
                                                onOpen = { selectedActionTaskId = task.id },
                                                onBreakdown = { onBreakdownTask(task.id) },
                                                onToggle = { onToggle(task) },
                                                onDelete = { onDelete(task) },
                                            )
                                            Box(
                                                Modifier.align(Alignment.TopCenter).offset(y = (-16).dp).width(10.dp).height(27.dp)
                                                    .background(Color(0xFF9A9991), RoundedCornerShape(3.dp)),
                                                contentAlignment = Alignment.Center
                                            ) { Box(Modifier.width(2.dp).fillMaxHeight().background(Color(0xFFD7D4C8))) }
                                        }
                                    }
                                    if (rowTasks.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        } else {
                            rowTasks.forEach { task ->
                                Box(Modifier.fillMaxWidth()) {
                                    Surface(Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size(11.dp), shape = RoundedCornerShape(50), color = Color(0xFF766D60), border = androidx.compose.foundation.BorderStroke(2.dp, Canvas)) {}
                                    CompletedTaskSlip(
                                        task = task,
                                        onOpen = { selectedActionTaskId = task.id },
                                        onTakeOff = { onToggle(task) },
                                        onDelete = { onDelete(task) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedActionTask?.let { task ->
        TaskActionsDialog(
            task = task,
            history = focusHistory.filter { it.taskId == task.id }.sortedByDescending { it.completedAt },
            onDismiss = { selectedActionTaskId = null },
            onEdit = { selectedActionTaskId = null; onEditTask(task) },
            onBreakdown = { selectedActionTaskId = null; onBreakdownTask(task.id) }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { dateFilter = pickerMillisToDateKey(it) }; showDatePicker = false }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) { DatePicker(state = datePickerState, showModeToggle = false) }
    }

}

@Composable
private fun QuadrantTaskBoard(
    tasks: List<TaskItem>,
    onOpenTask: (Long) -> Unit,
    onToggle: (TaskItem) -> Unit
) {
    val quadrants = listOf(
        Triple("重要且紧急", "优先马上处理", Color(0xFFFFE4D8)),
        Triple("重要不紧急", "安排时间稳步完成", Color(0xFFFFF1C7)),
        Triple("紧急但不重要", "尽量简化或委托", Color(0xFFDDF4F8)),
        Triple("不重要不紧急", "减少投入或暂缓", Color(0xFFECE3FA))
    )
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        quadrants.chunked(2).forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { columnIndex, (title, hint, color) ->
                    val quadrantIndex = rowIndex * 2 + columnIndex
                    val quadrantTasks = tasks.filter { taskQuadrantIndex(it.priority) == quadrantIndex }
                    Card(
                        modifier = Modifier.weight(1f).height(220.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = color),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE3C96E)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(Modifier.fillMaxSize().padding(10.dp)) {
                            Text(title, style = MaterialTheme.typography.titleSmall, color = Ink, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("$hint · ${quadrantTasks.size}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF776F61), maxLines = 1)
                            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = Color(0x55B9AA8D))
                            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                if (quadrantTasks.isEmpty()) {
                                    Text("把任务放在这里", style = MaterialTheme.typography.labelSmall, color = Color(0x88776F61))
                                } else {
                                    quadrantTasks.forEach { task ->
                                        Row(
                                            Modifier.fillMaxWidth()
                                                .background(Color.White.copy(alpha = 0.76f), RoundedCornerShape(8.dp))
                                                .clickable { onOpenTask(task.id) }
                                                .padding(start = 7.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(task.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = Ink, maxLines = 2)
                                            IconButton(onClick = { onToggle(task) }, modifier = Modifier.size(30.dp)) {
                                                Icon(
                                                    if (task.done) Icons.Default.Replay else Icons.Default.CheckCircle,
                                                    contentDescription = if (task.done) "恢复任务" else "完成任务",
                                                    tint = Teal,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun taskQuadrantIndex(priority: String): Int = when (priority) {
    "重要且紧急", "高" -> 0
    "重要不紧急", "普通" -> 1
    "紧急但不重要" -> 2
    "不重要不紧急", "低" -> 3
    else -> 1
}

@Composable
private fun TaskPaperCard(
    task: TaskItem,
    hanging: Boolean,
    dragging: Boolean,
    dragOffset: Offset,
    supportsDrag: Boolean,
    modifier: Modifier = Modifier,
    onBounds: (Rect) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onOpen: () -> Unit,
    onBreakdown: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val completedSteps = task.steps.count { it.done }
    Card(
        modifier = modifier.fillMaxWidth()
            .onGloballyPositioned { onBounds(it.boundsInRoot()) }
            .graphicsLayer {
                rotationZ = if (hanging) if (task.id % 2L == 0L) -1.1f else 1.1f else 0f
                translationX = dragOffset.x
                translationY = dragOffset.y
                shadowElevation = if (dragging) 18.dp.toPx() else 0f
            }
            .zIndex(if (dragging) 1f else 0f)
            .then(if (supportsDrag) Modifier.pointerInput(task.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                    onDrag = { change, delta -> change.consume(); onDrag(delta) }
                )
            } else Modifier)
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = if (task.done) Color(0xFFF0EEE7) else Color(0xFFF4EEDC)),
        shape = RoundedCornerShape(topStart = if (hanging) 3.dp else 12.dp, topEnd = if (hanging) 3.dp else 12.dp, bottomStart = 5.dp, bottomEnd = 5.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD6CFBD)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (hanging) 4.dp else 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (task.done) "DONE" else "TO DO", style = MaterialTheme.typography.labelSmall, color = Color(0xFF776F61), fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Close, "删除任务", tint = Color(0xFF817B70), modifier = Modifier.size(16.dp)) }
            }
            Text(task.title, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), fontWeight = FontWeight.Bold, color = if (task.done) Color(0xFF77736B) else Ink, style = MaterialTheme.typography.titleSmall)
            Text("${taskDateLabel(task.due)}  ·  ${task.priority}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF77736B))
            if (task.focusMinutes > 0) {
                Text("⏱ 专注 ${focusTimeLabel(task.focusMinutes)}", modifier = Modifier.padding(top = 3.dp), style = MaterialTheme.typography.labelSmall, color = Teal)
            }
            if (task.steps.isNotEmpty()) {
                Spacer(Modifier.height(7.dp))
                LinearProgressIndicator(progress = { completedSteps.toFloat() / task.steps.size }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = Teal, trackColor = Color(0xFFE2DDCF))
            }
            Spacer(Modifier.height(7.dp))
            Text(if (task.steps.isEmpty()) "还没有小任务" else "小任务  $completedSteps / ${task.steps.size}", style = MaterialTheme.typography.labelSmall, color = Teal)
            HorizontalDivider(color = Color(0xFFD6CFBD), modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onToggle, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 0.dp)) { Text(if (task.done) "取下" else "Done", style = MaterialTheme.typography.labelMedium) }
                TextButton(onClick = onBreakdown, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 0.dp)) { Text("拆解", style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}

@Composable
private fun CompletedTaskSlip(
    task: TaskItem,
    onOpen: () -> Unit,
    onTakeOff: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(start = 26.dp).clickable(onClick = onOpen),
        shape = RoundedCornerShape(5.dp),
        color = Color(0xFFEAE4D1),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB9AE91)),
        shadowElevation = 2.dp
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 5.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DragHandle, contentDescription = null, tint = Color(0xFF817965), modifier = Modifier.size(18.dp))
            Box(Modifier.padding(horizontal = 8.dp).size(34.dp).background(Color(0xFFD9D1B9), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF403E37), modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f).padding(end = 4.dp)) {
                Text(task.title, maxLines = 1, fontWeight = FontWeight.Bold, color = Ink, style = MaterialTheme.typography.labelLarge)
                Text("${taskDateLabel(task.due)}  ·  ${task.priority}${if (task.focusMinutes > 0) "  ·  专注 ${focusTimeLabel(task.focusMinutes)}" else ""}", maxLines = 1, color = Color(0xFF77736B), style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = onTakeOff, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("取下", style = MaterialTheme.typography.labelSmall) }
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Close, contentDescription = "删除任务", tint = Color(0xFF817B70), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun TaskActionsDialog(
    task: TaskItem,
    history: List<FocusSession>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onBreakdown: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.91f).heightIn(max = 680.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xEEF9F7F0),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.85f)),
            shadowElevation = 24.dp
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("任务卡片", style = MaterialTheme.typography.labelMedium, color = Teal, fontWeight = FontWeight.Bold)
                        Text(task.title, modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleLarge, color = Ink, fontWeight = FontWeight.Bold)
                        Text("${taskDateLabel(task.due)} · ${task.priority}", modifier = Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color(0xFF77736B))
                    }
                }

                OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("编辑任务")
                }
                Text("长按挂绳上的纸卡并拖动，即可调整任务位置。", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDDF1EC).copy(alpha = 0.88f))
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = Teal, modifier = Modifier.size(30.dp))
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("累计专注", style = MaterialTheme.typography.labelMedium, color = Color(0xFF58736D))
                            Text(focusTimeLabel(task.focusMinutes), style = MaterialTheme.typography.headlineSmall, color = Ink, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.weight(1f))
                        Text("${history.size} 次", style = MaterialTheme.typography.labelLarge, color = Teal, fontWeight = FontWeight.SemiBold)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("专注历史", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("最近 ${minOf(history.size, 4)} 次", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    if (history.isEmpty()) {
                        Text(
                            "专注历史从本次更新起记录；此前的投入仍会计入累计时长。",
                            modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.62f), RoundedCornerShape(14.dp)).padding(13.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    } else {
                        history.take(4).forEach { session ->
                            Row(
                                Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.68f), RoundedCornerShape(13.dp)).padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(session.stepTitle?.let { "小任务 · $it" } ?: "整个任务", style = MaterialTheme.typography.labelMedium, color = Ink, maxLines = 1)
                                    Text(SimpleDateFormat("M月d日  HH:mm", Locale.CHINA).format(Date(session.completedAt)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Text("${session.minutes} 分钟", color = Teal, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                TextButton(onClick = onBreakdown, modifier = Modifier.align(Alignment.End)) { Text("查看小任务") }
            }
        }
    }
}

@Composable
fun TaskBreakdownScreen(
    task: TaskItem,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onToggleParent: () -> Unit,
    onToggleStep: (Int) -> Unit,
    onEditStep: (Int, String) -> Unit,
    onAddStep: (String) -> Unit,
    onDeleteStep: (Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var editingStepIndex by remember { mutableStateOf<Int?>(null) }
    val completed = task.steps.count { it.done }

    Column(modifier.fillMaxSize().background(Canvas)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
            Column(Modifier.weight(1f)) {
                Text("任务思维导图", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
                Text("已专注 ${focusTimeLabel(task.focusMinutes)} · 把目标变成下一步能做的小行动", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF82C9F4)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF67B5E1))
                ) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(58.dp).background(Color(0xFFB5E4FA), RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                            Text("☺", style = MaterialTheme.typography.headlineLarge, color = Ink)
                        }
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text("大任务", style = MaterialTheme.typography.labelMedium, color = Ink, fontWeight = FontWeight.Bold)
                            Text(task.title, modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleLarge, color = Ink, fontWeight = FontWeight.Bold)
                            Text("${taskDateLabel(task.due)}  ·  ${task.priority}", modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = Ink.copy(alpha = 0.72f))
                            Text("小任务完成 $completed / ${task.steps.size}", modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.labelMedium, color = Ink)
                            LinearProgressIndicator(
                                progress = { if (task.steps.isEmpty()) if (task.done) 1f else 0f else completed.toFloat() / task.steps.size },
                                modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                                color = Color(0xFF7552E8),
                                trackColor = Color(0xFFB5E4FA)
                            )
                        }
                        Checkbox(checked = task.done, onCheckedChange = { onToggleParent() }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF7552E8)))
                    }
                }
                Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.width(2.dp).height(24.dp).background(Color(0xFF7552E8)))
                        Text("↓", color = Color(0xFF7552E8), style = MaterialTheme.typography.titleMedium)
                    }
                    Box(Modifier.weight(1f))
                }
                Text("拆成几个现在就能开始的小任务", modifier = Modifier.fillMaxWidth(), color = Color(0xFF776A9A), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(10.dp))
            }

            item {
                val nodeCount = task.steps.size + 1
                val nodeWidth = 238.dp
                val nodeGap = 18.dp
                val lineColor = Color(0xFF9B80E8)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    Box(Modifier.wrapContentWidth()) {
                        DrawCanvas(Modifier.matchParentSize()) {
                            val cardWidthPx = nodeWidth.toPx()
                            val gapPx = nodeGap.toPx()
                            val centerY = 18.dp.toPx()
                            val firstX = cardWidthPx / 2
                            val lastX = firstX + (nodeCount - 1) * (cardWidthPx + gapPx)
                            if (nodeCount > 1) drawLine(lineColor, androidx.compose.ui.geometry.Offset(firstX, centerY), androidx.compose.ui.geometry.Offset(lastX, centerY), strokeWidth = 2.dp.toPx())
                            repeat(nodeCount) { index ->
                                val x = firstX + index * (cardWidthPx + gapPx)
                                drawLine(lineColor, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, 42.dp.toPx()), strokeWidth = 2.dp.toPx())
                                drawCircle(lineColor, radius = 5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, centerY))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(nodeGap)) {
                            task.steps.forEachIndexed { index, step ->
                                Column(Modifier.width(nodeWidth), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Spacer(Modifier.height(42.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth().height(190.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = if (step.done) Color(0xFFF1D9F4) else Color(0xFFEE9AF2)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD779E2)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                                    ) {
                                        Column(Modifier.fillMaxWidth().height(190.dp).padding(14.dp)) {
                                            Box(Modifier.fillMaxWidth().height(18.dp).background(Color(0xFF8655F2), RoundedCornerShape(50)))
                                            Row(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(checked = step.done, onCheckedChange = { onToggleStep(index) }, modifier = Modifier.size(42.dp), colors = CheckboxDefaults.colors(checkedColor = Teal))
                                                Text(
                                                    step.title.ifBlank { "点击编辑小任务" },
                                                    modifier = Modifier.weight(1f).fillMaxWidth().clickable { draft = step.title; editingStepIndex = index; showAddDialog = true },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (step.done) Color.Gray else Ink,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            HorizontalDivider(color = Color(0xFFD779E2))
                                            Row(Modifier.fillMaxWidth().height(38.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text("${if (step.done) "✓ 已完成" else "○ 待完成"} · ${index + 1}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = Color(0xFF725877), maxLines = 1)
                                                if (step.focusMinutes > 0) Text("⏱ ${focusTimeLabel(step.focusMinutes)}", style = MaterialTheme.typography.labelSmall, color = Teal, maxLines = 1)
                                                IconButton(onClick = { draft = step.title; editingStepIndex = index; showAddDialog = true }, modifier = Modifier.size(34.dp)) {
                                                    Icon(Icons.Default.Edit, contentDescription = "编辑小任务", tint = Color(0xFF725877), modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(onClick = { onDeleteStep(index) }, modifier = Modifier.size(34.dp)) {
                                                    Icon(Icons.Default.Close, contentDescription = "删除小任务", tint = Color(0xFF725877), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Column(Modifier.width(nodeWidth), horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(Modifier.height(42.dp))
                                OutlinedCard(
                                    onClick = { draft = ""; editingStepIndex = null; showAddDialog = true },
                                    modifier = Modifier.fillMaxWidth().height(190.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFFFF6FF)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD779E2))
                                ) {
                                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF8655F2), modifier = Modifier.size(34.dp))
                                        Text("添加小任务", color = Color(0xFF725877), modifier = Modifier.padding(top = 8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; editingStepIndex = null },
            title = { Text(if (editingStepIndex == null) "添加小任务" else "编辑小任务") },
            text = { OutlinedTextField(value = draft, onValueChange = { draft = it }, label = { Text("下一步具体做什么？") }, singleLine = true) },
            confirmButton = { TextButton(onClick = {
                if (draft.isNotBlank()) {
                    val index = editingStepIndex
                    if (index == null) onAddStep(draft.trim()) else onEditStep(index, draft.trim())
                    showAddDialog = false
                    editingStepIndex = null
                }
            }, enabled = draft.isNotBlank()) { Text(if (editingStepIndex == null) "添加" else "保存") } },
            dismissButton = { TextButton(onClick = { showAddDialog = false; editingStepIndex = null }) { Text("取消") } }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AddTaskDialog(initialTask: TaskItem? = null, onDismiss: () -> Unit, onSave: (String, String, String, List<String>) -> Unit) {
    var title by remember(initialTask?.id) { mutableStateOf(initialTask?.title ?: "") }
    val initialRange = initialTask?.due?.let { taskDateRange(it) }
    var dueStart by remember(initialTask?.id) { mutableStateOf(initialRange?.first ?: todayDateKey()) }
    var dueEnd by remember(initialTask?.id) { mutableStateOf(initialRange?.second ?: todayDateKey()) }
    var showDuePicker by remember { mutableStateOf(false) }
    val duePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialRange?.first?.let(::dateKeyToPickerMillis) ?: todayPickerMillis(),
        initialSelectedEndDateMillis = initialRange?.second?.let(::dateKeyToPickerMillis) ?: todayPickerMillis()
    )
    val priorityOptions = listOf("重要且紧急", "重要不紧急", "紧急但不重要", "不重要不紧急")
    val priorityDescriptions = mapOf(
        "重要且紧急" to "优先马上处理",
        "重要不紧急" to "安排时间稳步完成",
        "紧急但不重要" to "尽量简化或委托",
        "不重要不紧急" to "减少投入或暂缓"
    )
    var priority by remember(initialTask?.id) { mutableStateOf(initialTask?.priority?.takeIf { it in priorityOptions } ?: priorityOptions[1]) }
    var priorityMenuExpanded by remember { mutableStateOf(false) }
    var estimateHours by remember(initialTask?.id) { mutableStateOf(((initialTask?.steps?.size ?: 0).coerceAtLeast(2)).toString()) }
    var generated by remember(initialTask?.id) { mutableStateOf<List<String>>(initialTask?.steps?.map { it.title } ?: emptyList()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialTask == null) "创建任务" else "编辑任务") },
        text = {
            Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (initialTask == null) "先写清要交付的结果，计划会按任务类型和预计时长给出可修改步骤。" else "修改任务名称、时间范围、四象限等级或小任务。", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                OutlinedTextField(title, { title = it; if (initialTask == null) generated = emptyList() }, label = { Text("任务名称 / 想达成的结果") }, singleLine = true)
                OutlinedButton(onClick = { showDuePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("时间范围：${taskDateLabel("$dueStart|$dueEnd")}")
                }
                Box {
                    OutlinedButton(onClick = { priorityMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("任务四象限：$priority", modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "选择任务象限")
                    }
                    DropdownMenu(expanded = priorityMenuExpanded, onDismissRequest = { priorityMenuExpanded = false }) {
                        priorityOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(option, fontWeight = FontWeight.Medium)
                                        Text(priorityDescriptions.getValue(option), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                },
                                onClick = { priority = option; priorityMenuExpanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(estimateHours, { estimateHours = it.filter(Char::isDigit).take(2) }, label = { Text("预计总时长（小时）") }, singleLine = true)
                Button(onClick = { if (title.isNotBlank()) generated = suggestSteps(title.trim(), estimateHours.toIntOrNull() ?: 2) }, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("为这个目标生成行动计划") }
                if (generated.isNotEmpty()) {
                    Text("行动计划（点文字可改，点 × 可删）", fontWeight = FontWeight.SemiBold)
                    generated.forEachIndexed { i, value ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}.", color = Teal, fontWeight = FontWeight.Bold)
                            OutlinedTextField(value, { v -> generated = generated.toMutableList().also { it[i] = v } }, modifier = Modifier.weight(1f), singleLine = false)
                            IconButton(onClick = { generated = generated.filterIndexed { index, _ -> index != i } }) { Icon(Icons.Default.Close, "移除步骤") }
                        }
                    }
                    TextButton(onClick = { generated = generated + "" }) { Text("＋ 自己添加一步") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (title.isNotBlank()) onSave(title.trim(), "$dueStart|$dueEnd", priority, generated.filter { it.isNotBlank() }) }) { Text(if (initialTask == null) "保存任务" else "保存修改") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
    if (showDuePicker) {
        DatePickerDialog(
            onDismissRequest = { showDuePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    duePickerState.selectedStartDateMillis?.let { dueStart = pickerMillisToDateKey(it) }
                    duePickerState.selectedEndDateMillis?.let { dueEnd = pickerMillisToDateKey(it) } ?: run { dueEnd = dueStart }
                    if (dueEnd < dueStart) {
                        val earlier = dueEnd
                        dueEnd = dueStart
                        dueStart = earlier
                    }
                    showDuePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDuePicker = false }) { Text("取消") } }
        ) { DateRangePicker(state = duePickerState, showModeToggle = false) }
    }
}

private val taskDatePattern = Regex("\\d{4}-\\d{2}-\\d{2}")

private fun todayDateKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

private fun pickerMillisToDateKey(millis: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}.format(Date(millis))

private fun dateKeyToPickerMillis(dateKey: String): Long = runCatching {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(dateKey)!!.time
}.getOrDefault(todayPickerMillis())

private fun todayPickerMillis(): Long {
    val localToday = Calendar.getInstance()
    return Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).apply {
        clear()
        set(localToday.get(Calendar.YEAR), localToday.get(Calendar.MONTH), localToday.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun normalizedSingleTaskDate(due: String): String = when (due) {
    "今天" -> todayDateKey()
    "明天" -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time.let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it) }
    else -> {
        val legacyDate = Regex("(\\d{1,2})[./月](\\d{1,2})日?").matchEntire(due)
        if (legacyDate == null) due else runCatching {
            val year = Calendar.getInstance().get(Calendar.YEAR)
            val month = legacyDate.groupValues[1].toInt()
            val day = legacyDate.groupValues[2].toInt()
            Calendar.getInstance().apply { clear(); set(year, month - 1, day) }.time.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it)
            }
        }.getOrDefault(due)
    }
}

private fun taskDateRange(due: String): Pair<String, String>? {
    val parts = due.split("|", limit = 2)
    if (parts.size == 2 && taskDatePattern.matches(parts[0]) && taskDatePattern.matches(parts[1])) {
        return parts[0] to parts[1]
    }
    val singleDate = normalizedSingleTaskDate(due)
    return if (taskDatePattern.matches(singleDate)) singleDate to singleDate else null
}

private fun normalizedTaskDate(due: String): String = taskDateRange(due)?.first ?: normalizedSingleTaskDate(due)

fun taskDateLabel(dateKey: String): String {
    val range = taskDateRange(dateKey)
    if (range != null && range.first != range.second) {
        return "${shortTaskDate(range.first)}—${shortTaskDate(range.second)}"
    }
    val normalizedDate = range?.first ?: normalizedSingleTaskDate(dateKey)
    if (!taskDatePattern.matches(normalizedDate)) return dateKey
    val today = todayDateKey()
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time.let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it) }
    val parsed = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(normalizedDate) }.getOrNull()
        ?: return dateKey
    val shortDate = SimpleDateFormat("M月d日", Locale.CHINA).format(parsed)
    return when (normalizedDate) {
        today -> "今天 · $shortDate"
        tomorrow -> "明天 · $shortDate"
        else -> "$shortDate${if (normalizedDate < today) " · 已逾期" else ""}"
    }
}

private fun shortTaskDate(dateKey: String): String = runCatching {
    SimpleDateFormat("M月d日", Locale.CHINA).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey)!!)
}.getOrDefault(dateKey)

private fun matchesDateFilter(due: String, filter: String, todayKey: String): Boolean {
    if (filter == "ALL") return true
    val range = taskDateRange(due) ?: return false
    return when (filter) {
        "TODAY" -> todayKey >= range.first && todayKey <= range.second
        "OVERDUE" -> range.second < todayKey
        "UPCOMING" -> range.first > todayKey
        else -> taskDatePattern.matches(filter) && filter >= range.first && filter <= range.second
    }
}

private fun suggestSteps(title: String, hours: Int): List<String> {
    val blocks = max(1, (hours * 60) / 25)
    return when {
        listOf("考试", "学习", "复习", "背", "课程", "作业").any { title.contains(it, ignoreCase = true) } -> listOf(
            "把目标具体化：本次结束时要掌握什么？",
            "找齐资料，把内容分成 $blocks 个 25 分钟学习块",
            "完成第 1 组学习内容，并记下 3 个关键点",
            "合上资料做一次回忆或练习题，标记不会的地方",
            "整理错点，安排下一次复习时间"
        )
        listOf("论文", "报告", "方案", "演讲", "文章", "总结").any { title.contains(it, ignoreCase = true) } -> listOf(
            "确认要求、读者和提交时间，写下验收标准",
            "收集并标注至少 3 条可靠资料",
            "列出标题和每一部分要表达的要点",
            "按提纲完成初稿，不在第一遍纠结措辞",
            "核对事实、格式和缺漏，完成最终稿"
        )
        listOf("开发", "代码", "程序", "软件", "功能", "修复", "app").any { title.contains(it, ignoreCase = true) } -> listOf(
            "写出使用场景、输入和期望结果",
            "拆出最小可运行版本和验收条件",
            "实现最核心的一段流程",
            "运行一次完整流程，记录并修复问题",
            "整理使用说明，并确认任务已达到验收条件"
        )
        listOf("运动", "健身", "跑步", "锻炼").any { title.contains(it, ignoreCase = true) } -> listOf(
            "确定本次目标和可用时间",
            "准备装备，做 5 分钟热身",
            "完成主要训练（按计划分成 $blocks 组）",
            "拉伸放松，记录距离、次数或感受"
        )
        listOf("打扫", "整理", "收拾", "清洁").any { title.contains(it, ignoreCase = true) } -> listOf(
            "选定一个具体区域，拍下或写下完成标准",
            "准备收纳袋和清洁用品",
            "先分类：保留、移走、丢弃",
            "清洁并把保留物放回固定位置",
            "检查结果，记下还需要处理的一件事"
        )
        else -> listOf(
            "把“$title”改写成可以检查的完成标准",
            "列出完成它需要的材料、信息或准备",
            "用 $blocks 个 25 分钟专注块完成核心部分",
            "检查结果是否符合完成标准",
            "记录卡点，并决定下一步"
        )
    }
}

