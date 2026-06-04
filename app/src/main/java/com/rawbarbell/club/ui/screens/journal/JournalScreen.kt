package com.rawbarbell.club.ui.screens.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rawbarbell.club.ui.theme.*
import com.rawbarbell.club.ui.viewmodel.JournalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    navController: NavController,
    viewModel: JournalViewModel = hiltViewModel()
) {
    val journal by viewModel.journal.collectAsState()

    // Pre-populate from existing journal or blank
    var timeToComplete by remember { mutableStateOf(journal?.timeToComplete ?: "") }
    var recoveryRating by remember { mutableFloatStateOf((journal?.recoveryRating ?: 5).toFloat()) }

    val existingHighlights = remember(journal) {
        journal?.highlights?.split("|")?.padEnd(3, "") ?: listOf("", "", "")
    }
    val existingLowpoints = remember(journal) {
        journal?.lowpoints?.split("|")?.padEnd(3, "") ?: listOf("", "", "")
    }

    var highlight1 by remember { mutableStateOf(existingHighlights[0]) }
    var highlight2 by remember { mutableStateOf(existingHighlights[1]) }
    var highlight3 by remember { mutableStateOf(existingHighlights[2]) }

    var lowpoint1 by remember { mutableStateOf(existingLowpoints[0]) }
    var lowpoint2 by remember { mutableStateOf(existingLowpoints[1]) }
    var lowpoint3 by remember { mutableStateOf(existingLowpoints[2]) }

    var injuries by remember { mutableStateOf(journal?.injuries ?: "") }

    // Update fields if journal loads after composition
    LaunchedEffect(journal) {
        journal?.let { j ->
            if (timeToComplete.isBlank()) timeToComplete = j.timeToComplete ?: ""
            recoveryRating = (j.recoveryRating ?: 5).toFloat()
            val hl = j.highlights?.split("|")?.padEnd(3, "") ?: listOf("", "", "")
            val lp = j.lowpoints?.split("|")?.padEnd(3, "") ?: listOf("", "", "")
            highlight1 = hl[0]; highlight2 = hl[1]; highlight3 = hl[2]
            lowpoint1 = lp[0]; lowpoint2 = lp[1]; lowpoint3 = lp[2]
            injuries = j.injuries ?: ""
        }
    }

    val recoveryLabel = when (recoveryRating.toInt()) {
        1, 2 -> "POOR"
        3, 4 -> "BELOW AVERAGE"
        5, 6 -> "AVERAGE"
        7, 8 -> "GOOD"
        9, 10 -> "GREAT"
        else -> ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "WEEKLY CHECK-IN",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Time to complete
            JournalSection("Time to Complete This Week") {
                OutlinedTextField(
                    value = timeToComplete,
                    onValueChange = { timeToComplete = it },
                    label = { Text("e.g. 5h 30min", color = WhiteText.copy(alpha = 0.6f)) },
                    colors = journalFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Recovery rating
            JournalSection("Recovery Rating") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("POOR", color = WhiteText.copy(alpha = 0.4f), fontSize = 11.sp)
                    Slider(
                        value = recoveryRating,
                        onValueChange = { recoveryRating = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = TealAccent,
                            activeTrackColor = TealAccent,
                            inactiveTrackColor = PurpleLight
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    )
                    Text("GREAT", color = WhiteText.copy(alpha = 0.4f), fontSize = 11.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        color = TealAccent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "${recoveryRating.toInt()} / 10  —  $recoveryLabel",
                            color = TealAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Highlights
            JournalSection("Highlights") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        Triple("Highlight 1", highlight1) { v: String -> highlight1 = v },
                        Triple("Highlight 2", highlight2) { v: String -> highlight2 = v },
                        Triple("Highlight 3", highlight3) { v: String -> highlight3 = v }
                    ).forEach { (label, value, setter) ->
                        OutlinedTextField(
                            value = value,
                            onValueChange = setter,
                            label = { Text(label, color = WhiteText.copy(alpha = 0.6f)) },
                            colors = journalFieldColors(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Lowpoints
            JournalSection("Lowpoints") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        Triple("Lowpoint 1", lowpoint1) { v: String -> lowpoint1 = v },
                        Triple("Lowpoint 2", lowpoint2) { v: String -> lowpoint2 = v },
                        Triple("Lowpoint 3", lowpoint3) { v: String -> lowpoint3 = v }
                    ).forEach { (label, value, setter) ->
                        OutlinedTextField(
                            value = value,
                            onValueChange = setter,
                            label = { Text(label, color = WhiteText.copy(alpha = 0.6f)) },
                            colors = journalFieldColors(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Injuries
            JournalSection("Injuries / Niggles") {
                OutlinedTextField(
                    value = injuries,
                    onValueChange = { injuries = it },
                    label = { Text("Any injuries or niggles to note?", color = WhiteText.copy(alpha = 0.6f)) },
                    colors = journalFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }

            // Save
            Button(
                onClick = {
                    val highlights = listOf(highlight1, highlight2, highlight3)
                        .filter { it.isNotBlank() }
                        .joinToString("|")
                    val lowpoints = listOf(lowpoint1, lowpoint2, lowpoint3)
                        .filter { it.isNotBlank() }
                        .joinToString("|")

                    viewModel.saveJournal(
                        timeToComplete = timeToComplete.takeIf { it.isNotBlank() },
                        recoveryRating = recoveryRating.toInt(),
                        highlights = highlights.takeIf { it.isNotBlank() },
                        lowpoints = lowpoints.takeIf { it.isNotBlank() },
                        injuries = injuries.takeIf { it.isNotBlank() }
                    )
                    navController.popBackStack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    "Save Journal",
                    color = WhiteText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun JournalSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .padding(14.dp)
    ) {
        Text(
            text = title.uppercase(),
            color = YellowHighlight,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun journalFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = WhiteText,
    unfocusedTextColor = WhiteText,
    focusedBorderColor = TealAccent,
    unfocusedBorderColor = PurpleLight,
    focusedContainerColor = PurpleDark,
    unfocusedContainerColor = PurpleDark,
    cursorColor = TealAccent,
    focusedLabelColor = TealAccent
)

private fun List<String>.padEnd(size: Int, element: String): List<String> {
    return if (this.size >= size) this.take(size)
    else this + List(size - this.size) { element }
}
