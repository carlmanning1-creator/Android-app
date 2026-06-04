package com.rawbarbell.club.ui.screens.importsheet

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rawbarbell.club.ui.navigation.Screen
import com.rawbarbell.club.ui.theme.*
import com.rawbarbell.club.ui.viewmodel.ImportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSheetScreen(
    navController: NavController,
    viewModel: ImportViewModel = hiltViewModel()
) {
    val spreadsheetUrl by viewModel.spreadsheetUrl.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val importedProgramId by viewModel.importedProgramId.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(importedProgramId) {
        importedProgramId?.let { id ->
            navController.navigate(Screen.ProgramDetail.createRoute(id)) {
                popUpTo(Screen.ImportSheet.route) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "IMPORT FROM SHEETS",
                        color = WhiteText,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        fontSize = 16.sp
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Instructions card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground)
                    .padding(16.dp)
            ) {
                Text(
                    "Import Your Program",
                    color = YellowHighlight,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Paste your Google Sheets URL below, or sign in with Google to browse your sheets.",
                    color = WhiteText.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "The sheet must follow the RAW Barbell Club template format.",
                    color = WhiteText.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }

            // URL input
            OutlinedTextField(
                value = spreadsheetUrl,
                onValueChange = { viewModel.onUrlChange(it) },
                label = { Text("Paste Google Sheets URL", color = WhiteText.copy(alpha = 0.7f)) },
                placeholder = { Text("https://docs.google.com/spreadsheets/d/...", color = WhiteText.copy(alpha = 0.3f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = WhiteText,
                    unfocusedTextColor = WhiteText,
                    focusedBorderColor = TealAccent,
                    unfocusedBorderColor = PurpleLight,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    cursorColor = TealAccent,
                    focusedLabelColor = TealAccent
                ),
                singleLine = false,
                minLines = 2,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Import from URL button
            Button(
                onClick = { viewModel.importSheet(context) },
                enabled = spreadsheetUrl.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = WhiteText,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Importing…", color = WhiteText, fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        "Import from URL",
                        color = WhiteText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }

            // Error
            if (!error.isNullOrBlank()) {
                Surface(
                    color = Color(0xFFF44336).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error!!,
                        color = Color(0xFFF44336),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = PurpleLight
                )
                Text(
                    "  OR  ",
                    color = WhiteText.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = PurpleLight
                )
            }

            // Google Sign In button
            Button(
                onClick = { /* TODO: Trigger Google OAuth sign-in flow */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "🔵 Sign in with Google",
                    color = Color(0xFF1A1A1A),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            // Setup guide card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PurplePrimary.copy(alpha = 0.6f))
                    .padding(14.dp)
            ) {
                Text(
                    "Setup Required",
                    color = YellowHighlight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Google sign-in requires OAuth configuration.\nSee code comments in SheetsImporter.kt for detailed setup instructions including:\n• Google Cloud Console project setup\n• OAuth 2.0 credentials\n• Drive API scope configuration",
                    color = WhiteText.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
