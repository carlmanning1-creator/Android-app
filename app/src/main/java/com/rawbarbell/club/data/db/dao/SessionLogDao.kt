package com.rawbarbell.club.data.db.dao

import androidx.room.*
import com.rawbarbell.club.data.db.entities.SessionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionLogDao {

    @Query("SELECT * FROM session_logs WHERE dayId = :dayId ORDER BY loggedAt ASC")
    fun getLogsForDay(dayId: String): Flow<List<SessionLogEntity>>

    @Query("SELECT * FROM session_logs WHERE exerciseSlotId = :exerciseSlotId AND weekId = :weekId LIMIT 1")
    fun getLogForExerciseInWeek(exerciseSlotId: String, weekId: String): Flow<SessionLogEntity?>

    @Query("SELECT * FROM session_logs WHERE weekId = :weekId ORDER BY loggedAt ASC")
    fun getLogsForWeek(weekId: String): Flow<List<SessionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SessionLogEntity)

    @Update
    suspend fun updateLog(log: SessionLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: SessionLogEntity)

    @Query("SELECT * FROM session_logs WHERE exerciseSlotId IN (SELECT id FROM exercise_slots WHERE programId = :programId) ORDER BY loggedAt ASC")
    suspend fun getLogsForProgram(programId: String): List<SessionLogEntity>
}
