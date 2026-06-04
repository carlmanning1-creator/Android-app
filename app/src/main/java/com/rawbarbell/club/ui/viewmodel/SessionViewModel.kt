package com.rawbarbell.club.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rawbarbell.club.data.db.entities.*
import com.rawbarbell.club.data.repository.PersonalBestRepository
import com.rawbarbell.club.data.repository.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val programRepo: ProgramRepository,
    private val pbRepo: PersonalBestRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val dayId: String = checkNotNull(savedStateHandle["dayId"])
    private val weekId: String = checkNotNull(savedStateHandle["weekId"])

    val exercises: StateFlow<List<ExerciseSlotEntity>> = programRepo.getExercisesForDay(dayId)
        .map { it.sortedBy { e -> e.orderIndex } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentLogs: StateFlow<List<SessionLogEntity>> = programRepo.getLogsForDay(dayId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<SessionLogEntity>> = currentLogs

    val weekLogs: StateFlow<List<SessionLogEntity>> = programRepo.getLogsForWeek(weekId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dayNumber = MutableStateFlow(0)
    val dayNumber: StateFlow<Int> = _dayNumber

    private val _weekNumber = MutableStateFlow(0)
    val weekNumber: StateFlow<Int> = _weekNumber

    // Previous week logs for comparison (loaded after we know the program)
    private val _previousWeekLogs = MutableStateFlow<List<SessionLogEntity>>(emptyList())

    init {
        viewModelScope.launch {
            programRepo.getDayById(dayId)?.let { day ->
                _dayNumber.value = day.dayNumber
                programRepo.getWeekById(weekId)?.let { week ->
                    _weekNumber.value = week.weekNumber
                    // Load previous week's logs
                    programRepo.getPreviousWeekLogs(week.programId, week.weekNumber)
                        .collect { _previousWeekLogs.value = it }
                }
            }
        }
    }

    private val _prescribedWeights = MutableStateFlow<Map<String, Float?>>(emptyMap())
    val prescribedWeights: StateFlow<Map<String, Float?>> = _prescribedWeights

    init {
        viewModelScope.launch {
            exercises.collectLatest { slots ->
                val weights = mutableMapOf<String, Float?>()
                for (slot in slots) {
                    val reference = slot.maxReference?.takeIf { it.isNotBlank() } ?: slot.category
                    val weight = if (reference != null && slot.relIntensity > 0f) {
                        pbRepo.calculateWeight(reference, slot.relIntensity)
                    } else null
                    weights[slot.id] = weight
                }
                _prescribedWeights.value = weights
            }
        }
    }

    fun logExercise(
        slotId: String,
        weightDone: Float?,
        repsDone: Int?,
        performance: String?,
        rpe: Float?,
        filmingDone: Boolean,
        notes: String
    ) {
        viewModelScope.launch {
            val e1rm = if (weightDone != null && repsDone != null && repsDone > 0) {
                weightDone * (1 + repsDone / 30f)
            } else null

            val prescribed = _prescribedWeights.value[slotId]
            val existing = getLogForSlot(slotId)

            val log = SessionLogEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                exerciseSlotId = slotId,
                weekId = weekId,
                dayId = dayId,
                weightPrescribed = prescribed,
                weightDone = weightDone,
                repsDone = repsDone,
                performance = performance,
                rpe = rpe,
                e1rm = e1rm,
                filmingDone = filmingDone,
                notes = notes,
                loggedAt = System.currentTimeMillis()
            )

            if (existing != null) {
                programRepo.updateSessionLog(log)
            } else {
                programRepo.logSession(log)
            }
        }
    }

    fun getLogForSlot(slotId: String): SessionLogEntity? =
        currentLogs.value.firstOrNull { it.exerciseSlotId == slotId }

    fun getPreviousLog(slotId: String): SessionLogEntity? =
        _previousWeekLogs.value.firstOrNull { it.exerciseSlotId == slotId }

    fun markDayComplete() {
        viewModelScope.launch { programRepo.markDayComplete(dayId) }
    }
}
