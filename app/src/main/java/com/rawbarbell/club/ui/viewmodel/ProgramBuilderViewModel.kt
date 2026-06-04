package com.rawbarbell.club.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rawbarbell.club.data.db.entities.*
import com.rawbarbell.club.data.repository.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProgramBuilderViewModel @Inject constructor(
    private val programRepo: ProgramRepository
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    private val _sportType = MutableStateFlow("")
    val sportType: StateFlow<String> = _sportType

    private val _daysPerWeek = MutableStateFlow(3)
    val daysPerWeek: StateFlow<Int> = _daysPerWeek

    private val _totalWeeks = MutableStateFlow(4)
    val totalWeeks: StateFlow<Int> = _totalWeeks

    private val _coachName = MutableStateFlow("")
    val coachName: StateFlow<String> = _coachName

    private val _blockType = MutableStateFlow("")
    val blockType: StateFlow<String> = _blockType

    private val _exercisesByDay = MutableStateFlow<Map<Int, List<ExerciseSlotEntity>>>(emptyMap())
    val exercisesByDay: StateFlow<Map<Int, List<ExerciseSlotEntity>>> = _exercisesByDay

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _savedProgramId = MutableStateFlow<String?>(null)
    val savedProgramId: StateFlow<String?> = _savedProgramId

    fun setName(value: String) { _name.value = value }
    fun setSportType(value: String) { _sportType.value = value }
    fun setDaysPerWeek(value: Int) { _daysPerWeek.value = value }
    fun setTotalWeeks(value: Int) { _totalWeeks.value = value }
    fun setCoachName(value: String) { _coachName.value = value }
    fun setBlockType(value: String) { _blockType.value = value }

    fun addExercise(dayNumber: Int, slot: ExerciseSlotEntity) {
        val current = _exercisesByDay.value.toMutableMap()
        val dayList = current[dayNumber]?.toMutableList() ?: mutableListOf()
        dayList.add(slot.copy(orderIndex = dayList.size))
        current[dayNumber] = dayList
        _exercisesByDay.value = current
    }

    fun removeExercise(dayNumber: Int, slotId: String) {
        val current = _exercisesByDay.value.toMutableMap()
        val dayList = current[dayNumber]?.toMutableList() ?: return
        dayList.removeAll { it.id == slotId }
        val reindexed = dayList.mapIndexed { idx, slot -> slot.copy(orderIndex = idx) }
        current[dayNumber] = reindexed
        _exercisesByDay.value = current
    }

    fun reorderExercises(dayNumber: Int, reordered: List<ExerciseSlotEntity>) {
        val current = _exercisesByDay.value.toMutableMap()
        current[dayNumber] = reordered.mapIndexed { idx, slot -> slot.copy(orderIndex = idx) }
        _exercisesByDay.value = current
    }

    fun saveProgram() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val programId = UUID.randomUUID().toString()
                val program = ProgramEntity(
                    id = programId,
                    name = _name.value,
                    sportType = _sportType.value,
                    daysPerWeek = _daysPerWeek.value,
                    isActive = false,
                    createdAt = System.currentTimeMillis(),
                    completedAt = null,
                    sourceSheetId = null,
                    coachName = _coachName.value,
                    blockType = _blockType.value,
                    totalWeeks = _totalWeeks.value
                )
                programRepo.createProgram(program)

                val exercisesMap = _exercisesByDay.value
                for ((_, slots) in exercisesMap) {
                    for (slot in slots) {
                        programRepo.addExerciseToDay(slot.copy(programId = programId))
                    }
                }

                _savedProgramId.value = programId
            } finally {
                _isSaving.value = false
            }
        }
    }
}
