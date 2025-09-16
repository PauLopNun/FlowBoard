package com.flowboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import com.flowboard.presentation.ui.theme.FlowBoardTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlowBoardTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FlowBoardApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}