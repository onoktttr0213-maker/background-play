package com.logicpunch.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logicpunch.core.Card
import com.logicpunch.core.GameConfig
import com.logicpunch.core.PlayerId

@Composable
fun BenchSelectScreen(
    player: PlayerId,
    hand: List<Card>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(playerLabel(player) + ": ベンチに置くカードを3枚選択", fontSize = 18.sp)
            Text(
                "手札からベンチへ裏側の控えを作ります。あと" +
                    (GameConfig.BENCH_SIZE - selected.size).coerceAtLeast(0) + "枚選んでください。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(hand, key = { it.id }) { card ->
                CardTile(
                    card = card,
                    modifier = Modifier.fillMaxWidth(),
                    selected = selected.contains(card.id),
                    onClick = { onToggle(card.id) },
                )
            }
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Button(
                onClick = onConfirm,
                enabled = selected.size == GameConfig.BENCH_SIZE,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("ベンチを確定 (${selected.size}/${GameConfig.BENCH_SIZE})")
            }
        }
    }
}
