package com.example.taskmanager.ui.pet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taskmanager.ui.common.*
import com.example.taskmanager.ui.focus.DeskCatPet
import kotlinx.coroutines.delay
@Composable
fun PetScreen(
    pets: Set<String>, companion: String, hunger: Float, treats: Int, cans: Int, salmon: Int,
    sessions: Int, totalMinutes: Int, modifier: Modifier,
    onFeed: (String) -> Unit
) {
    var feeding by remember { mutableStateOf(false) }
    LaunchedEffect(feeding) {
        if (feeding) {
            delay(1800)
            feeding = false
        }
    }
    val fullnessColor = when { hunger < 25f -> Color(0xFFD66F65); hunger < 55f -> Color(0xFFE4A14E); else -> Teal }
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("我的宠物", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("专注赚食物，回来陪小咪吃一顿。", color = Color.Gray)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E9)), shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    DeskCatPet(companion = companion, focusing = false, eating = feeding, modifier = Modifier.size(154.dp))
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(if (feeding) "小咪正在嚼嚼…" else "${petName(companion)}在等你", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
                        Text(if (pets.isEmpty()) "完成第一次专注，迎接第一位伙伴。" else if (feeding) "看起来很喜欢这份食物！" else "饱腹值会随时间慢慢下降。", modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("饱腹 ${hunger.toInt()} / 100", modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.labelLarge, color = fullnessColor, fontWeight = FontWeight.SemiBold)
                        LinearProgressIndicator(progress = { hunger / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 5.dp), color = fullnessColor, trackColor = Color(0xFFE9DED5))
                    }
                }
            }
            Text("饱腹值每小时约下降 5 点；喂食会恢复饱腹。", modifier = Modifier.padding(start = 6.dp, top = 5.dp), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
        item {
            SectionTitle("猫咪食物", "完成专注后获得，喂得越好恢复越多")
            FoodStockCard("🍗", "猫条", "恢复 18 点", treats, enabled = pets.isNotEmpty() && !feeding) { onFeed("treat"); feeding = true }
            FoodStockCard("🥫", "猫罐头", "恢复 32 点", cans, enabled = pets.isNotEmpty() && !feeding) { onFeed("can"); feeding = true }
            FoodStockCard("🐟", "三文鱼", "恢复 48 点", salmon, enabled = pets.isNotEmpty() && !feeding) { onFeed("salmon"); feeding = true }
            if (pets.isEmpty()) Text("先完成一段专注，解锁第一只宠物和食物。", modifier = Modifier.padding(8.dp), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Ink), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🐾", style = MaterialTheme.typography.headlineMedium)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("你们一起专注了 $sessions 次", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("累计 $totalMinutes 分钟 · 继续为宠物赚取食物", color = Color(0xFFD3E0E0), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodStockCard(emoji: String, name: String, effect: String, count: Int, enabled: Boolean, onFeed: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(bottom = 7.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(15.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(name, fontWeight = FontWeight.SemiBold, color = Ink)
                Text("$effect  ·  库存 $count", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
            Button(onClick = onFeed, enabled = enabled && count > 0, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) { Text("喂食") }
        }
    }
}

private fun petName(pet: String) = when (pet) { "🦉" -> "猫头鹰"; "🦊" -> "小狐狸"; "🐱" -> "小猫"; "🐰" -> "咪咪"; else -> "小伙伴" }

