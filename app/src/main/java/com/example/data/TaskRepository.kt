package com.example.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: Flow<List<Task>> = taskDao.getAllTasksFlow()

    suspend fun getTaskById(id: Int): Task? = taskDao.getTaskById(id)

    suspend fun addTask(title: String, description: String, priority: String) {
        val multiplier = when (priority) {
            "High" -> 1.3f
            "Medium" -> 1.0f
            "Low" -> 0.75f
            else -> 1.0f
        }

        val newTask = Task(
            title = title,
            description = description,
            priority = priority,
            sizeMultiplier = multiplier,
            // Initial positions can be random floating inside the normalized space [0.1..0.9]
            x = (20..80).random().toFloat() / 100f,
            y = (20..60).random().toFloat() / 100f
        )
        taskDao.insertTask(newTask)
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    suspend fun updateTaskPosition(id: Int, x: Float, y: Float) {
        val task = taskDao.getTaskById(id)
        if (task != null) {
            taskDao.updateTask(task.copy(x = x, y = y))
        }
    }

    suspend fun clearAll() {
        taskDao.deleteAllTasks()
    }
}
