package com.rawbarbell.club.data.db.dao

import androidx.room.*
import com.rawbarbell.club.data.db.entities.ExerciseSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseSlotDao {

    @Query("SELECT * FROM exercise_slots WHERE dayId = :dayId ORDER BY orderIndex ASC")
    fun getExercisesForDay(dayId: String): Flow<List<ExerciseSlotEntity>>

    @Query("SELECT * FROM exercise_slots WHERE id = :id")
    suspend fun getExerciseById(id: String): ExerciseSlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(slot: ExerciseSlotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(slots: List<ExerciseSlotEntity>)

    @Update
    suspend fun updateExercise(slot: ExerciseSlotEntity)

    @Query("DELETE FROM exercise_slots WHERE id = :id")
    suspend fun deleteExercise(id: String)

    @Query("SELECT * FROM exercise_slots WHERE programId = :programId ORDER BY orderIndex ASC")
    suspend fun getExercisesForProgram(programId: String): List<ExerciseSlotEntity>
}
