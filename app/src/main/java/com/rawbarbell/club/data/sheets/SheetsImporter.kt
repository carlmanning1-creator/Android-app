package com.rawbarbell.club.data.sheets

// To enable Google Sheets import:
// 1) Create OAuth 2.0 credentials in Google Cloud Console
// 2) Enable Google Sheets API v4
// 3) Add your web client ID to strings.xml as google_oauth_client_id
// 4) Implement the auth flow in ImportScreen

import com.rawbarbell.club.data.db.entities.*
import java.util.UUID
import javax.inject.Inject

class SheetsImporter @Inject constructor() {

    companion object {
        val WEEK_RANGES = (1..16).map { "Week $it" }
        const val SHEETS_API_BASE = "https://sheets.googleapis.com/v4/spreadsheets"
        const val PROGRAM_META_RANGE = "Program!A1:Z10"
        const val EXERCISES_HEADER_RANGE = "Exercises!A1:Z1"
    }

    fun extractSpreadsheetId(url: String): String? {
        val pattern = Regex("/spreadsheets/d/([a-zA-Z0-9_-]+)")
        return pattern.find(url)?.groupValues?.getOrNull(1)
    }

    fun buildSheetsApiUrl(spreadsheetId: String, range: String): String {
        return "$SHEETS_API_BASE/$spreadsheetId/values/$range"
    }

    /**
     * Parses raw sheet data (tab-separated or CSV) into a ProgramEntity and associated exercises.
     *
     * Expected row format (tab-separated):
     * slotCode | exerciseName | exerciseType | category | sets | reps | relIntensity | progressionModel | format | notes | maxReference | filmingRequired
     *
     * First row is treated as a header and skipped.
     *
     * @param sheetContent Raw text content from the spreadsheet
     * @param programName  Name to assign to the created program
     * @return A pair of the ProgramEntity and a list of ExerciseSlotEntities parsed from the sheet
     */
    fun parseSheetData(sheetContent: String, programName: String): Pair<ProgramEntity, List<ExerciseSlotEntity>> {
        val programId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val program = ProgramEntity(
            id = programId,
            name = programName,
            sportType = "",
            daysPerWeek = 4,
            isActive = false,
            createdAt = now,
            completedAt = null,
            sourceSheetId = null,
            coachName = "",
            blockType = "",
            totalWeeks = 4
        )

        val lines = sheetContent.lines()
        if (lines.isEmpty()) return Pair(program, emptyList())

        val delimiter = if (sheetContent.contains('\t')) '\t' else ','
        val dataLines = lines.drop(1).filter { it.isNotBlank() }

        val exercises = mutableListOf<ExerciseSlotEntity>()
        var orderIndex = 0

        for (line in dataLines) {
            val cols = line.split(delimiter).map { it.trim() }
            if (cols.size < 2) continue

            val slotCode = cols.getOrNull(0) ?: ""
            val exerciseName = cols.getOrNull(1) ?: continue.also { }
            if (exerciseName.isBlank()) continue

            val slot = ExerciseSlotEntity(
                id = UUID.randomUUID().toString(),
                dayId = "",
                programId = programId,
                slotCode = slotCode,
                exerciseName = exerciseName,
                exerciseType = cols.getOrNull(2) ?: "",
                category = cols.getOrNull(3) ?: "",
                sets = cols.getOrNull(4)?.toIntOrNull() ?: 0,
                reps = cols.getOrNull(5)?.toIntOrNull() ?: 0,
                relIntensity = cols.getOrNull(6)?.toFloatOrNull() ?: 0f,
                progressionModel = cols.getOrNull(7) ?: "",
                format = cols.getOrNull(8) ?: "",
                notes = cols.getOrNull(9) ?: "",
                maxReference = cols.getOrNull(10) ?: "",
                filmingRequired = cols.getOrNull(11)?.lowercase()?.let {
                    it == "true" || it == "yes" || it == "1"
                } ?: false,
                orderIndex = orderIndex++
            )
            exercises.add(slot)
        }

        return Pair(program, exercises)
    }
}
