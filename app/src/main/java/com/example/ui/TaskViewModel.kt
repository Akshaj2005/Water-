package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskViewModel(
    application: Application,
    private val repository: TaskRepository
) : AndroidViewModel(application) {

    // Particle splash triggers for water bubble explosions
    private val _completedSplashTrigger = MutableSharedFlow<Unit>(replay = 0)
    val completedSplashTrigger: SharedFlow<Unit> = _completedSplashTrigger.asSharedFlow()

    // Freezer sound / ice visual effect triggers
    private val _frostTrigger = MutableSharedFlow<Unit>(replay = 0)
    val frostTrigger: SharedFlow<Unit> = _frostTrigger.asSharedFlow()

    // New task creation sound effect Trigger
    private val _taskCreatedTrigger = MutableSharedFlow<Unit>(replay = 0)
    val taskCreatedTrigger: SharedFlow<Unit> = _taskCreatedTrigger.asSharedFlow()

    // Banner message notifications
    private val _bannerFlow = MutableSharedFlow<String>(replay = 0)
    val bannerFlow: SharedFlow<String> = _bannerFlow.asSharedFlow()

    // Central state flow for full list of tasks
    val allTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTask(title: String, description: String, priority: String) {
        viewModelScope.launch {
            repository.addTask(title, description, priority)
            _taskCreatedTrigger.emit(Unit)
            _bannerFlow.emit("New droplet inflated! 💧")
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val nextCompleted = !task.isCompleted
            val updatedTask = task.copy(isCompleted = nextCompleted)
            repository.updateTask(updatedTask)
            
            if (nextCompleted) {
                // Froze into ice!
                _frostTrigger.emit(Unit)
                _bannerFlow.emit("Droplet frozen into an ice sphere! ❄️🧊")
            } else {
                // Melted back into water!
                _completedSplashTrigger.emit(Unit)
                _bannerFlow.emit("Ice melt back into water droplet! 💧")
            }
        }
    }

    fun updateTaskPosition(id: Int, x: Float, y: Float) {
        viewModelScope.launch {
            repository.updateTaskPosition(id, x, y)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
            _completedSplashTrigger.emit(Unit)
            _bannerFlow.emit("Droplet popped out of existence! 🫧")
        }
    }

    fun clearAllTasks() {
        viewModelScope.launch {
            repository.clearAll()
            _bannerFlow.emit("All tasks evaporated! 💨")
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = TaskDatabase.getDatabase(application)
                    val repository = TaskRepository(database.taskDao())
                    return TaskViewModel(application, repository) as T
                }
            }
        }
    }
}
