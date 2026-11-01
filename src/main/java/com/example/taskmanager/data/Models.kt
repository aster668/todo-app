package com.example.taskmanager.data

data class TaskItem(val id: Long, val title: String, val due: String, val priority: String, val done: Boolean, val steps: List<StepItem>, val focusMinutes: Int = 0)
data class StepItem(val title: String, val done: Boolean = false, val id: Long = System.nanoTime(), val focusMinutes: Int = 0)
data class FocusSession(val id: Long, val taskId: Long?, val stepId: Long?, val taskTitle: String?, val stepTitle: String?, val minutes: Int, val completedAt: Long)
data class MemoNote(val id: Long, val content: String, val tag: String, val date: String, val imageUri: String? = null)
