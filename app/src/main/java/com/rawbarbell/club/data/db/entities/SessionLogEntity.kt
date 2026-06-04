package com.rawbarbell.club.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "session_logs",
    indices = [Index("exerciseSlotId"), Index("weekId"), Index("dayId")]
)
data class SessionLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val exerciseSlotId: String,
    val weekId: String,
    val dayId: String,
    val weightPrescribed: Float? = null,
    val weightDone: Float? = null,
    val repsDone: Int? = null,
    val performance: String? = null, // VERY_GOOD, GOOD, STANDARD, NOT_GREAT, ROUGH
    val rpe: Float? = null,
    val e1rm: Float? = null,
    val filmingDone: Boolean = false,
    val notes: String = "",
    val loggedAt: Long = System.currentTimeMillis()
)
