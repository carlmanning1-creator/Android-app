package com.rawbarbell.club.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor() : ViewModel() {

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _importedProgramId = MutableStateFlow<String?>(null)
    val importedProgramId: StateFlow<String?> = _importedProgramId

    private val _spreadsheetUrl = MutableStateFlow("")
    val spreadsheetUrl: StateFlow<String> = _spreadsheetUrl

    fun onUrlChange(url: String) {
        _spreadsheetUrl.value = url
        _error.value = null
    }

    fun extractSheetId(url: String): String? {
        val pattern = Regex("/spreadsheets/d/([a-zA-Z0-9_-]+)")
        return pattern.find(url)?.groupValues?.getOrNull(1)
    }

    fun importSheet(context: Context) {
        viewModelScope.launch {
            val sheetId = extractSheetId(_spreadsheetUrl.value)
            if (sheetId == null) {
                _error.value = "Invalid Google Sheets URL. Please paste the full URL from your browser."
                return@launch
            }
            _isLoading.value = true
            _error.value = "Google Sheets import requires OAuth setup. See setup guide."
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
