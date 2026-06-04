package com.rawbarbell.club.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "week_journals",
    indices = [Index("weekId", unique = true)]
)
data class WeekJournalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val weekId: String,
    val programId: String,
    val timeToComplete: String = "",
    val recoveryRating: Int = 0, // 1-10
    val highlights: String = "", // comma-separated
    val lowpoints: String = "", // comma-separated
    val injuries: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
