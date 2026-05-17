package com.carlmanning.carlsbrain.ui.screens.capture

import androidx.lifecycle.ViewModel
import com.carlmanning.carlsbrain.domain.model.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CaptureUiState(
    val text: String = "",
    val selectedBucketId: Long? = null,
    val selectedPriority: Priority = Priority.NORMAL,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

class CaptureViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    fun onTextChange(text: String) {
        _uiState.update { it.copy(text = text) }
    }

    fun onBucketSelected(bucketId: Long?) {
        _uiState.update { it.copy(selectedBucketId = bucketId) }
    }

    fun onPrioritySelected(priority: Priority) {
        _uiState.update { it.copy(selectedPriority = priority) }
    }

    fun save(onComplete: () -> Unit) {
        // Persistence via repository will be wired up in Phase 2
        _uiState.update { it.copy(isSaved = true) }
        onComplete()
    }
}
