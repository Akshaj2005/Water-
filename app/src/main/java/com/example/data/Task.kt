package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false, // true means it turned into an Ice Sphere
    val priority: String = "Medium",  // "High", "Medium", "Low"
    val createdAt: Long = System.currentTimeMillis(),
    val sizeMultiplier: Float = 1.0f,
    // Store positions so after restarts they remain custom
    val x: Float = 0f,
    val y: Float = 0f
)
