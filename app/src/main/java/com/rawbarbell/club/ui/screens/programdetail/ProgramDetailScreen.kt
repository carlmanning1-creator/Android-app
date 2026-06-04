package com.rawbarbell.club.ui.screens.programdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import com.rawbarbell.club.data.local.entity.DayEntity
import com.rawbarbell.club.data.local.entity.WeekEntity
import com.rawbarbell.club.navigation.Screen
import com.rawbarbell.club.ui.components.WeekHeader
import com.rawbarbell.club.ui.theme.*
import com.rawbarbell.club.ui.viewmodel.ProgramDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramDetailScreen(
    navController: NavController,
    viewModel: ProgramDetailViewModel = hiltViewModel()
) {
    val program by viewModel.program.collectAsState()
    val weeks by viewModel.weeks.collectAsState()
    val selectedWeekDays by viewModel.selectedWeekDays.collectAsState()

    var expandedWeekId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        program?.name ?: "Program",
                        color = WhiteText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Program info
            program?.let { p ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardBackground)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InfoBlock("SPORT", p.sportType ?: "-")
                        InfoBlock("DAYS/WK", "${p.daysPerWeek}")
                        InfoBlock("BLOCK", p.blockType ?: "-")
                        if (p.totalWeeks != null) {
                            InfoBlock("WEEKS", "${p.totalWeeks}")
                        }
                    }
                }
            }

            items(weeks) { week ->
                WeekSection(
                    week = week,
                    days = if (expandedWeekId == week.id) selectedWeekDays else emptyList(),
                    isExpanded = expandedWeekId == week.id,
                    onToggle = {
                        if (expandedWeekId == week.id) {
                            expandedWeekId = null
                        } else {
                            expandedWeekId = week.id
                            viewModel.loadDaysForWeek(week.id)
                        }
                    },
                    onDayClick = { day ->
                        navController.navigate(Screen.Session.createRoute(day.id, week.id))
                    },
                    onCompleteWeek = { viewModel.markWeekComplete(week.id) },
                    navController = navController,
                    weekId = week.id
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun InfoBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = WhiteText.copy(alpha = 0.5f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = value,
            color = YellowHighlight,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun WeekSection(
    week: WeekEntity,
    days: List<DayEntity>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDayClick: (DayEntity) -> Unit,
    onCompleteWeek: () -> Unit,
    navController: NavController,
    weekId: Long
) {
    val completedDays = days.count { it.isCompleted }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            WeekHeader(
                weekNumber = week.weekNumber,
                isCompleted = week.isCompleted,
                dayCount = days.size,
                completedDays = completedDays
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = WhiteText.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceDark)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                days.forEach { day ->
                    DayRow(day = day, onClick = { onDayClick(day) })
                }

                if (!week.isCompleted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onCompleteWeek,
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Complete Week",
                            color = WhiteText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Journal button
                OutlinedButton(
                    onClick = { navController.navigate(Screen.Journal.createRoute(weekId)) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = YellowHighlight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, YellowHighlight),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Weekly Journal", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DayRow(day: DayEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CardBackground)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(YellowHighlight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${day.dayNumber}",
                color = PurpleDark,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "DAY ${day.dayNumber}",
            color = WhiteText,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        if (day.isCompleted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Completed",
                tint = TealAccent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
