package com.rawbarbell.club.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rawbarbell.club.data.db.entities.PersonalBestEntity
import com.rawbarbell.club.data.repository.PersonalBestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MaxesViewModel @Inject constructor(
    private val pbRepo: PersonalBestRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val pbs: StateFlow<List<PersonalBestEntity>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) pbRepo.getAllPBs()
            else pbRepo.searchPBs(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(q: String) { _searchQuery.value = q }

    fun savePB(exerciseName: String, max: Float, notes: String = "") {
        viewModelScope.launch { pbRepo.savePB(exerciseName, max, notes) }
    }

    fun deletePB(id: String) {
        viewModelScope.launch { pbRepo.deletePB(id) }
    }
}
