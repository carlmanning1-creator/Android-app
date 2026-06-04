package com.rawbarbell.club.data.db.dao

import androidx.room.*
import com.rawbarbell.club.data.db.entities.WeekJournalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeekJournalDao {

    @Query("SELECT * FROM week_journals WHERE weekId = :weekId LIMIT 1")
    fun getJournalForWeek(weekId: String): Flow<WeekJournalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJournal(journal: WeekJournalEntity)
}
