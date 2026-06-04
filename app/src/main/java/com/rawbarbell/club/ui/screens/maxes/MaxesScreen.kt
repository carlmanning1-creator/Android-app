package com.rawbarbell.club.ui.screens.maxes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rawbarbell.club.data.db.entities.PersonalBestEntity
import com.rawbarbell.club.ui.theme.*
import com.rawbarbell.club.ui.viewmodel.MaxesViewModel
import java.text.SimpleDateFormat
import java.util.Locale

private val EXERCISE_SUGGESTIONS = listOf(
    "Back Squat", "Front Squat", "Pause Squat",
    "Bench Press", "Close Grip Bench", "Overhead Press",
    "Deadlift", "Romanian Deadlift", "Sumo Deadlift",
    "Snatch", "Clean & Jerk", "Clean", "Jerk",
    "Power Snatch", "Power Clean", "Hang Snatch", "Hang Clean"
)

private val CATEGORY_GROUPS = mapOf(
    "SQUAT" to listOf("Back Squat", "Front Squat", "Pause Squat"),
    "BENCH" to listOf("Bench Press", "Close Grip Bench", "Overhead Press"),
    "DEADLIFT" to listOf("Deadlift", "Romanian Deadlift", "Sumo Deadlift"),
    "SNATCH" to listOf("Snatch", "Power Snatch", "Hang Snatch"),
    "CLEAN & JERK" to listOf("Clean & Jerk", "Clean", "Jerk", "Power Clean", "Hang Clean")
)

fun categoryForExercise(name: String): String {
    CATEGORY_GROUPS.forEach { (cat, exercises) ->
        if (exercises.any { it.equals(name, ignoreCase = true) }) return cat
    }
    return "OTHER"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaxesScreen(
    viewModel: MaxesViewModel = hiltViewModel()
) {
    val pbs by viewModel.pbs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingPb by remember { mutableStateOf<PersonalBestEntity?>(null) }
    var pbToDelete by remember { mutableStateOf<PersonalBestEntity?>(null) }

    val filtered = if (searchQuery.isBlank()) pbs else
        pbs.filter { it.exerciseName.contains(searchQuery, ignoreCase = true) }

    val grouped = filtered.groupBy { categoryForExercise(it.exerciseName) }
        .toSortedMap()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "PERSONAL BESTS",
                        color = WhiteText,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = TealAccent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingPb = null
                    showAddDialog = true
                },
                containerColor = TealAccent,
                contentColor = WhiteText
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add PB")
            }
        },
        containerColor = PurpleDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Search exercises…", color = WhiteText.copy(alpha = 0.4f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TealAccent) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = WhiteText,
                    unfocusedTextColor = WhiteText,
                    focusedBorderColor = TealAccent,
                    unfocusedBorderColor = PurpleLight,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    cursorColor = TealAccent
                ),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )

            if (pbs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No personal bests yet.\nAdd your maxes to get prescribed weights.",
                        color = WhiteText.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (category, items) ->
                        item {
                            Text(
                                text = category,
                                color = YellowHighlight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }
                        items(items) { pb ->
                            PbCard(
                                pb = pb,
                                onEdit = { editingPb = pb; showAddDialog = true },
                                onDelete = { pbToDelete = pb }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPbDialog(
            existing = editingPb,
            onDismiss = { showAddDialog = false },
            onSave = { name, max, notes ->
                viewModel.savePB(
                    exerciseName = name,
                    max = max,
                    notes = notes ?: ""
                )
                showAddDialog = false
            }
        )
    }

    if (pbToDelete != null) {
        AlertDialog(
            onDismissRequest = { pbToDelete = null },
            containerColor = SurfaceDark,
            title = { Text("Delete PB?", color = WhiteText) },
            text = {
                Text(
                    "Remove ${pbToDelete!!.exerciseName} (${pbToDelete!!.max} kg)?",
                    color = WhiteText.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePB(pbToDelete!!.id)
                        pbToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = WhiteText)
                }
            },
            dismissButton = {
                TextButton(onClick = { pbToDelete = null }) {
                    Text("Cancel", color = WhiteText)
                }
            }
        )
    }
}

@Composable
private fun PbCard(
    pb: PersonalBestEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardBackground)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pb.exerciseName,
                color = TealAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = dateFormat.format(java.util.Date(pb.achievedAt)),
                color = WhiteText.copy(alpha = 0.4f),
                fontSize = 11.sp
            )
            if (!pb.notes.isNullOrBlank()) {
                Text(
                    text = pb.notes,
                    color = WhiteText.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }
        Text(
            text = "${pb.max} kg",
            color = YellowHighlight,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = WhiteText.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPbDialog(
    existing: PersonalBestEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Float, String?) -> Unit
) {
    var name by remember { mutableStateOf(existing?.exerciseName ?: "") }
    var maxText by remember { mutableStateOf(existing?.max?.toString() ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var showSuggestions by remember { mutableStateOf(false) }

    val suggestions = if (name.length >= 2)
        EXERCISE_SUGGESTIONS.filter { it.contains(name, ignoreCase = true) && !it.equals(name, ignoreCase = true) }
    else emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                if (existing != null) "Edit Personal Best" else "Add Personal Best",
                color = WhiteText,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = showSuggestions && suggestions.isNotEmpty(),
                    onExpandedChange = { showSuggestions = it }
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            showSuggestions = true
                        },
                        label = { Text("Exercise name", color = WhiteText.copy(alpha = 0.7f)) },
                        colors = dialogFieldColors(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    if (suggestions.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = showSuggestions,
                            onDismissRequest = { showSuggestions = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            suggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion, color = WhiteText) },
                                    onClick = {
                                        name = suggestion
                                        showSuggestions = false
                                    },
                                    modifier = Modifier.background(SurfaceDark)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = maxText,
                    onValueChange = { maxText = it },
                    label = { Text("Max (kg)", color = WhiteText.copy(alpha = 0.7f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = dialogFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)", color = WhiteText.copy(alpha = 0.7f)) },
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val max = maxText.toFloatOrNull() ?: return@Button
                    onSave(name.trim(), max, notes.takeIf { it.isNotBlank() })
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                enabled = name.isNotBlank() && maxText.toFloatOrNull() != null
            ) {
                Text("Save", color = WhiteText)
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
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = WhiteText,
    unfocusedTextColor = WhiteText,
    focusedBorderColor = TealAccent,
    unfocusedBorderColor = PurpleLight,
    focusedContainerColor = PurpleDark,
    unfocusedContainerColor = PurpleDark,
    cursorColor = TealAccent,
    focusedLabelColor = TealAccent
)
