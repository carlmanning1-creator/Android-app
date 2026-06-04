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

data class ExerciseBuilderItem(
    val id: String = UUID.randomUUID().toString(),
    val exerciseName: String,
    val category: String,
    val exerciseType: String,
    val sets: Int,
    val reps: Int,
    val intensityPct: Float,
    val progressionModel: String = "PERCENTAGE",
    val notes: String? = null,
    val maxReference: String? = null
)

@HiltViewModel
class ProgramBuilderViewModel @Inject constructor(
    private val programRepo: ProgramRepository
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    private val _sportType = MutableStateFlow("WEIGHTLIFTING")
    val sportType: StateFlow<String> = _sportType

    private val _daysPerWeek = MutableStateFlow(3)
    val daysPerWeek: StateFlow<Int> = _daysPerWeek

    private val _totalWeeks = MutableStateFlow(8)
    val totalWeeks: StateFlow<Int> = _totalWeeks

    private val _coachName = MutableStateFlow("")
    val coachName: StateFlow<String> = _coachName

    private val _blockType = MutableStateFlow("")
    val blockType: StateFlow<String> = _blockType

    private val _exercisesByDay = MutableStateFlow<Map<Int, List<ExerciseBuilderItem>>>(emptyMap())
    val exercisesByDay: StateFlow<Map<Int, List<ExerciseBuilderItem>>> = _exercisesByDay

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _savedProgramId = MutableStateFlow<String?>(null)
    val savedProgramId: StateFlow<String?> = _savedProgramId

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError

    fun clearSaveError() { _saveError.value = null }

    fun updateName(value: String) { _name.value = value }
    fun updateSportType(value: String) { _sportType.value = value }
    fun updateDaysPerWeek(value: Int) { _daysPerWeek.value = value }
    fun updateTotalWeeks(value: Int) { _totalWeeks.value = value }
    fun updateCoachName(value: String) { _coachName.value = value }
    fun updateBlockType(value: String) { _blockType.value = value }

    fun addExercise(dayNumber: Int, item: ExerciseBuilderItem) {
        val current = _exercisesByDay.value.toMutableMap()
        val dayList = current[dayNumber]?.toMutableList() ?: mutableListOf()
        dayList.add(item)
        current[dayNumber] = dayList
        _exercisesByDay.value = current
    }

    fun removeExercise(dayNumber: Int, item: ExerciseBuilderItem) {
        val current = _exercisesByDay.value.toMutableMap()
        val dayList = current[dayNumber]?.toMutableList() ?: return
        dayList.removeAll { it.id == item.id }
        current[dayNumber] = dayList
        _exercisesByDay.value = current
    }

    fun saveProgram() {
        viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = null
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
                    coachName = _coachName.value.takeIf { it.isNotBlank() },
                    blockType = _blockType.value.takeIf { it.isNotBlank() },
                    totalWeeks = _totalWeeks.value
                )
                programRepo.createProgram(program)

                val weeks = programRepo.getWeeksForProgram(programId).first()
                val days = weeks.flatMap { week ->
                    programRepo.getDaysForWeek(week.id).first()
                }

                val exercisesMap = _exercisesByDay.value
                for ((dayNumber, items) in exercisesMap) {
                    val matchingDay = days.firstOrNull { it.dayNumber == dayNumber } ?: continue
                    items.forEachIndexed { idx, item ->
                        programRepo.addExerciseToDay(
                            ExerciseSlotEntity(
                                id = UUID.randomUUID().toString(),
                                dayId = matchingDay.id,
                                programId = programId,
                                slotCode = "${dayNumber}_${idx + 1}",
                                exerciseName = item.exerciseName,
                                exerciseType = item.exerciseType,
                                category = item.category,
                                sets = item.sets,
                                reps = item.reps,
                                relIntensity = item.intensityPct / 100f,
                                progressionModel = item.progressionModel,
                                format = "Per",
                                notes = item.notes ?: "",
                                maxReference = item.maxReference,
                                filmingRequired = false,
                                orderIndex = idx
                            )
                        )
                    }
                }

                _savedProgramId.value = programId
            } catch (e: Exception) {
                _saveError.value = "Failed to save program: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }
}
