package com.carlmanning.carlsbrain.ui.screens.todos

import androidx.lifecycle.ViewModel
import com.carlmanning.carlsbrain.domain.model.Priority
import com.carlmanning.carlsbrain.domain.model.Todo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TodosUiState(
    val todos: List<Todo> = emptyList(),
    val isLoading: Boolean = false,
    val selectedPriority: Priority? = null
)

class TodosViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TodosUiState())
    val uiState: StateFlow<TodosUiState> = _uiState.asStateFlow()

    fun onPriorityFilterSelected(priority: Priority?) {
        _uiState.update { it.copy(selectedPriority = priority) }
    }
}
