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
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardBackground)
            .border(
                width = 3.dp,
                color = TealAccent,
                shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
            )
            .clickable { onLog() }
            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row: slotCode + type chip + filming icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = slot.slotCode,
                    color = WhiteText.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Exercise type chip
                val typeColor = exerciseTypeColor(slot.exerciseType)
                Surface(
                    color = typeColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = slot.exerciseType,
                        color = typeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (slot.filmingRequired) {
                    Surface(
                        color = YellowHighlight.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "FILM",
                            color = YellowHighlight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Exercise name
            Text(
                text = slot.exerciseName,
                color = TealAccent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Sets × Reps + Intensity
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${slot.sets} × ${slot.reps}",
                    color = WhiteText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(16.dp))
                if (slot.relIntensity > 0f) {
                    Text(
                        text = "${slot.relIntensity}%",
                        color = YellowHighlight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (prescribedWeight != null && prescribedWeight > 0f) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "~${prescribedWeight.toInt()} kg",
                        color = WhiteText.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }

            // Notes
            if (slot.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = slot.notes,
                    color = YellowHighlight,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic
                )
            }

            // Log result
            if (log != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = PurpleLight, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${log.weightDone} kg × ${log.repsDone}",
                        color = WhiteText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    if (!log.performance.isNullOrBlank()) {
                        Surface(
                            color = performanceColor(log.performance).copy(alpha = 0.25f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = log.performance,
                                color = performanceColor(log.performance),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (log.e1rm != null && log.e1rm > 0f) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "e1RM: ${log.e1rm.toInt()} kg",
                            color = WhiteText.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
