package com.rawbarbell.club.data.repository

import com.rawbarbell.club.data.db.dao.PersonalBestDao
import com.rawbarbell.club.data.db.entities.PersonalBestEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonalBestRepository @Inject constructor(
    private val personalBestDao: PersonalBestDao
) {

    fun getAllPBs(): Flow<List<PersonalBestEntity>> = personalBestDao.getAllPBs()

    suspend fun getPBByExercise(name: String): PersonalBestEntity? =
        personalBestDao.getPBByExercise(name)

    suspend fun savePB(exerciseName: String, max: Float, notes: String) {
        val existing = personalBestDao.getPBByExercise(exerciseName)
        val pb = existing?.copy(max = max, notes = notes, achievedAt = System.currentTimeMillis())
            ?: PersonalBestEntity(exerciseName = exerciseName, max = max, notes = notes)
        personalBestDao.upsertPB(pb)
    }

    suspend fun deletePB(id: String) = personalBestDao.deletePB(id)

    fun searchPBs(query: String): Flow<List<PersonalBestEntity>> =
        personalBestDao.searchPBs(query)

    suspend fun calculateWeight(exerciseName: String, intensity: Float): Float? {
        val pb = personalBestDao.getPBByExercise(exerciseName) ?: return null
        return pb.max * intensity
    }
}
