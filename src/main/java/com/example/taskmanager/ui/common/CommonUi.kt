package com.example.taskmanager.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val Ink = Color(0xFF192B3A)
val Teal = Color(0xFF168C83)
val Canvas = Color(0xFFF5F7F5)
@Composable
fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(top = 4.dp, bottom = 8.dp)) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall) }
}


@Composable
fun EmptyHint(message: String) { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text(message, color = Color.Gray) } }
