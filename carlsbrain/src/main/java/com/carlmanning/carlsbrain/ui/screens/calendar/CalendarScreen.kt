package com.carlmanning.carlsbrain.ui.screens.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.carlmanning.carlsbrain.ui.components.BrainTopBar

@Composable
fun CalendarScreen(
    onVaultToggle: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            BrainTopBar(
                title = "Calendar",
                onVaultToggle = onVaultToggle,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Upcoming events will appear here",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
