package com.example.taskmanager.data

data class FocusPrize(val foodKey: String, val foodEmoji: String, val foodName: String, val amount: Int, val pet: String, val petName: String)

fun prizeFor(minutes: Int): FocusPrize = when {
    minutes >= 60 -> FocusPrize("salmon", "🐟", "三文鱼", 2, "🦉", "猫头鹰")
    minutes >= 45 -> FocusPrize("salmon", "🐟", "三文鱼", 1, "🦊", "小狐狸")
    minutes >= 25 -> FocusPrize("can", "🥫", "猫罐头", 1, "🐱", "小猫")
    else -> FocusPrize("treat", "🍗", "猫条", 1, "🐰", "小兔")
}