package com.rawbarbell.club.ui.viewmodel

import android.accounts.Account
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.rawbarbell.club.data.repository.ProgramRepository
import com.rawbarbell.club.data.sheets.SheetsImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject

private const val SHEETS_SCOPE = "oauth2:https://www.googleapis.com/auth/spreadsheets.readonly"
private const val SHEETS_API = "https://sheets.googleapis.com/v4/spreadsheets"

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val programRepo: ProgramRepository,
    private val sheetsImporter: SheetsImporter
) : ViewModel() {

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

    private var signedInAccount: GoogleSignInAccount? = null

    fun buildSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/spreadsheets.readonly"))
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    fun handleSignInResult(context: Context, data: Intent?) {
        viewModelScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                signedInAccount = task.result
                _isSignedIn.value = true
                _error.value = null
                // If a URL was already entered, kick off import
                if (_spreadsheetUrl.value.isNotBlank()) {
                    importSheet(context)
                }
            } catch (e: Exception) {
                _error.value = "Sign-in failed: ${e.message}"
            }
        }
    }

    fun onUrlChange(url: String) {
        _spreadsheetUrl.value = url
        _error.value = null
    }

    fun extractSheetId(url: String): String? {
        val pattern = Regex("/spreadsheets/d/([a-zA-Z0-9_-]+)")
        return pattern.find(url)?.groupValues?.getOrNull(1)
    }

    fun importSheet(context: Context) {
        val sheetId = extractSheetId(_spreadsheetUrl.value)
        if (sheetId == null) {
            _error.value = "Invalid Google Sheets URL. Paste the full URL from your browser."
            return
        }
        val account = signedInAccount ?: GoogleSignIn.getLastSignedInAccount(context)
        if (account == null) {
            _error.value = "Please sign in with Google first."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val token = withContext(Dispatchers.IO) {
                    GoogleAuthUtil.getToken(
                        context,
                        Account(account.email, "com.google"),
                        SHEETS_SCOPE
                    )
                }

                // Fetch the first week tab to determine structure
                val weekData = withContext(Dispatchers.IO) {
                    fetchSheetTab(sheetId, "Week 1", token)
                }

                val programName = account.displayName?.let { "$it — Imported Program" }
                    ?: "Imported Program"

                val (program, exercises) = sheetsImporter.parseSheetData(weekData, programName)
                programRepo.createProgram(program)
                exercises.forEach { programRepo.addExerciseToDay(it) }

                _importedProgramId.value = program.id
            } catch (e: Exception) {
                _error.value = "Import failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchSheetTab(spreadsheetId: String, tab: String, token: String): String {
        val url = "$SHEETS_API/$spreadsheetId/values/${tab.replace(" ", "%20")}?majorDimension=ROWS"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Sheets API error ${response.code}: ${response.message}")
        val body = response.body?.string() ?: throw Exception("Empty response from Sheets API")
        return parseSheetJsonToTsv(body)
    }

    private fun parseSheetJsonToTsv(json: String): String {
        val obj = JSONObject(json)
        val rows = obj.optJSONArray("values") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until rows.length()) {
            val row = rows.getJSONArray(i)
            val cols = (0 until row.length()).map { row.optString(it, "") }
            sb.appendLine(cols.joinToString("\t"))
        }
        return sb.toString()
    }

    fun clearError() { _error.value = null }
}
