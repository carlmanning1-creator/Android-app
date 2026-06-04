package com.rawbarbell.club.di

import android.content.Context
import androidx.room.Room
import com.rawbarbell.club.data.db.AppDatabase
import com.rawbarbell.club.data.db.dao.*
import com.rawbarbell.club.data.repository.PersonalBestRepository
import com.rawbarbell.club.data.repository.ProgramRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "raw_barbell_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideProgramDao(db: AppDatabase): ProgramDao = db.programDao()

    @Provides
    @Singleton
    fun provideWeekDao(db: AppDatabase): WeekDao = db.weekDao()

    @Provides
    @Singleton
    fun provideDayDao(db: AppDatabase): DayDao = db.dayDao()

    @Provides
    @Singleton
    fun provideExerciseSlotDao(db: AppDatabase): ExerciseSlotDao = db.exerciseSlotDao()

    @Provides
    @Singleton
    fun provideSessionLogDao(db: AppDatabase): SessionLogDao = db.sessionLogDao()

    @Provides
    @Singleton
    fun provideWeekJournalDao(db: AppDatabase): WeekJournalDao = db.weekJournalDao()

    @Provides
    @Singleton
    fun providePersonalBestDao(db: AppDatabase): PersonalBestDao = db.personalBestDao()

    @Provides
    @Singleton
    fun provideProgramRepository(
        programDao: ProgramDao,
        weekDao: WeekDao,
        dayDao: DayDao,
        exerciseSlotDao: ExerciseSlotDao,
        sessionLogDao: SessionLogDao,
        weekJournalDao: WeekJournalDao
    ): ProgramRepository = ProgramRepository(
        programDao, weekDao, dayDao, exerciseSlotDao, sessionLogDao, weekJournalDao
    )

    @Provides
    @Singleton
    fun providePersonalBestRepository(
        personalBestDao: PersonalBestDao
    ): PersonalBestRepository = PersonalBestRepository(personalBestDao)
}
