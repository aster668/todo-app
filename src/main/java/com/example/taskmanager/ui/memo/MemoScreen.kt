package com.example.taskmanager.ui.memo

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskmanager.data.MemoNote
import com.example.taskmanager.ui.common.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(notes: List<MemoNote>, modifier: Modifier, onChange: (List<MemoNote>) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var randomReview by remember { mutableStateOf<MemoNote?>(null) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var draft by remember { mutableStateOf("") }
    var tagDraft by remember { mutableStateOf("灵感") }
    var imageDraft by remember { mutableStateOf<String?>(null) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            imageDraft = uri.toString()
        }
    }
    val tags = notes.map { it.tag }.filter { it.isNotBlank() }.distinct().sorted()
    val filteredNotes = notes.filter { note ->
        (selectedTag == null || note.tag == selectedTag) &&
            (selectedDate == null || note.date == selectedDate) &&
            (query.isBlank() || note.content.contains(query.trim(), true) || note.tag.contains(query.trim(), true))
    }.sortedByDescending { it.date }
    val weekDates = (6 downTo 0).map { offset ->
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }.time
    }
    val weekDateKeys = weekDates.map { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it) }

    fun saveDraftAndClose() {
        if (draft.isNotBlank()) {
            val old = notes.firstOrNull { it.id == editingId }
            val saved = MemoNote(
                editingId ?: System.currentTimeMillis(),
                draft.trim(),
                tagDraft.trim().ifBlank { "灵感" },
                old?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                imageDraft
            )
            onChange(if (old == null) listOf(saved) + notes else notes.map { if (it.id == old.id) saved else it })
        }
        showEditor = false
    }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("灵感收集", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
                Text("把想法先收好，之后还能找回来", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { randomReview = notes.randomOrNull() }, enabled = notes.isNotEmpty()) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Text("回顾", modifier = Modifier.padding(start = 4.dp))
            }
        }

        Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F0)), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("这一周的灵感", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Ink, style = MaterialTheme.typography.labelLarge)
                    Text("${notes.count { it.date in weekDateKeys }} 条", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    weekDates.forEach { date ->
                        val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
                        val count = notes.count { it.date == key }
                        val dayLabel = SimpleDateFormat("EEE", Locale.CHINA).format(date).takeLast(1)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(24.dp).background(
                                    when { count == 0 -> Color(0xFFE9E7E3); count == 1 -> Color(0xFFD8C8EE); count < 4 -> Color(0xFFB9A0DC); else -> Color(0xFF8B68BA) },
                                    RoundedCornerShape(7.dp)
                                ).clickable { selectedDate = if (selectedDate == key) null else key },
                                contentAlignment = Alignment.Center
                            ) { if (count > 0) Text(count.toString(), color = if (count > 2) Color.White else Ink, style = MaterialTheme.typography.labelSmall) }
                            Text(dayLabel, modifier = Modifier.padding(top = 3.dp), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(onClick = { showCalendar = true }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.CalendarMonth, contentDescription = "按日期查找", tint = Teal) }
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = if (query.isNotEmpty()) ({ IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "清除搜索") } }) else null,
            placeholder = { Text("搜索灵感或标签") },
            shape = RoundedCornerShape(14.dp)
        )

        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = selectedTag == null, onClick = { selectedTag = null }, label = { Text("全部") }, modifier = Modifier.padding(end = 6.dp))
            tags.forEach { tag -> FilterChip(selected = selectedTag == tag, onClick = { selectedTag = if (selectedTag == tag) null else tag }, label = { Text(tag) }, modifier = Modifier.padding(end = 6.dp)) }
            if (selectedDate != null) TextButton(onClick = { selectedDate = null }) { Text("清除日期") }
        }

        Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (selectedDate != null) "${selectedDate} 的记录" else "我的纸条", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Ink)
            Text("${filteredNotes.size} 条", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }

        if (filteredNotes.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (notes.isEmpty()) "写下第一条灵感，让它有个温柔的归处。" else "没有找到匹配的纸条", color = Color.Gray)
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 10.dp)) {
                items(filteredNotes, key = { it.id }) { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            editingId = note.id; draft = note.content; tagDraft = note.tag; imageDraft = note.imageUri; showEditor = true
                        },
                        colors = CardDefaults.cardColors(containerColor = listOf(Color(0xFFFFF8EF), Color(0xFFF9F1FA), Color(0xFFF1F7F1))[((note.id % 3).toInt())]),
                        shape = RoundedCornerShape(15.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8E0D8))
                    ) {
                        Column(Modifier.fillMaxWidth().padding(13.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AssistChip(onClick = { selectedTag = note.tag }, label = { Text("#${note.tag}") }, modifier = Modifier.height(30.dp))
                                Spacer(Modifier.weight(1f))
                                Text(note.date, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                IconButton(onClick = { onChange(notes.filterNot { it.id == note.id }) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.DeleteOutline, "删除纸条", tint = Color.Gray, modifier = Modifier.size(18.dp)) }
                            }
                            Text(note.content, modifier = Modifier.padding(top = 5.dp), color = Ink, style = MaterialTheme.typography.bodyMedium, maxLines = 5)
                            note.imageUri?.let { MemoImage(it, Modifier.fillMaxWidth().height(130.dp).padding(top = 8.dp)) }
                        }
                    }
                }
            }
        }

        Button(
            onClick = { editingId = null; draft = ""; tagDraft = "灵感"; imageDraft = null; showEditor = true },
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Icon(Icons.Default.Add, null); Text("写一张灵感纸条", modifier = Modifier.padding(start = 6.dp)) }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { saveDraftAndClose() },
            title = { Text(if (editingId == null) "新灵感纸条" else "编辑纸条") },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(draft, { draft = it }, modifier = Modifier.fillMaxWidth(), label = { Text("记下你的想法") }, minLines = 3)
                    OutlinedTextField(tagDraft, { tagDraft = it }, modifier = Modifier.fillMaxWidth(), label = { Text("自定义标签") }, supportingText = { Text("可以输入任意标签；留空时会使用“灵感”") }, singleLine = true)
                    val existingTags = notes.map { it.tag.trim() }.filter { it.isNotBlank() }.distinct().sorted()
                    if (existingTags.isNotEmpty()) {
                        Text("已有标签 · 点选复用，也可以继续输入新标签", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            existingTags.forEach { tag ->
                                FilterChip(selected = tagDraft.trim() == tag, onClick = { tagDraft = tag }, label = { Text(tag) })
                            }
                        }
                    }
                    OutlinedButton(onClick = { imagePicker.launch(arrayOf("image/*")) }) { Icon(Icons.Default.Image, null); Text("添加图片", modifier = Modifier.padding(start = 6.dp)) }
                    imageDraft?.let { MemoImage(it, Modifier.fillMaxWidth().height(140.dp)) }
                    if (imageDraft != null) TextButton(onClick = { imageDraft = null }) { Text("移除图片") }
                }
            },
            confirmButton = { TextButton(onClick = { saveDraftAndClose() }, enabled = draft.isNotBlank()) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("取消") } }
        )
    }

    if (showCalendar) {
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = { TextButton(onClick = {
                selectedDate = datePickerState.selectedDateMillis?.let { millis ->
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(Date(millis))
                }
                showCalendar = false
            }) { Text("查看") } },
            dismissButton = { TextButton(onClick = { showCalendar = false }) { Text("取消") } }
        ) { DatePicker(state = datePickerState, showModeToggle = false) }
    }

    randomReview?.let { note ->
        AlertDialog(
            onDismissRequest = { randomReview = null },
            title = { Text("偶然翻到一张旧纸条 ✨") },
            text = { Column { Text("#${note.tag}  ·  ${note.date}", color = Teal, style = MaterialTheme.typography.labelMedium); Text(note.content, modifier = Modifier.padding(top = 10.dp), color = Ink); note.imageUri?.let { MemoImage(it, Modifier.fillMaxWidth().height(150.dp).padding(top = 10.dp)) } } },
            confirmButton = { TextButton(onClick = { randomReview = null }) { Text("收好") } },
            dismissButton = { TextButton(onClick = { randomReview = notes.randomOrNull() }) { Text("再翻一张") } }
        )
    }
}

@Composable
private fun MemoImage(uriString: String, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, uriString) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uriString))?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            }.getOrNull()
        }
    }
    if (bitmap != null) Image(bitmap = bitmap!!, contentDescription = "备忘图片", modifier = modifier, contentScale = ContentScale.Crop)
}

