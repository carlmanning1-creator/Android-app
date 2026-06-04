package com.rawbarbell.club.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "personal_bests",
    indices = [Index("exerciseName", unique = true)]
)
data class PersonalBestEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val exerciseName: String,
    val max: Float,
    val achievedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)
