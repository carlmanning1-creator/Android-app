package com.rawbarbell.club.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.rawbarbell.club.ui.theme.*

val PERFORMANCE_RATINGS = listOf("VERY GOOD", "GOOD", "STANDARD", "NOT GREAT", "ROUGH")

fun performanceColor(rating: String?): Color = when (rating) {
    "VERY GOOD" -> Color(0xFF4CAF50)
    "GOOD"      -> TealAccent
    "STANDARD"  -> YellowHighlight
    "NOT GREAT" -> Color(0xFFFF9800)
    "ROUGH"     -> Color(0xFFF44336)
    else        -> WhiteText
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceDropdown(selected: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected ?: "Select performance",
            onValueChange = {},
            readOnly = true,
            label = { Text("Performance", color = WhiteText) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = if (selected != null) performanceColor(selected) else WhiteText,
                unfocusedTextColor = if (selected != null) performanceColor(selected) else WhiteText,
                focusedBorderColor = TealAccent,
                unfocusedBorderColor = PurpleLight,
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedLabelColor = TealAccent,
                unfocusedLabelColor = WhiteText,
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SurfaceDark)
        ) {
            PERFORMANCE_RATINGS.forEach { rating ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = rating,
                            color = performanceColor(rating),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onSelect(rating)
                        expanded = false
                    },
                    modifier = Modifier.background(SurfaceDark)
                )
            }
        }
    }
}
