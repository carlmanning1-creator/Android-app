package com.rawbarbell.club.data.db.dao

import androidx.room.*
import com.rawbarbell.club.data.db.entities.DayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayDao {

    @Query("SELECT * FROM days WHERE weekId = :weekId ORDER BY dayNumber ASC")
    fun getDaysForWeek(weekId: String): Flow<List<DayEntity>>

    @Query("SELECT * FROM days WHERE id = :id")
    suspend fun getDayById(id: String): DayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: DayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDays(days: List<DayEntity>)

    @Update
    suspend fun updateDay(day: DayEntity)

    @Query("UPDATE days SET isCompleted = 1 WHERE id = :id")
    suspend fun markDayComplete(id: String)

    @Query("SELECT * FROM days WHERE programId = :programId ORDER BY dayNumber ASC")
    fun getDaysForProgram(programId: String): Flow<List<DayEntity>>
}
