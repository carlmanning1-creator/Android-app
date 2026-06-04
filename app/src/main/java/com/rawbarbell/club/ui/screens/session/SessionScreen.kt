package com.rawbarbell.club.ui.screens.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rawbarbell.club.data.local.entity.ExerciseSlotEntity
import com.rawbarbell.club.ui.components.ExerciseCard
import com.rawbarbell.club.ui.components.PerformanceDropdown
import com.rawbarbell.club.ui.theme.*
import com.rawbarbell.club.ui.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    navController: NavController,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val exercises by viewModel.exercises.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val prescribedWeights by viewModel.prescribedWeights.collectAsState()

    val dayNumber by viewModel.dayNumber.collectAsState()
    val weekNumber by viewModel.weekNumber.collectAsState()

    var selectedSlot by remember { mutableStateOf<ExerciseSlotEntity?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    val loggedCount = logs.size
    val totalCount = exercises.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "DAY $dayNumber — WEEK $weekNumber",
                            color = WhiteText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "$loggedCount / $totalCount logged",
                            color = WhiteText.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = WhiteText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TealAccent)
            )
        },
        containerColor = PurpleDark
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(exercises) { slot ->
                    val log = viewModel.getLogForSlot(slot.id)
                    val prescribed = prescribedWeights[slot.id]
                    ExerciseCard(
                        slot = slot,
                        log = log,
                        prescribedWeight = prescribed,
                        onLog = {
                            selectedSlot = slot
                            showSheet = true
                        }
                    )
                }
            }

            // Complete Day button
            Button(
                onClick = {
                    viewModel.markDayComplete()
                    navController.popBackStack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = YellowHighlight),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .height(52.dp)
            ) {
                Text(
                    "COMPLETE DAY",
                    color = PurpleDark,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    letterSpacing = 2.sp
                )
            }
        }
    }

    if (showSheet && selectedSlot != null) {
        LogBottomSheet(
            slot = selectedSlot!!,
            existingLog = viewModel.getLogForSlot(selectedSlot!!.id),
            prescribedWeight = prescribedWeights[selectedSlot!!.id],
            previousLog = viewModel.getPreviousLog(selectedSlot!!.id),
            onDismiss = { showSheet = false },
            onSave = { weight, reps, performance, rpe, filmingDone, notes ->
                viewModel.logExercise(
                    slotId = selectedSlot!!.id,
                    weightDone = weight,
                    repsDone = reps,
                    performance = performance,
                    rpe = rpe,
                    filmingDone = filmingDone,
                    notes = notes
                )
                showSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogBottomSheet(
    slot: ExerciseSlotEntity,
    existingLog: com.rawbarbell.club.data.local.entity.SessionLogEntity?,
    prescribedWeight: Float?,
    previousLog: com.rawbarbell.club.data.local.entity.SessionLogEntity?,
    onDismiss: () -> Unit,
    onSave: (Float, Int, String?, Float?, Boolean, String?) -> Unit
) {
    var weightText by remember { mutableStateOf(existingLog?.weightDone?.toString() ?: prescribedWeight?.toInt()?.toString() ?: "") }
    var repsText by remember { mutableStateOf(existingLog?.repsDone?.toString() ?: slot.reps.toString()) }
    var performance by remember { mutableStateOf(existingLog?.performance) }
    var rpeText by remember { mutableStateOf(existingLog?.rpe?.toString() ?: "") }
    var filmingDone by remember { mutableStateOf(existingLog?.filmingDone ?: false) }
    var notes by remember { mutableStateOf(existingLog?.notes ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = slot.exerciseName,
                color = TealAccent,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${slot.sets} sets × ${slot.reps} reps${if ((slot.relIntensity ?: 0f) > 0f) " @ ${slot.relIntensity}%" else ""}",
                color = WhiteText.copy(alpha = 0.7f),
                fontSize = 13.sp
            )

            if (prescribedWeight != null && prescribedWeight > 0f) {
                Text(
                    text = "Prescribed: ~${prescribedWeight.toInt()} kg",
                    color = YellowHighlight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (previousLog != null) {
                Surface(
                    color = PurplePrimary.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Last: ${previousLog.weightDone}kg × ${previousLog.repsDone}" +
                            (if (!previousLog.performance.isNullOrBlank()) " (${previousLog.performance})" else ""),
                        color = WhiteText.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Weight
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = { Text("Weight Done (kg)", color = WhiteText.copy(alpha = 0.7f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = outlinedFieldColors(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Reps
            OutlinedTextField(
                value = repsText,
                onValueChange = { repsText = it },
                label = { Text("Reps Done", color = WhiteText.copy(alpha = 0.7f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = outlinedFieldColors(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Performance dropdown
            PerformanceDropdown(
                selected = performance,
                onSelect = { performance = it }
            )

            // RPE
            OutlinedTextField(
                value = rpeText,
                onValueChange = { if (it.length <= 4) rpeText = it },
                label = { Text("RPE (optional, 1-10)", color = WhiteText.copy(alpha = 0.7f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = outlinedFieldColors(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Filming
            if (slot.filmingRequired == true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = filmingDone,
                        onCheckedChange = { filmingDone = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = TealAccent,
                            uncheckedColor = WhiteText.copy(alpha = 0.5f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Filming done", color = WhiteText)
                }
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)", color = WhiteText.copy(alpha = 0.7f)) },
                colors = outlinedFieldColors(),
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Button(
                onClick = {
                    val weight = weightText.toFloatOrNull() ?: 0f
                    val reps = repsText.toIntOrNull() ?: 0
                    val rpe = rpeText.toFloatOrNull()
                    onSave(weight, reps, performance, rpe, filmingDone, notes.takeIf { it.isNotBlank() })
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Save", color = WhiteText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = WhiteText,
    unfocusedTextColor = WhiteText,
    focusedBorderColor = TealAccent,
    unfocusedBorderColor = PurpleLight,
    focusedContainerColor = PurpleDark,
    unfocusedContainerColor = PurpleDark,
    focusedLabelColor = TealAccent,
    cursorColor = TealAccent
)
