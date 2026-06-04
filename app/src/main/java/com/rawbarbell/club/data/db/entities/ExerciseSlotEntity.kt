package com.rawbarbell.club.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "exercise_slots",
    foreignKeys = [ForeignKey(
        entity = DayEntity::class,
        parentColumns = ["id"],
        childColumns = ["dayId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("dayId"), Index("programId")]
)
data class ExerciseSlotEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dayId: String,
    val programId: String,
    val slotCode: String, // "1a", "1b", "2a" etc
    val exerciseName: String,
    val exerciseType: String, // PRIMARY, SECONDARY, TOP_SET, PRIMER
    val category: String, // SQUAT, BENCH, DEADLIFT, SNATCH, CLEAN_AND_JERK, BACK, SHOULDERS, LEGS, ACCESSORIES
    val sets: Int,
    val reps: Int,
    val relIntensity: Float = 0f, // 0.825 = 82.5%
    val progressionModel: String = "STANDARD",
    val format: String = "Per", // Per, GYM, MoB
    val notes: String = "",
    val maxReference: String? = null,
    val filmingRequired: Boolean = false,
    val orderIndex: Int = 0
)
