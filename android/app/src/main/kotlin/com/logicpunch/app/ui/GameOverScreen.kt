package com.logicpunch.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logicpunch.core.GameConfig
import com.logicpunch.core.PlayerId

@Composable
fun GameOverScreen(winner: PlayerId, vsCpu: Boolean = false, onRestart: () -> Unit) {
    val loser = if (winner == PlayerId.PLAYER_ONE) PlayerId.PLAYER_TWO else PlayerId.PLAYER_ONE
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🏆 ${seatLabel(winner, vsCpu)} の勝利！", fontSize = 26.sp, textAlign = TextAlign.Center)
        Text(
            "${seatLabel(loser, vsCpu)} はライフを ${GameConfig.LIFE_COUNT} 枚すべて失いました。",
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Button(
            onClick = onRestart,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) { Text("もう一度あそぶ") }
    }
}
