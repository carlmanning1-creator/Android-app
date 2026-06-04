package com.rawbarbell.club.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rawbarbell.club.data.db.entities.*
import com.rawbarbell.club.data.repository.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val programRepo: ProgramRepository
) : ViewModel() {

    val activeProgram: StateFlow<ProgramEntity?> = programRepo.getActiveProgram()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentWeek = MutableStateFlow<WeekEntity?>(null)
    val currentWeek: StateFlow<WeekEntity?> = _currentWeek

    private val _todayDay = MutableStateFlow<DayEntity?>(null)
    val todayDay: StateFlow<DayEntity?> = _todayDay

    private val _todayExercises = MutableStateFlow<List<ExerciseSlotEntity>>(emptyList())
    val todayExercises: StateFlow<List<ExerciseSlotEntity>> = _todayExercises

    init {
        viewModelScope.launch {
            activeProgram.filterNotNull().collectLatest { program ->
                programRepo.getWeeksForProgram(program.id).collectLatest { weeks ->
                    val incomplete = weeks.filter { !it.isCompleted }
                    val current = incomplete.minByOrNull { it.weekNumber } ?: weeks.lastOrNull()
                    _currentWeek.value = current
                    current?.let { week ->
                        programRepo.getDaysForWeek(week.id).collectLatest { days ->
                            val incompleteDay = days.filter { !it.isCompleted }.minByOrNull { it.dayNumber }
                            _todayDay.value = incompleteDay
                            if (incompleteDay != null) {
                                programRepo.getExercisesForDay(incompleteDay.id).collectLatest { exercises ->
                                    _todayExercises.value = exercises.sortedBy { it.orderIndex }
                                }
                            } else {
                                _todayExercises.value = emptyList()
                            }
                        }
                    }
                }
            }
        }
    }
}
