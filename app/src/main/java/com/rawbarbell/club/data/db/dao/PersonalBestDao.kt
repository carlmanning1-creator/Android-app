package com.rawbarbell.club.data.db.dao

import androidx.room.*
import com.rawbarbell.club.data.db.entities.PersonalBestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalBestDao {

    @Query("SELECT * FROM personal_bests ORDER BY exerciseName ASC")
    fun getAllPBs(): Flow<List<PersonalBestEntity>>

    @Query("SELECT * FROM personal_bests WHERE exerciseName = :exerciseName LIMIT 1")
    suspend fun getPBByExercise(exerciseName: String): PersonalBestEntity?

    @Query("SELECT * FROM personal_bests WHERE exerciseName IN (:names)")
    suspend fun getPBsByExercises(names: List<String>): List<PersonalBestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPB(pb: PersonalBestEntity)

    @Query("DELETE FROM personal_bests WHERE id = :id")
    suspend fun deletePB(id: String)

    @Query("SELECT * FROM personal_bests WHERE exerciseName LIKE '%' || :query || '%' ORDER BY exerciseName ASC")
    fun searchPBs(query: String): Flow<List<PersonalBestEntity>>
}
