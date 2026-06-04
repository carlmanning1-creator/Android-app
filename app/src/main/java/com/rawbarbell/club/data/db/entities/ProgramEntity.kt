package com.rawbarbell.club.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "programs")
data class ProgramEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sportType: String, // WEIGHTLIFTING, POWERLIFTING, SUPERTOTAL
    val daysPerWeek: Int,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val sourceSheetId: String? = null,
    val coachName: String? = null,
    val blockType: String? = null,
    val totalWeeks: Int = 4
)
