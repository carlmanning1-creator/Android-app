package com.rawbarbell.club.ui.screens.session

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rawbarbell.club.data.db.entities.ExerciseSlotEntity
import com.rawbarbell.club.data.db.entities.SessionLogEntity
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
    var attachedVideoUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    val videoCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.data?.let { attachedVideoUri = it }
    }

    val videoPickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { attachedVideoUri = it }
    }

    val loggedCount = logs.size
    val totalCount = exercises.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "WEEK $weekNumber — DAY $dayNumber",
                            color = WhiteText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "$loggedCount / $totalCount exercises logged",
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
                    start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            attachedVideoUri = null
                            showSheet = true
                        }
                    )
                }
            }

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
            attachedVideoUri = attachedVideoUri,
            onFilmCamera = {
                val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                videoCaptureLauncher.launch(intent)
            },
            onFilmPick = { videoPickLauncher.launch("video/*") },
            onDismiss = { showSheet = false },
            onSave = { weight, performance, notes ->
                viewModel.logExercise(
                    slotId = selectedSlot!!.id,
                    weightDone = weight,
                    repsDone = selectedSlot!!.reps,
                    performance = performance,
                    filmingDone = attachedVideoUri != null,
                    notes = notes ?: ""
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
    existingLog: SessionLogEntity?,
    prescribedWeight: Float?,
    previousLog: SessionLogEntity?,
    attachedVideoUri: Uri?,
    onFilmCamera: () -> Unit,
    onFilmPick: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (Float?, String?, String?) -> Unit
) {
    var weightText by remember { mutableStateOf(existingLog?.weightDone?.toString() ?: prescribedWeight?.toInt()?.toString() ?: "") }
    var performance by remember { mutableStateOf(existingLog?.performance) }
    var notes by remember { mutableStateOf(existingLog?.notes ?: "") }
    var showFilmOptions by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Exercise header
            if (slot.slotCode.isNotBlank()) {
                Text(
                    text = slot.slotCode,
                    color = WhiteText.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = slot.exerciseName,
                color = TealAccent,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )

            // Programmed prescription
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrescriptionChip(label = "SETS", value = slot.sets.toString())
                PrescriptionChip(label = "REPS", value = slot.reps.toString())
                if (prescribedWeight != null && prescribedWeight > 0f) {
                    PrescriptionChip(label = "TARGET", value = "${prescribedWeight.toInt()} kg")
                } else if (slot.relIntensity > 0f) {
                    PrescriptionChip(label = "INTENSITY", value = "${slot.relIntensity}%")
                }
            }

            // Coach notes / instructions
            if (slot.notes.isNotBlank()) {
                Surface(
                    color = PurplePrimary.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = slot.notes,
                        color = WhiteText.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Previous week comparison
            if (previousLog != null) {
                Surface(
                    color = TealAccent.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Last week",
                            color = TealAccent.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                        Text(
                            buildString {
                                previousLog.weightDone?.let { append("${it}kg") }
                                if (!previousLog.performance.isNullOrBlank()) {
                                    append(" · ${previousLog.performance}")
                                }
                            },
                            color = WhiteText.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            HorizontalDivider(color = PurpleLight, thickness = 0.5.dp)

            // Weight done
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = { Text("Weight Done (kg)", color = WhiteText.copy(alpha = 0.7f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Performance
            PerformanceDropdown(
                selected = performance,
                onSelect = { performance = it }
            )

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("How did it go?", color = WhiteText.copy(alpha = 0.7f)) },
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )

            // Film row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (attachedVideoUri != null) {
                    Surface(
                        color = TealAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "✓ Video attached",
                            color = TealAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
                OutlinedButton(
                    onClick = { showFilmOptions = true },
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(YellowHighlight)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = if (attachedVideoUri == null) Modifier.fillMaxWidth() else Modifier
                ) {
                    Text(
                        if (attachedVideoUri != null) "Change Video" else "📹  Film / Attach Video",
                        color = YellowHighlight,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            // Save
            Button(
                onClick = {
                    val weight = weightText.toFloatOrNull()
                    onSave(weight, performance, notes.takeIf { it.isNotBlank() })
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Save", color = WhiteText, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }

    if (showFilmOptions) {
        FilmOptionsDialog(
            onCamera = {
                showFilmOptions = false
                onFilmCamera()
            },
            onPick = {
                showFilmOptions = false
                onFilmPick()
            },
            onDismiss = { showFilmOptions = false }
        )
    }
}

@Composable
private fun PrescriptionChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = WhiteText.copy(alpha = 0.45f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Surface(
            color = PurplePrimary,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                value,
                color = WhiteText,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun FilmOptionsDialog(
    onCamera: () -> Unit,
    onPick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text("Attach Video", color = WhiteText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onCamera,
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Record with Camera", color = WhiteText, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onPick,
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(PurpleLight)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Upload from Gallery", color = WhiteText.copy(alpha = 0.8f))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = WhiteText.copy(alpha = 0.5f))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = WhiteText,
    unfocusedTextColor = WhiteText,
    focusedBorderColor = TealAccent,
    unfocusedBorderColor = PurpleLight,
    focusedContainerColor = PurpleDark,
    unfocusedContainerColor = PurpleDark,
    focusedLabelColor = TealAccent,
    cursorColor = TealAccent
)
