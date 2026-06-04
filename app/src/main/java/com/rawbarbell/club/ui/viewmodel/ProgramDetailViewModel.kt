package com.rawbarbell.club.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rawbarbell.club.data.db.entities.*
import com.rawbarbell.club.data.repository.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgramDetailViewModel @Inject constructor(
    private val programRepo: ProgramRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val programId: String = checkNotNull(savedStateHandle["programId"])

    val program: StateFlow<ProgramEntity?> = programRepo.getProgramById(programId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val weeks: StateFlow<List<WeekEntity>> = programRepo.getWeeksForProgram(programId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedWeekDays = MutableStateFlow<List<DayEntity>>(emptyList())
    val selectedWeekDays: StateFlow<List<DayEntity>> = _selectedWeekDays

    fun loadDaysForWeek(weekId: String) {
        viewModelScope.launch {
            programRepo.getDaysForWeek(weekId).collectLatest {
                _selectedWeekDays.value = it.sortedBy { d -> d.dayNumber }
            }
        }
    }

    fun markWeekComplete(weekId: String) {
        viewModelScope.launch { programRepo.markWeekComplete(weekId) }
    }
}
