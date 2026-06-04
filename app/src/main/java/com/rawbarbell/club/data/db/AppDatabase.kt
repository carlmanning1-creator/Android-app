package com.rawbarbell.club.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rawbarbell.club.data.db.dao.*
import com.rawbarbell.club.data.db.entities.*

@Database(
    entities = [
        ProgramEntity::class,
        WeekEntity::class,
        DayEntity::class,
        ExerciseSlotEntity::class,
        SessionLogEntity::class,
        WeekJournalEntity::class,
        PersonalBestEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun programDao(): ProgramDao
    abstract fun weekDao(): WeekDao
    abstract fun dayDao(): DayDao
    abstract fun exerciseSlotDao(): ExerciseSlotDao
    abstract fun sessionLogDao(): SessionLogDao
    abstract fun weekJournalDao(): WeekJournalDao
    abstract fun personalBestDao(): PersonalBestDao
}
