package com.rawbarbell.club.data.repository

import com.rawbarbell.club.data.db.dao.*
import com.rawbarbell.club.data.db.entities.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgramRepository @Inject constructor(
    private val programDao: ProgramDao,
    private val weekDao: WeekDao,
    private val dayDao: DayDao,
    private val exerciseSlotDao: ExerciseSlotDao,
    private val sessionLogDao: SessionLogDao,
    private val weekJournalDao: WeekJournalDao
) {

    fun getAllPrograms(): Flow<List<ProgramEntity>> = programDao.getAllPrograms()

    fun getActiveProgram(): Flow<ProgramEntity?> = programDao.getActiveProgram()

    fun getProgramById(id: String): Flow<ProgramEntity?> = programDao.getProgramById(id)

    fun getCompletedPrograms(): Flow<List<ProgramEntity>> = programDao.getCompletedPrograms()

    suspend fun createProgram(program: ProgramEntity) {
        programDao.insertProgram(program)

        val weeks = (1..program.totalWeeks).map { weekNumber ->
            WeekEntity(programId = program.id, weekNumber = weekNumber)
        }
        weekDao.insertWeeks(weeks)

        val days = weeks.flatMap { week ->
            (1..program.daysPerWeek).map { dayNumber ->
                DayEntity(weekId = week.id, programId = program.id, dayNumber = dayNumber)
            }
        }
        dayDao.insertDays(days)
    }

    suspend fun setActiveProgram(id: String) = programDao.setActiveProgram(id)

    suspend fun completeProgram(id: String) {
        programDao.setCompletedAt(id, System.currentTimeMillis())
    }

    suspend fun deleteProgram(id: String) = programDao.deleteProgram(id)

    fun getWeeksForProgram(programId: String): Flow<List<WeekEntity>> =
        weekDao.getWeeksForProgram(programId)

    fun getDaysForWeek(weekId: String): Flow<List<DayEntity>> =
        dayDao.getDaysForWeek(weekId)

    fun getExercisesForDay(dayId: String): Flow<List<ExerciseSlotEntity>> =
        exerciseSlotDao.getExercisesForDay(dayId)

    suspend fun logSession(log: SessionLogEntity) = sessionLogDao.upsertLog(log)

    suspend fun updateSessionLog(log: SessionLogEntity) = sessionLogDao.updateLog(log)

    fun getLogsForDay(dayId: String): Flow<List<SessionLogEntity>> =
        sessionLogDao.getLogsForDay(dayId)

    fun getLogsForWeek(weekId: String): Flow<List<SessionLogEntity>> =
        sessionLogDao.getLogsForWeek(weekId)

    fun getJournalForWeek(weekId: String): Flow<WeekJournalEntity?> =
        weekJournalDao.getJournalForWeek(weekId)

    suspend fun saveJournal(journal: WeekJournalEntity) =
        weekJournalDao.upsertJournal(journal)

    suspend fun addExerciseToDay(slot: ExerciseSlotEntity) =
        exerciseSlotDao.insertExercise(slot)

    suspend fun updateExercise(slot: ExerciseSlotEntity) =
        exerciseSlotDao.updateExercise(slot)

    suspend fun deleteExercise(id: String) =
        exerciseSlotDao.deleteExercise(id)

    suspend fun getDayById(dayId: String): DayEntity? =
        dayDao.getDayById(dayId)

    suspend fun getWeekById(weekId: String): WeekEntity? =
        weekDao.getWeekByIdOnce(weekId)

    suspend fun markDayComplete(dayId: String) =
        dayDao.markDayComplete(dayId)

    suspend fun markWeekComplete(weekId: String) =
        weekDao.markWeekComplete(weekId, System.currentTimeMillis())

    fun getPreviousWeekLogs(programId: String, currentWeekNumber: Int): Flow<List<SessionLogEntity>> =
        sessionLogDao.getLogsForProgramBeforeWeek(programId, currentWeekNumber)
}
