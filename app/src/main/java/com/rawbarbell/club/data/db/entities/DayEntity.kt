package com.rawbarbell.club.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "days",
    foreignKeys = [ForeignKey(
        entity = WeekEntity::class,
        parentColumns = ["id"],
        childColumns = ["weekId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("weekId"), Index("programId")]
)
data class DayEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val weekId: String,
    val programId: String,
    val dayNumber: Int,
    val isCompleted: Boolean = false
)
