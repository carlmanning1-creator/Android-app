package com.rawbarbell.club.ui.screens.programs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rawbarbell.club.data.db.entities.ProgramEntity
import com.rawbarbell.club.ui.navigation.Screen
import com.rawbarbell.club.ui.theme.*
import com.rawbarbell.club.ui.viewmodel.ProgramsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProgramsScreen(
    navController: NavController,
    viewModel: ProgramsViewModel = hiltViewModel()
) {
    val allPrograms by viewModel.allPrograms.collectAsState()
    val activeProgram by viewModel.activeProgram.collectAsState()

    var selectedProgram by remember { mutableStateOf<ProgramEntity?>(null) }
    var showActionsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val active = allPrograms.filter { it.isActive }
    val completed = allPrograms.filter { it.completedAt != null && !it.isActive }
    val inactive = allPrograms.filter { !it.isActive && it.completedAt == null }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "MY PROGRAMS",
                        color = WhiteText,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = TealAccent
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { navController.navigate(Screen.ImportSheet.route) },
                    containerColor = PurplePrimary,
                    contentColor = WhiteText
                ) {
                    Text("⬇", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.ProgramBuilder.route) },
                    containerColor = TealAccent,
                    contentColor = WhiteText
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add program")
                }
            }
        },
        containerColor = PurpleDark
    ) { padding ->
        if (allPrograms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No programs yet.\nCreate one or import from Google Sheets.",
                    color = WhiteText.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (active.isNotEmpty()) {
                    item {
                        SectionLabel("ACTIVE")
                    }
                    items(active) { program ->
                        ProgramCard(
                            program = program,
                            isActive = true,
                            onClick = { navController.navigate(Screen.ProgramDetail.createRoute(program.id)) },
                            onLongClick = {
                                selectedProgram = program
                                showActionsDialog = true
                            }
                        )
                    }
                }

                if (inactive.isNotEmpty()) {
                    item { SectionLabel("INACTIVE") }
                    items(inactive) { program ->
                        ProgramCard(
                            program = program,
                            isActive = false,
                            onClick = { navController.navigate(Screen.ProgramDetail.createRoute(program.id)) },
                            onLongClick = {
                                selectedProgram = program
                                showActionsDialog = true
                            }
                        )
                    }
                }

                if (completed.isNotEmpty()) {
                    item { SectionLabel("COMPLETED") }
                    items(completed) { program ->
                        ProgramCard(
                            program = program,
                            isActive = false,
                            isCompleted = true,
                            onClick = { navController.navigate(Screen.ProgramDetail.createRoute(program.id)) },
                            onLongClick = {
                                selectedProgram = program
                                showActionsDialog = true
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        if (showActionsDialog && selectedProgram != null) {
            AlertDialog(
                onDismissRequest = { showActionsDialog = false },
                containerColor = SurfaceDark,
                title = {
                    Text(
                        selectedProgram!!.name,
                        color = WhiteText,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        if (!selectedProgram!!.isActive) {
                            TextButton(onClick = {
                                viewModel.setActive(selectedProgram!!.id)
                                showActionsDialog = false
                            }) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = YellowHighlight)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Set Active", color = YellowHighlight)
                            }
                        }
                        if (selectedProgram!!.completedAt == null) {
                            TextButton(onClick = {
                                viewModel.completeProgram(selectedProgram!!.id)
                                showActionsDialog = false
                            }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TealAccent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mark Complete", color = TealAccent)
                            }
                        }
                        TextButton(onClick = {
                            showActionsDialog = false
                            showDeleteConfirm = true
                        }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showActionsDialog = false }) {
                        Text("Cancel", color = WhiteText)
                    }
                }
            )
        }

        if (showDeleteConfirm && selectedProgram != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = SurfaceDark,
                title = { Text("Delete program?", color = WhiteText) },
                text = {
                    Text(
                        "This will permanently delete '${selectedProgram!!.name}' and all its data.",
                        color = WhiteText.copy(alpha = 0.7f)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteProgram(selectedProgram!!.id)
                            showDeleteConfirm = false
                            selectedProgram = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete", color = WhiteText)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel", color = WhiteText)
                    }
                }
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = WhiteText.copy(alpha = 0.5f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProgramCard(
    program: ProgramEntity,
    isActive: Boolean,
    isCompleted: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val borderColor = when {
        isActive -> TealAccent
        isCompleted -> PurpleLight.copy(alpha = 0.5f)
        else -> PurpleLight
    }
    val alpha = if (isCompleted) 0.55f else 1f
    val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground.copy(alpha = alpha))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = program.name,
                    color = if (isActive) YellowHighlight else WhiteText.copy(alpha = alpha),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (isActive) {
                    Surface(
                        color = TealAccent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "ACTIVE",
                            color = TealAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                if (isCompleted) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = PurpleLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                MetaChip(program.sportType ?: "")
                Spacer(modifier = Modifier.width(6.dp))
                MetaChip("${program.daysPerWeek}d/wk")
                if (program.totalWeeks != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    MetaChip("${program.totalWeeks}wks")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Created ${dateFormat.format(java.util.Date(program.createdAt))}",
                color = WhiteText.copy(alpha = 0.4f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Surface(
        color = PurplePrimary,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            color = WhiteText.copy(alpha = 0.8f),
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
