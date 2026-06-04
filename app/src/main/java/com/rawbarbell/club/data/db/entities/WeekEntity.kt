package com.rawbarbell.club.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "weeks",
    foreignKeys = [ForeignKey(
        entity = ProgramEntity::class,
        parentColumns = ["id"],
        childColumns = ["programId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("programId")]
)
data class WeekEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val programId: String,
    val weekNumber: Int,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)
