package com.rawbarbell.club.data.db.dao

import androidx.room.*
import com.rawbarbell.club.data.db.entities.WeekEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeekDao {

    @Query("SELECT * FROM weeks WHERE programId = :programId ORDER BY weekNumber ASC")
    fun getWeeksForProgram(programId: String): Flow<List<WeekEntity>>

    @Query("SELECT * FROM weeks WHERE id = :id")
    fun getWeekById(id: String): Flow<WeekEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeek(week: WeekEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeks(weeks: List<WeekEntity>)

    @Update
    suspend fun updateWeek(week: WeekEntity)

    @Query("UPDATE weeks SET isCompleted = 1, completedAt = :completedAt WHERE id = :id")
    suspend fun markWeekComplete(id: String, completedAt: Long)
}
