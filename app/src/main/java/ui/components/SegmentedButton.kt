package com.cowanbas.gym.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cowanbas.gym.ui.theme.AppTheme

@Composable
fun SegmentedButton(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, AppTheme.border, RoundedCornerShape(8.dp))
    ) {
        options.forEach { opt ->
            val isSelected = opt == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (isSelected) AppTheme.primary else AppTheme.card)
                    .clickable { onSelect(opt) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    opt,
                    color = AppTheme.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}