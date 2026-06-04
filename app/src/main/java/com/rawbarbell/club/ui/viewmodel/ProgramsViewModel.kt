package com.rawbarbell.club.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rawbarbell.club.data.db.entities.ProgramEntity
import com.rawbarbell.club.data.repository.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgramsViewModel @Inject constructor(
    private val programRepo: ProgramRepository
) : ViewModel() {

    val allPrograms: StateFlow<List<ProgramEntity>> = programRepo.getAllPrograms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProgram: StateFlow<ProgramEntity?> = programRepo.getActiveProgram()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setActive(programId: String) {
        viewModelScope.launch { programRepo.setActiveProgram(programId) }
    }

    fun deleteProgram(programId: String) {
        viewModelScope.launch { programRepo.deleteProgram(programId) }
    }

    fun completeProgram(programId: String) {
        viewModelScope.launch { programRepo.completeProgram(programId) }
    }
}
