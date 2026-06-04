package com.rawbarbell.club.data.db.dao

import androidx.room.*
import com.rawbarbell.club.data.db.entities.ProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramDao {

    @Query("SELECT * FROM programs ORDER BY createdAt DESC")
    fun getAllPrograms(): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE isActive = 1 LIMIT 1")
    fun getActiveProgram(): Flow<ProgramEntity?>

    @Query("SELECT * FROM programs WHERE id = :id")
    fun getProgramById(id: String): Flow<ProgramEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(program: ProgramEntity)

    @Update
    suspend fun updateProgram(program: ProgramEntity)

    @Query("DELETE FROM programs WHERE id = :id")
    suspend fun deleteProgram(id: String)

    @Transaction
    suspend fun setActiveProgram(id: String) {
        clearActivePrograms()
        activateProgram(id)
    }

    @Query("UPDATE programs SET isActive = 0")
    suspend fun clearActivePrograms()

    @Query("UPDATE programs SET isActive = 1 WHERE id = :id")
    suspend fun activateProgram(id: String)

    @Query("SELECT * FROM programs WHERE completedAt IS NOT NULL ORDER BY completedAt DESC")
    fun getCompletedPrograms(): Flow<List<ProgramEntity>>

    @Query("UPDATE programs SET completedAt = :completedAt, isActive = 0 WHERE id = :id")
    suspend fun setCompletedAt(id: String, completedAt: Long)
}
