package com.logicpunch.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logicpunch.core.PlayerId

@Composable
fun PassScreen(
    forPlayer: PlayerId,
    message: String,
    onReveal: () -> Unit,
) {
    val badgeColor = attributeColorForPlayer(forPlayer)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(24.dp),
        ) {
            Text(
                playerLabel(forPlayer),
                color = androidx.compose.ui.graphics.Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier
                    .background(badgeColor, RoundedCornerShape(50))
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            )
            Text(
                message,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 16.dp),
            )
            Button(
                onClick = onReveal,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("タップして画面を見る")
            }
        }
        Text(
            playerLabel(if (forPlayer == PlayerId.PLAYER_ONE) PlayerId.PLAYER_TWO else PlayerId.PLAYER_ONE) + " は画面を見ないでください",
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
fun attributeColorForPlayer(id: PlayerId): androidx.compose.ui.graphics.Color {
    val colors = com.logicpunch.app.ui.theme.LocalAttributeColors.current
    return if (id == PlayerId.PLAYER_ONE) colors.blue else colors.red
}
