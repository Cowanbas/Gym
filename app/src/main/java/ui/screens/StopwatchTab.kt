package com.cowanbas.gym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cowanbas.gym.ui.theme.AppTheme
import java.util.Locale

@Composable
fun StopwatchTab(
    timeInMillis: Long,
    isRunning: Boolean,
    onTimeChanged: (Long) -> Unit,
    onRunningChanged: (Boolean) -> Unit
) {
    val totalSeconds = timeInMillis / 1000
    val deciseconds = (timeInMillis % 1000) / 100
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    val timeText = if (minutes > 0) {
        String.format(Locale.US, "%d:%02d.%d", minutes, seconds, deciseconds)
    } else {
        String.format(Locale.US, "%d.%d", seconds, deciseconds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.card)
            .padding(24.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = timeText,
                style = TextStyle(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Light,
                    color = AppTheme.text,
                    textAlign = TextAlign.Center
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = AppTheme.muted.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .clickable {
                            onRunningChanged(!isRunning)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Start",
                            tint = AppTheme.text,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                if (timeInMillis > 0) {
                    Box(
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        IconButton(
                            onClick = {
                                onTimeChanged(0L)
                                onRunningChanged(false)
                            },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset",
                                tint = AppTheme.text,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}