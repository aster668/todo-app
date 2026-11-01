package com.example.taskmanager.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PetHunger(val value: Float, val updatedAt: Long)

fun focusSessionsJson(sessions: List<FocusSession>): String = JSONArray().apply {
    sessions.forEach { session -> put(JSONObject().apply {
        put("id", session.id)
        put("taskId", session.taskId ?: JSONObject.NULL)
        put("stepId", session.stepId ?: JSONObject.NULL)
        put("taskTitle", session.taskTitle ?: JSONObject.NULL)
        put("stepTitle", session.stepTitle ?: JSONObject.NULL)
        put("minutes", session.minutes)
        put("completedAt", session.completedAt)
    }) }
}.toString()

fun loadFocusSessions(raw: String?): List<FocusSession> = try {
    if (raw.isNullOrBlank()) emptyList() else JSONArray(raw).let { array ->
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            FocusSession(
                id = item.optLong("id", System.currentTimeMillis() + index),
                taskId = if (item.isNull("taskId")) null else item.optLong("taskId"),
                stepId = if (item.isNull("stepId")) null else item.optLong("stepId"),
                taskTitle = item.optString("taskTitle").takeIf { it.isNotBlank() && it != "null" },
                stepTitle = item.optString("stepTitle").takeIf { it.isNotBlank() && it != "null" },
                minutes = item.optInt("minutes", 0),
                completedAt = item.optLong("completedAt", System.currentTimeMillis())
            )
        }
    }
} catch (_: Exception) { emptyList() }

fun memosJson(notes: List<MemoNote>): String = JSONArray().apply {
    notes.forEach { note -> put(JSONObject().apply {
        put("id", note.id)
        put("content", note.content)
        put("tag", note.tag)
        put("date", note.date)
        put("imageUri", note.imageUri)
    }) }
}.toString()

fun loadMemos(raw: String?, legacy: String?): List<MemoNote> = try {
    if (!raw.isNullOrBlank()) {
        JSONArray(raw).let { array -> (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            MemoNote(obj.optLong("id", System.currentTimeMillis() + index), obj.optString("content"), obj.optString("tag", "灵感"), obj.optString("date", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())), obj.optString("imageUri").takeIf { it.isNotBlank() && it != "null" })
        } }
    } else if (!legacy.isNullOrBlank()) {
        listOf(MemoNote(System.currentTimeMillis(), legacy, "灵感", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())))
    } else emptyList()
} catch (_: Exception) { emptyList() }

fun decayPetHunger(status: PetHunger, now: Long): PetHunger {
    val elapsedHours = (now - status.updatedAt).coerceAtLeast(0L) / 3_600_000f
    return PetHunger((status.value - elapsedHours * 5f).coerceIn(0f, 100f), now)
}

fun petHungerJson(statuses: Map<String, PetHunger>): String = JSONObject().apply {
    statuses.forEach { (pet, status) -> put(pet, JSONObject().put("value", status.value.toDouble()).put("updatedAt", status.updatedAt)) }
}.toString()

fun loadPetHunger(raw: String?): Map<String, PetHunger> = try {
    if (raw.isNullOrBlank()) emptyMap() else JSONObject(raw).let { root ->
        val result = mutableMapOf<String, PetHunger>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val pet = keys.next()
            val status = root.optJSONObject(pet) ?: continue
            result[pet] = PetHunger(status.optDouble("value", 100.0).toFloat().coerceIn(0f, 100f), status.optLong("updatedAt", System.currentTimeMillis()))
        }
        result
    }
} catch (_: Exception) { emptyMap() }

fun tasksJson(tasks: List<TaskItem>): String = JSONArray().apply {
    tasks.forEach { task -> put(JSONObject().apply {
        put("id", task.id); put("title", task.title); put("due", task.due); put("priority", task.priority); put("done", task.done)
        put("focusMinutes", task.focusMinutes)
        put("steps", JSONArray().apply { task.steps.forEach { put(JSONObject().put("id", it.id).put("title", it.title).put("done", it.done).put("focusMinutes", it.focusMinutes)) } })
    }) }
}.toString()

fun loadTasks(raw: String?): List<TaskItem> = try {
    if (raw.isNullOrBlank()) emptyList() else JSONArray(raw).let { array -> (0 until array.length()).map { i ->
        val o = array.getJSONObject(i); val s = o.optJSONArray("steps") ?: JSONArray()
        TaskItem(o.getLong("id"), o.getString("title"), o.optString("due", "今天"), o.optString("priority", "普通"), o.optBoolean("done"), (0 until s.length()).map { j -> val step = s.getJSONObject(j); StepItem(step.getString("title"), step.optBoolean("done"), step.optLong("id", System.currentTimeMillis() + j), step.optInt("focusMinutes", 0)) }, o.optInt("focusMinutes", 0))
    } }
} catch (_: Exception) { emptyList() }
