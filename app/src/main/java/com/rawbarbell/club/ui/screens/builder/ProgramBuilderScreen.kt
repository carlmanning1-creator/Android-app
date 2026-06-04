package com.rawbarbell.club.ui.screens.builder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rawbarbell.club.ui.theme.*
import com.rawbarbell.club.ui.viewmodel.ProgramBuilderViewModel

private val SPORT_TYPES = listOf("WEIGHTLIFTING", "POWERLIFTING", "SUPERTOTAL")
private val BLOCK_TYPES = listOf("VOLUME", "STRENGTH", "PEAK")
private val EXERCISE_TYPES = listOf("PRIMARY", "SECONDARY", "TOP SET", "PRIMER")
private val PROGRESSION_MODELS = listOf("LINEAR", "WAVE", "STEP", "PERCENTAGE", "RPE BASED")

private val EXERCISES_BY_SPORT = mapOf(
    "WEIGHTLIFTING" to mapOf(
        "SNATCH" to listOf("Snatch", "Power Snatch", "Hang Snatch", "Snatch Pull", "Snatch Deadlift", "Overhead Squat"),
        "CLEAN & JERK" to listOf("Clean & Jerk", "Clean", "Jerk", "Power Clean", "Hang Clean", "Clean Pull", "Front Squat"),
        "SQUAT" to listOf("Back Squat", "Front Squat", "Pause Squat"),
        "ACCESSORY" to listOf("Romanian Deadlift", "Good Morning", "Press")
    ),
    "POWERLIFTING" to mapOf(
        "SQUAT" to listOf("Back Squat", "Pause Squat", "Box Squat", "SSB Squat"),
        "BENCH" to listOf("Bench Press", "Close Grip Bench", "Overhead Press", "Incline Bench"),
        "DEADLIFT" to listOf("Deadlift", "Sumo Deadlift", "Romanian Deadlift", "Deficit Deadlift"),
        "ACCESSORY" to listOf("Row", "Pull Up", "Tricep Pushdown", "Leg Press")
    ),
    "SUPERTOTAL" to mapOf(
        "SNATCH" to listOf("Snatch", "Power Snatch", "Hang Snatch"),
        "CLEAN & JERK" to listOf("Clean & Jerk", "Clean", "Jerk"),
        "SQUAT" to listOf("Back Squat", "Front Squat"),
        "BENCH" to listOf("Bench Press", "Close Grip Bench"),
        "DEADLIFT" to listOf("Deadlift", "Romanian Deadlift"),
        "ACCESSORY" to listOf("Press", "Row", "Pull Up")
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramBuilderScreen(
    navController: NavController,
    viewModel: ProgramBuilderViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsState()
    val sportType by viewModel.sportType.collectAsState()
    val daysPerWeek by viewModel.daysPerWeek.collectAsState()
    val totalWeeks by viewModel.totalWeeks.collectAsState()
    val exercisesByDay by viewModel.exercisesByDay.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val savedProgramId by viewModel.savedProgramId.collectAsState()
    val saveError by viewModel.saveError.collectAsState()

    var currentStep by remember { mutableIntStateOf(0) } // 0 = details, 1 = exercises
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableIntStateOf(1) }

    LaunchedEffect(savedProgramId) {
        if (savedProgramId != null) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "BUILD PROGRAM",
                        color = WhiteText,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Step indicator
            StepIndicator(currentStep = currentStep)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (currentStep == 0) {
                    // Step 1: Program details
                    item {
                        SectionHeader("Program Details")
                    }

                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { viewModel.updateName(it) },
                            label = { Text("Program Name", color = WhiteText.copy(alpha = 0.7f)) },
                            colors = builderFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        DropdownSelector(
                            label = "Sport Type",
                            options = SPORT_TYPES,
                            selected = sportType,
                            onSelect = { viewModel.updateSportType(it) }
                        )
                    }

                    item {
                        SectionHeader("Days Per Week")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            (1..5).forEach { d ->
                                FilterChip(
                                    selected = daysPerWeek == d,
                                    onClick = { viewModel.updateDaysPerWeek(d) },
                                    label = { Text("$d", color = if (daysPerWeek == d) PurpleDark else WhiteText, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = YellowHighlight,
                                        containerColor = CardBackground
                                    )
                                )
                            }
                        }
                    }

                    item {
                        SectionHeader("Total Weeks")
                        Spacer(modifier = Modifier.height(8.dp))
                        var weekSlider by remember { mutableFloatStateOf((totalWeeks ?: 8).toFloat()) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = weekSlider,
                                onValueChange = { weekSlider = it; viewModel.updateTotalWeeks(it.toInt()) },
                                valueRange = 1f..16f,
                                steps = 14,
                                colors = SliderDefaults.colors(
                                    thumbColor = TealAccent,
                                    activeTrackColor = TealAccent,
                                    inactiveTrackColor = PurpleLight
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "${weekSlider.toInt()} wks",
                                color = YellowHighlight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    item {
                        DropdownSelector(
                            label = "Block Type",
                            options = BLOCK_TYPES,
                            selected = viewModel.blockType.collectAsState().value,
                            onSelect = { viewModel.updateBlockType(it) }
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = viewModel.coachName.collectAsState().value ?: "",
                            onValueChange = { viewModel.updateCoachName(it) },
                            label = { Text("Coach Name (optional)", color = WhiteText.copy(alpha = 0.7f)) },
                            colors = builderFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                } else {
                    // Step 2: Exercises per day
                    item { SectionHeader("Add Exercises") }

                    // Day tabs
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            (1..(daysPerWeek ?: 4)).forEach { d ->
                                FilterChip(
                                    selected = selectedDay == d,
                                    onClick = { selectedDay = d },
                                    label = {
                                        Text(
                                            "Day $d",
                                            color = if (selectedDay == d) PurpleDark else WhiteText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TealAccent,
                                        containerColor = CardBackground
                                    )
                                )
                            }
                        }
                    }

                    // Exercise list for selected day
                    val dayExercises = exercisesByDay[selectedDay] ?: emptyList()
                    items(dayExercises) { exercise ->
                        ExerciseBuilderRow(
                            exercise = exercise,
                            onRemove = { viewModel.removeExercise(selectedDay, exercise) }
                        )
                    }

                    item {
                        OutlinedButton(
                            onClick = { showAddExerciseDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TealAccent),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TealAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Exercise to Day $selectedDay", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Bottom navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep == 1) {
                    OutlinedButton(
                        onClick = { currentStep = 0 },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WhiteText),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PurpleLight),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                }

                Button(
                    onClick = {
                        if (currentStep == 0) {
                            currentStep = 1
                        } else {
                            viewModel.saveProgram()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentStep == 1) YellowHighlight else TealAccent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    enabled = !isSaving && name.isNotBlank()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = WhiteText,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (currentStep == 0) "Next: Exercises" else "Save Program",
                            color = if (currentStep == 1) PurpleDark else WhiteText,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }

    if (!saveError.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { viewModel.clearSaveError() },
            containerColor = SurfaceDark,
            title = { Text("Save Failed", color = WhiteText, fontWeight = FontWeight.Bold) },
            text = { Text(saveError!!, color = WhiteText.copy(alpha = 0.8f), fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSaveError() }) {
                    Text("OK", color = TealAccent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAddExerciseDialog) {
        AddExerciseDialog(
            dayNumber = selectedDay,
            sportType = sportType ?: "WEIGHTLIFTING",
            onDismiss = { showAddExerciseDialog = false },
            onAdd = { exerciseData ->
                viewModel.addExercise(selectedDay, exerciseData)
                showAddExerciseDialog = false
            }
        )
    }
}

@Composable
private fun StepIndicator(currentStep: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("Details", "Exercises").forEachIndexed { index, label ->
            val active = index == currentStep
            val done = index < currentStep
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when {
                            done -> TealAccent.copy(alpha = 0.4f)
                            active -> TealAccent
                            else -> PurpleLight.copy(alpha = 0.3f)
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    "${index + 1}. $label",
                    color = if (active) WhiteText else WhiteText.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = YellowHighlight,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun ExerciseBuilderRow(
    exercise: com.rawbarbell.club.ui.viewmodel.ExerciseBuilderItem,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CardBackground)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(exercise.exerciseName, color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                "${exercise.exerciseType} · ${exercise.sets}×${exercise.reps} @ ${exercise.intensityPct.toInt()}%",
                color = WhiteText.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = WhiteText.copy(alpha = 0.4f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseDialog(
    dayNumber: Int,
    sportType: String,
    onDismiss: () -> Unit,
    onAdd: (com.rawbarbell.club.ui.viewmodel.ExerciseBuilderItem) -> Unit
) {
    val categories = (EXERCISES_BY_SPORT[sportType] ?: EXERCISES_BY_SPORT["WEIGHTLIFTING"]!!).keys.toList()
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "") }
    val exercises = (EXERCISES_BY_SPORT[sportType] ?: EXERCISES_BY_SPORT["WEIGHTLIFTING"]!!)[selectedCategory] ?: emptyList()

    var selectedExercise by remember { mutableStateOf(exercises.firstOrNull() ?: "") }
    var exerciseType by remember { mutableStateOf("PRIMARY") }
    var setsText by remember { mutableStateOf("3") }
    var repsText by remember { mutableStateOf("5") }
    var intensitySlider by remember { mutableFloatStateOf(80f) }
    var progressionModel by remember { mutableStateOf("PERCENTAGE") }
    var notes by remember { mutableStateOf("") }
    var maxReference by remember { mutableStateOf("") }

    LaunchedEffect(selectedCategory) {
        selectedExercise = exercises.firstOrNull() ?: ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text("Add Exercise — Day $dayNumber", color = WhiteText, fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                item {
                    DropdownSelector(
                        label = "Category",
                        options = categories,
                        selected = selectedCategory,
                        onSelect = { selectedCategory = it }
                    )
                }

                item {
                    DropdownSelector(
                        label = "Exercise",
                        options = exercises,
                        selected = selectedExercise,
                        onSelect = { selectedExercise = it }
                    )
                }

                item {
                    Text("Exercise Type", color = YellowHighlight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        EXERCISE_TYPES.forEach { type ->
                            FilterChip(
                                selected = exerciseType == type,
                                onClick = { exerciseType = type },
                                label = {
                                    Text(
                                        type,
                                        color = if (exerciseType == type) PurpleDark else WhiteText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealAccent,
                                    containerColor = CardBackground
                                )
                            )
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = setsText,
                            onValueChange = { setsText = it },
                            label = { Text("Sets", color = WhiteText.copy(alpha = 0.7f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = dialogFieldColors(),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = repsText,
                            onValueChange = { repsText = it },
                            label = { Text("Reps", color = WhiteText.copy(alpha = 0.7f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = dialogFieldColors(),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text("Intensity: ${intensitySlider.toInt()}%", color = YellowHighlight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = intensitySlider,
                        onValueChange = { intensitySlider = it },
                        valueRange = 50f..105f,
                        colors = SliderDefaults.colors(
                            thumbColor = TealAccent,
                            activeTrackColor = TealAccent,
                            inactiveTrackColor = PurpleLight
                        )
                    )
                }

                item {
                    DropdownSelector(
                        label = "Progression Model",
                        options = PROGRESSION_MODELS,
                        selected = progressionModel,
                        onSelect = { progressionModel = it }
                    )
                }

                item {
                    OutlinedTextField(
                        value = maxReference,
                        onValueChange = { maxReference = it },
                        label = { Text("Max Reference (e.g. Back Squat)", color = WhiteText.copy(alpha = 0.7f)) },
                        colors = dialogFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)", color = WhiteText.copy(alpha = 0.7f)) },
                        colors = dialogFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        com.rawbarbell.club.ui.viewmodel.ExerciseBuilderItem(
                            exerciseName = selectedExercise,
                            category = selectedCategory,
                            exerciseType = exerciseType,
                            sets = setsText.toIntOrNull() ?: 3,
                            reps = repsText.toIntOrNull() ?: 5,
                            intensityPct = intensitySlider,
                            progressionModel = progressionModel,
                            notes = notes.takeIf { it.isNotBlank() },
                            maxReference = maxReference.takeIf { it.isNotBlank() }
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                enabled = selectedExercise.isNotBlank()
            ) {
                Text("Add", color = WhiteText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = WhiteText)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelector(
    label: String,
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = WhiteText.copy(alpha = 0.7f)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = builderFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SurfaceDark)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = WhiteText) },
                    onClick = { onSelect(option); expanded = false },
                    modifier = Modifier.background(SurfaceDark)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun builderFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = WhiteText,
    unfocusedTextColor = WhiteText,
    focusedBorderColor = TealAccent,
    unfocusedBorderColor = PurpleLight,
    focusedContainerColor = PurpleDark,
    unfocusedContainerColor = PurpleDark,
    cursorColor = TealAccent,
    focusedLabelColor = TealAccent
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = WhiteText,
    unfocusedTextColor = WhiteText,
    focusedBorderColor = TealAccent,
    unfocusedBorderColor = PurpleLight,
    focusedContainerColor = SurfaceDark,
    unfocusedContainerColor = SurfaceDark,
    cursorColor = TealAccent,
    focusedLabelColor = TealAccent
)
