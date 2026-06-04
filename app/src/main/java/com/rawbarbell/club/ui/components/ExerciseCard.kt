package com.rawbarbell.club.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rawbarbell.club.data.db.entities.ExerciseSlotEntity
import com.rawbarbell.club.data.db.entities.SessionLogEntity
import com.rawbarbell.club.ui.theme.*

fun exerciseTypeColor(type: String): Color = when (type.uppercase()) {
    "PRIMARY"   -> Color(0xFFE91E63)
    "SECONDARY" -> TealAccent
    "TOP SET"   -> YellowHighlight
    "PRIMER"    -> Color(0xFF9C27B0)
    else        -> PurpleLight
}


@Composable
fun ExerciseCard(
    slot: ExerciseSlotEntity,
    log: SessionLogEntity?,
    prescribedWeight: Float?,
    onLog: () -> Unit
) {
    val isLogged = log != null
    val accentColor = if (isLogged) TealAccent else PurpleLight
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardBackground)
            .border(width = if (isLogged) 2.dp else 1.dp, color = accentColor, shape = shape)
            .clickable { onLog() }
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .matchParentSize()
                .background(if (isLogged) TealAccent else PurplePrimary)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 14.dp, end = 14.dp, bottom = 14.dp)
        ) {
            // Top row: slot code + type chip + FILM badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (slot.slotCode.isNotBlank()) {
                    Text(
                        text = slot.slotCode,
                        color = WhiteText.copy(alpha = 0.45f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (slot.exerciseType.isNotBlank()) {
                    val typeColor = exerciseTypeColor(slot.exerciseType)
                    Surface(
                        color = typeColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = slot.exerciseType,
                            color = typeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (isLogged) {
                    Surface(
                        color = TealAccent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "✓ DONE",
                            color = TealAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (slot.filmingRequired) {
                    Surface(
                        color = YellowHighlight.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "📹 FILM",
                            color = YellowHighlight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Exercise name — prominent
            Text(
                text = slot.exerciseName,
                color = WhiteText,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 22.sp
            )

            // Instruction / coach notes
            if (slot.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = slot.notes,
                    color = WhiteText.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Prescription row: Sets | Reps | Target weight
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrescriptionStat(label = "SETS", value = slot.sets.toString())
                PrescriptionStat(label = "REPS", value = slot.reps.toString())
                if (prescribedWeight != null && prescribedWeight > 0f) {
                    PrescriptionStat(label = "TARGET", value = "${prescribedWeight.toInt()} kg")
                } else if (slot.relIntensity > 0f) {
                    PrescriptionStat(label = "%", value = "${slot.relIntensity}%")
                }
            }

            // Logged result
            if (log != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = TealAccent.copy(alpha = 0.25f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = buildString {
                            log.weightDone?.let { append("${it} kg") }
                            if (log.repsDone != null && log.repsDone > 0) {
                                if (log.weightDone != null) append(" × ")
                                append("${log.repsDone} reps")
                            }
                        },
                        color = WhiteText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (!log.performance.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(10.dp))
                        val pColor = performanceColor(log.performance)
                        Surface(
                            color = pColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = log.performance,
                                color = pColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (log.filmingDone) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📹", fontSize = 12.sp)
                    }
                }

                if (!log.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = log.notes,
                        color = WhiteText.copy(alpha = 0.55f),
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Tap to log →",
                    color = TealAccent.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PrescriptionStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = WhiteText.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(1.dp))
        Text(value, color = YellowHighlight, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}
