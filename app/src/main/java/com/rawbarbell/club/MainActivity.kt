package com.rawbarbell.club

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rawbarbell.club.ui.navigation.AppNavigation
import com.rawbarbell.club.ui.theme.RawBarbellClubTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RawBarbellClubTheme {
                AppNavigation()
            }
        }
    }
}
