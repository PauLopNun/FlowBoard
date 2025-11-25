package com.flowboard.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Cursor(
    user: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(color)
        )
        Text(
            text = user,
            color = Color.White,
            modifier = Modifier
                .background(color)
                .padding(horizontal = 4.dp)
        )
    }
}
