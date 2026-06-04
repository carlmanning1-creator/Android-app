package com.rawbarbell.club.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rawbarbell.club.data.db.entities.WeekJournalEntity
import com.rawbarbell.club.data.repository.ProgramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val programRepo: ProgramRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val weekId: String = checkNotNull(savedStateHandle["weekId"])

    val journal: StateFlow<WeekJournalEntity?> = programRepo.getJournalForWeek(weekId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveJournal(
        programId: String,
        timeToComplete: String,
        recoveryRating: Int,
        highlights: String,
        lowpoints: String,
        injuries: String
    ) {
        viewModelScope.launch {
            val existing = journal.value
            val entity = WeekJournalEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                weekId = weekId,
                programId = programId,
                timeToComplete = timeToComplete,
                recoveryRating = recoveryRating,
                highlights = highlights,
                lowpoints = lowpoints,
                injuries = injuries,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            programRepo.saveJournal(entity)
        }
    }
}
