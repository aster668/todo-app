package com.example.taskmanager.ui.focus

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas as DrawCanvas
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskmanager.data.*
import com.example.taskmanager.ui.common.*
import com.example.taskmanager.util.focusTimeLabel
import kotlinx.coroutines.delay
import kotlin.math.sin
@Composable
fun FocusScreen(
    durationMinutes: Int, seconds: Int, running: Boolean, sessions: Int, tasks: List<TaskItem>, selectedTaskId: Long?, selectedStepId: Long?,
    onSelectTask: (Long?, Long?) -> Unit, canChangeTask: Boolean, companion: String, prize: FocusPrize?, modifier: Modifier,
    onDurationChange: (Int) -> Unit, onStart: () -> Unit, onPause: () -> Unit, onReset: () -> Unit
) {
    var showDurationPicker by remember { mutableStateOf(false) }
    var showTaskPicker by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableStateOf(durationMinutes.toString()) }
    val selectedTask = tasks.firstOrNull { it.id == selectedTaskId }
    val selectedStep = selectedTask?.steps?.firstOrNull { it.id == selectedStepId }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(18.dp))
        Text("小咪的专注时光", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("让这只小猫陪你专心完成眼前的事", color = Color.Gray, modifier = Modifier.padding(top = 5.dp))
        OutlinedButton(
            onClick = { showTaskPicker = true },
            enabled = canChangeTask && !running,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("${if (selectedTask == null) "不关联任务" else "关联：${selectedTask.title}${selectedStep?.let { " · ${it.title}" } ?: ""}"}${if (running) "（专注中）" else if (canChangeTask) " · 点击选择" else "（本段已锁定）"}", maxLines = 1)
        }
        Card(
            Modifier.fillMaxWidth().padding(top = 18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5EE)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                DeskCatPet(companion = companion, focusing = running, modifier = Modifier.size(156.dp))
                Text(if (running) "小咪正在陪你专注" else "小咪在这里等你", fontWeight = FontWeight.Bold, color = Ink)
                Text(if (running) "慢慢来，先专注眼前这一小段。" else "开始后，它会陪你守住这段时间。", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(top = 3.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { if (durationMinutes == 0) 0f else seconds / (durationMinutes * 60f) }, modifier = Modifier.fillMaxSize(), strokeWidth = 12.dp, color = Teal, trackColor = Color(0xFFDDE8E5))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%02d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = Ink)
                Text(if (running) "正在专注" else "准备开始", color = Color.Gray)
            }
        }
        TextButton(onClick = { customMinutes = durationMinutes.toString(); showDurationPicker = true }, enabled = !running, modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.Default.Tune, null); Spacer(Modifier.width(6.dp)); Text("专注时长：$durationMinutes 分钟（点此设置）")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = if (running) onPause else onStart, shape = RoundedCornerShape(14.dp)) { Icon(if (running) Icons.Default.Pause else Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text(if (running) "暂停" else "开始专注") }
            OutlinedButton(onClick = onReset, shape = RoundedCornerShape(14.dp)) { Text("重置") }
        }
        Text("已完成 $sessions 次 · 专注可以赚取不同的猫咪食物", color = Color.Gray, modifier = Modifier.padding(top = 16.dp))
        prize?.let {
            Card(Modifier.fillMaxWidth().padding(top = 14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE4F3EF)), shape = RoundedCornerShape(18.dp)) {
                Text("完成得真棒！获得 ${it.foodEmoji} ${it.foodName} × ${it.amount}，${it.pet} ${it.petName}也来陪你啦。", Modifier.padding(16.dp), color = Ink)
            }
        }
    }
    if (showTaskPicker) {
        AlertDialog(
            onDismissRequest = { showTaskPicker = false },
            title = { Text("这段专注要做什么？") },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    TextButton(onClick = { onSelectTask(null, null); showTaskPicker = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("不关联任务", modifier = Modifier.fillMaxWidth(), color = if (selectedTaskId == null) Teal else Ink)
                    }
                    tasks.sortedBy { it.done }.forEach { task ->
                        TextButton(onClick = { onSelectTask(task.id, null); showTaskPicker = false }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth()) {
                                Text("${if (task.done) "✓ " else ""}${task.title}（整个任务）", color = if (task.id == selectedTaskId && selectedStepId == null) Teal else Ink, maxLines = 1)
                                Text("累计专注 ${focusTimeLabel(task.focusMinutes)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                        task.steps.forEach { step ->
                            TextButton(onClick = { onSelectTask(task.id, step.id); showTaskPicker = false }, modifier = Modifier.fillMaxWidth().padding(start = 18.dp)) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text("↳ ${step.title}", color = if (task.id == selectedTaskId && step.id == selectedStepId) Teal else Ink, maxLines = 1)
                                    Text("小任务专注 ${focusTimeLabel(step.focusMinutes)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                    if (tasks.isEmpty()) Text("还没有任务，可以先去计划页添加一个。", color = Color.Gray, modifier = Modifier.padding(12.dp))
                }
            },
            confirmButton = { TextButton(onClick = { showTaskPicker = false }) { Text("完成") } }
        )
    }
    if (showDurationPicker) {
        AlertDialog(
            onDismissRequest = { showDurationPicker = false },
            title = { Text("设置专注时长") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("选择一个节奏，完成后会获得对应的猫咪食物和伙伴。", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    listOf(15, 25, 45, 60).forEach { value ->
                        FilterChip(selected = durationMinutes == value, onClick = { onDurationChange(value); showDurationPicker = false }, label = { Text("$value 分钟 · ${prizeFor(value).foodEmoji} ${prizeFor(value).foodName} ×${prizeFor(value).amount} + ${prizeFor(value).petName}") })
                    }
                    OutlinedTextField(customMinutes, { customMinutes = it.filter(Char::isDigit).take(3) }, label = { Text("自定义分钟（1–180）") }, singleLine = true)
                }
            },
            confirmButton = { TextButton(onClick = { val value = customMinutes.toIntOrNull(); if (value != null && value in 1..180) { onDurationChange(value); showDurationPicker = false } }) { Text("应用") } },
            dismissButton = { TextButton(onClick = { showDurationPicker = false }) { Text("取消") } }
        )
    }
}

@Composable
fun DeskCatPet(companion: String, focusing: Boolean, eating: Boolean = false, modifier: Modifier = Modifier) {
    val motion = rememberInfiniteTransition(label = "catMotion")
    val phase by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = if (eating) 420 else 2600, easing = LinearEasing), RepeatMode.Restart),
        label = "tailAndBreath"
    )
    var blink by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(if (focusing) 4200 else 3000)
            blink = true
            delay(130)
            blink = false
        }
    }
    val collarColor = when (companion) {
        "🦊" -> Color(0xFFE99B61)
        "🦉" -> Color(0xFF9C8BC9)
        "🐰" -> Color(0xFFE994A8)
        else -> Color(0xFF63BDB1)
    }

    DrawCanvas(modifier) {
        val unit = size.minDimension / 160f
        val centerX = size.width / 2f
        val bob = kotlin.math.sin(phase * 2f * Math.PI).toFloat() * unit * when { eating -> 5f; focusing -> 2.5f; else -> 1f }
        val tailSwing = kotlin.math.sin(phase * 2f * Math.PI).toFloat() * unit * 12f
        fun p(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(centerX + x * unit, y * unit + bob)
        val fur = Color(0xFFFFD9B8)
        val furLight = Color(0xFFFFE9D7)
        val outline = Color(0xFF6A5148)

        // Ground shadow and swishing tail, drawn behind the cat.
        drawOval(Color(0x1A493A37), androidx.compose.ui.geometry.Offset(centerX - 46f * unit, 140f * unit + bob), androidx.compose.ui.geometry.Size(92f * unit, 12f * unit))
        val tail = androidx.compose.ui.graphics.Path().apply {
            moveTo(p(30f, 112f).x, p(30f, 112f).y)
            cubicTo(p(61f, 111f).x, p(61f, 111f).y, p(66f + tailSwing / unit, 77f).x, p(66f + tailSwing / unit, 77f).y, p(49f + tailSwing / unit, 76f).x, p(49f + tailSwing / unit, 76f).y)
        }
        drawPath(tail, fur, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 13f * unit, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawPath(tail, outline.copy(alpha = 0.45f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f * unit, cap = androidx.compose.ui.graphics.StrokeCap.Round))

        // Sitting body, belly and front paws.
        drawOval(fur, androidx.compose.ui.geometry.Offset(centerX - 38f * unit, 78f * unit + bob), androidx.compose.ui.geometry.Size(76f * unit, 66f * unit))
        drawOval(furLight, androidx.compose.ui.geometry.Offset(centerX - 22f * unit, 94f * unit + bob), androidx.compose.ui.geometry.Size(44f * unit, 47f * unit))
        drawOval(fur, androidx.compose.ui.geometry.Offset(centerX - 31f * unit, 126f * unit + bob), androidx.compose.ui.geometry.Size(30f * unit, 17f * unit))
        drawOval(fur, androidx.compose.ui.geometry.Offset(centerX + 1f * unit, 126f * unit + bob), androidx.compose.ui.geometry.Size(30f * unit, 17f * unit))

        // Ears with soft pink inner triangles.
        val leftEar = androidx.compose.ui.graphics.Path().apply {
            moveTo(p(-31f, 49f).x, p(-31f, 49f).y); quadraticTo(p(-43f, 15f).x, p(-43f, 15f).y, p(-8f, 32f).x, p(-8f, 32f).y); close()
        }
        val rightEar = androidx.compose.ui.graphics.Path().apply {
            moveTo(p(31f, 49f).x, p(31f, 49f).y); quadraticTo(p(43f, 15f).x, p(43f, 15f).y, p(8f, 32f).x, p(8f, 32f).y); close()
        }
        drawPath(leftEar, fur); drawPath(rightEar, fur)
        val leftInner = androidx.compose.ui.graphics.Path().apply { moveTo(p(-29f, 42f).x, p(-29f, 42f).y); quadraticTo(p(-35f, 24f).x, p(-35f, 24f).y, p(-15f, 34f).x, p(-15f, 34f).y); close() }
        val rightInner = androidx.compose.ui.graphics.Path().apply { moveTo(p(29f, 42f).x, p(29f, 42f).y); quadraticTo(p(35f, 24f).x, p(35f, 24f).y, p(15f, 34f).x, p(15f, 34f).y); close() }
        drawPath(leftInner, Color(0xFFF2A7A6)); drawPath(rightInner, Color(0xFFF2A7A6))

        // Round head and face.
        drawCircle(fur, radius = 37f * unit, center = p(0f, 56f))
        drawCircle(furLight.copy(alpha = 0.65f), radius = 35f * unit, center = p(0f, 57f))
        drawCircle(Color(0xFFFFB8B4).copy(alpha = 0.55f), radius = 5f * unit, center = p(-22f, 68f))
        drawCircle(Color(0xFFFFB8B4).copy(alpha = 0.55f), radius = 5f * unit, center = p(22f, 68f))
        if (blink) {
            drawLine(outline, p(-18f, 55f), p(-8f, 55f), strokeWidth = 2.6f * unit, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(outline, p(8f, 55f), p(18f, 55f), strokeWidth = 2.6f * unit, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        } else {
            drawOval(outline, androidx.compose.ui.geometry.Offset(centerX - 17f * unit, 51f * unit + bob), androidx.compose.ui.geometry.Size(5f * unit, 9f * unit))
            drawOval(outline, androidx.compose.ui.geometry.Offset(centerX + 12f * unit, 51f * unit + bob), androidx.compose.ui.geometry.Size(5f * unit, 9f * unit))
            drawCircle(Color.White, radius = 1.5f * unit, center = p(-15f, 53f))
            drawCircle(Color.White, radius = 1.5f * unit, center = p(14f, 53f))
        }
        val nose = androidx.compose.ui.graphics.Path().apply { moveTo(p(-4f, 65f).x, p(-4f, 65f).y); quadraticTo(p(0f, 70f).x, p(0f, 70f).y, p(4f, 65f).x, p(4f, 65f).y); close() }
        drawPath(nose, Color(0xFFCB777E))
        if (eating) {
            drawOval(outline, androidx.compose.ui.geometry.Offset(centerX - 4f * unit, 69f * unit + bob), androidx.compose.ui.geometry.Size(8f * unit, 10f * unit))
        } else {
            val mouthLeft = androidx.compose.ui.graphics.Path().apply { moveTo(p(0f, 69f).x, p(0f, 69f).y); quadraticTo(p(-2f, 76f).x, p(-2f, 76f).y, p(-8f, 73f).x, p(-8f, 73f).y) }
            val mouthRight = androidx.compose.ui.graphics.Path().apply { moveTo(p(0f, 69f).x, p(0f, 69f).y); quadraticTo(p(2f, 76f).x, p(2f, 76f).y, p(8f, 73f).x, p(8f, 73f).y) }
            drawPath(mouthLeft, outline, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8f * unit, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            drawPath(mouthRight, outline, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8f * unit, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        listOf(-1f, 1f).forEach { side ->
            drawLine(outline.copy(alpha = 0.65f), p(side * 22f, 66f), p(side * 37f, 62f), strokeWidth = 1f * unit)
            drawLine(outline.copy(alpha = 0.65f), p(side * 22f, 70f), p(side * 38f, 72f), strokeWidth = 1f * unit)
        }

        // A small collar makes collected companions feel like accessories for this same desk pet.
        val collar = androidx.compose.ui.graphics.Path().apply { moveTo(p(-23f, 91f).x, p(-23f, 91f).y); quadraticTo(p(0f, 102f).x, p(0f, 102f).y, p(23f, 91f).x, p(23f, 91f).y) }
        drawPath(collar, collarColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f * unit, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawCircle(Color(0xFFFFE39A), radius = 3.5f * unit, center = p(0f, 99f))
    }
}

