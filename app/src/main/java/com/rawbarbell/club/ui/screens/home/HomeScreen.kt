package com.rawbarbell.club.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rawbarbell.club.navigation.Screen
import com.rawbarbell.club.ui.components.ExerciseCard
import com.rawbarbell.club.ui.theme.*
import com.rawbarbell.club.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val activeProgram by viewModel.activeProgram.collectAsState()
    val currentWeek by viewModel.currentWeek.collectAsState()
    val todayDay by viewModel.todayDay.collectAsState()
    val todayExercises by viewModel.todayExercises.collectAsState()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TealAccent)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RAW BARBELL CLUB",
                    color = WhiteText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp
                )
                Text(
                    text = "PROGRAM",
                    color = WhiteText.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    letterSpacing = 4.sp
                )
            }
        },
        containerColor = PurpleDark
    ) { padding ->
        if (activeProgram == null) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No active program",
                        color = WhiteText.copy(alpha = 0.6f),
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select or create a program to get started",
                        color = WhiteText.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { navController.navigate(Screen.Programs.route) },
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Start a Program",
                            color = WhiteText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Program name
                item {
                    Text(
                        text = activeProgram!!.name,
                        color = YellowHighlight,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                // Week header
                if (currentWeek != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PurplePrimary)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "WEEK ${currentWeek!!.weekNumber}",
                                color = WhiteText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Day header
                if (todayDay != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(YellowHighlight)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "DAY ${todayDay!!.dayNumber}",
                                color = PurpleDark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Exercise list
                items(todayExercises) { slot ->
                    val log = viewModel.getLogForSlot(slot.id)
                    val prescribed = viewModel.prescribedWeights[slot.id]
                    ExerciseCard(
                        slot = slot,
                        log = log,
                        prescribedWeight = prescribed,
                        onLog = {}
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Bottom actions
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    val dayComplete = todayDay?.isCompleted == true
                    if (dayComplete) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = TealAccent.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "COMPLETE",
                                    color = TealAccent,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    letterSpacing = 2.sp,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    if (activeProgram != null && currentWeek != null) {
                                        navController.navigate(
                                            Screen.ProgramDetail.createRoute(activeProgram!!.id)
                                        )
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TealAccent),
                                border = androidx.compose.foundation.BorderStroke(1.dp, TealAccent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("VIEW WEEK", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Button(
                                onClick = {
                                    if (todayDay != null && currentWeek != null) {
                                        navController.navigate(
                                            Screen.Session.createRoute(
                                                todayDay!!.id,
                                                currentWeek!!.id
                                            )
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = "LOG SESSION",
                                    color = WhiteText,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
