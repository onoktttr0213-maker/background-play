package com.logicpunch.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logicpunch.core.Card
import com.logicpunch.core.GameConfig
import com.logicpunch.core.GameState
import com.logicpunch.core.PlayerId
import com.logicpunch.core.TurnPhase

@Composable
fun TurnScreen(
    state: GameState,
    onFieldCardTap: (String) -> Unit,
    onBenchToField: (String) -> Unit,
    onHandToBench: (String) -> Unit,
    onToBattle: () -> Unit,
    onEndTurn: () -> Unit,
) {
    val pid = state.currentPlayer
    val oid = if (pid == PlayerId.PLAYER_ONE) PlayerId.PLAYER_TWO else PlayerId.PLAYER_ONE
    val me = state.player(pid)
    val other = state.player(oid)

    Column(modifier = Modifier.fillMaxSize()) {
        StatusBar(state)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            ZoneBlock(
                title = "相手の場（" + playerLabel(oid) + "）",
                trailing = "手札 ${other.hand.size}枚",
                background = MaterialTheme.colorScheme.surface,
            ) {
                CardRow(other.field) { card -> CardTile(card) }
                SectionLabel("相手のベンチ")
                CardRow(other.bench) { card -> CardTile(card) }
            }

            ZoneBlock(
                title = "自分の場",
                trailing = "山札 ${state.deck.size}枚 / 捨札 ${me.discard.size + other.discard.size}枚",
                background = MaterialTheme.colorScheme.background,
            ) {
                CardRow(me.field) { card ->
                    val used = state.attackedFieldCardIdsThisTurn.contains(card.id)
                    val canAttack = state.phase == TurnPhase.BATTLE && !used &&
                        state.attacksUsedThisTurn < GameConfig.MAX_ATTACKS_PER_TURN
                    CardTile(
                        card = card,
                        mode = CardStatMode.ATTACK,
                        dim = used,
                        onClick = if (canAttack) ({ onFieldCardTap(card.id) }) else null,
                    )
                }

                when (state.phase) {
                    TurnPhase.MAIN -> {
                        SectionLabel("ベンチ (${me.bench.size}/${GameConfig.BENCH_SIZE})")
                        CardRow(me.bench) { card ->
                            val canMoveToField = me.field.size < GameConfig.MAX_ATTACKS_PER_TURN
                            CardTile(
                                card,
                                mode = CardStatMode.ATTACK,
                                dim = !canMoveToField,
                                onClick = if (canMoveToField) ({ onBenchToField(card.id) }) else null,
                            )
                        }
                        Text(
                            "ベンチのカードをタップで場へ。手札のカードをタップでベンチへ補充。",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        )
                        SectionLabel("手札 (${me.hand.size}枚)")
                        CardRow(me.hand) { card ->
                            val canAdd = me.bench.size < GameConfig.BENCH_SIZE
                            CardTile(card, dim = !canAdd, onClick = if (canAdd) ({ onHandToBench(card.id) }) else null)
                        }
                    }
                    TurnPhase.BATTLE -> {
                        SectionLabel("ベンチ")
                        CardRow(me.bench) { card -> CardTile(card) }
                        Text(
                            "場のカードをタップして攻撃を宣言（このターン未使用のカードのみ）。",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        )
                        SectionLabel("手札 (${me.hand.size}枚・非公開)")
                        CardRow(me.hand) { card -> CardTile(card) }
                    }
                    else -> {}
                }

                if (state.log.isNotEmpty()) {
                    Text(
                        state.log.takeLast(6).joinToString("\n"),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
        ) {
            when (state.phase) {
                TurnPhase.MAIN -> Button(
                    onClick = onToBattle,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) { Text("バトルフェイズへ →") }
                TurnPhase.BATTLE -> {
                    val usedUp = state.attacksUsedThisTurn >= GameConfig.MAX_ATTACKS_PER_TURN
                    Button(onClick = onEndTurn, modifier = Modifier.fillMaxWidth()) {
                        Text(if (usedUp) "手番を終了 →" else "こうげきを終了して手番へ →")
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun StatusBar(state: GameState) {
    val pid = state.currentPlayer
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            playerLabel(pid) + " のターン (${state.turnNumber})",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier
                .background(attributeColorForPlayer(pid), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LifePips(state.player(PlayerId.PLAYER_ONE).life.size, attributeColorForPlayer(PlayerId.PLAYER_ONE))
            Text(
                playerLabel(PlayerId.PLAYER_ONE) + " vs " + playerLabel(PlayerId.PLAYER_TWO),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            LifePips(state.player(PlayerId.PLAYER_TWO).life.size, attributeColorForPlayer(PlayerId.PLAYER_TWO))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(
                TurnPhase.BENCH_RECOVERY to "回復",
                TurnPhase.DRAW to "ドロー",
                TurnPhase.MAIN to "メイン",
                TurnPhase.BATTLE to "バトル",
            ).forEach { (phase, label) ->
                val active = phase == state.phase
                Text(
                    label,
                    fontSize = 10.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(6.dp),
                        )
                        .padding(vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun LifePips(count: Int, filledColor: Color) {
    val emptyBorder = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(GameConfig.LIFE_COUNT) { i ->
            Column(
                modifier = Modifier
                    .size(11.dp)
                    .then(
                        if (i < count) {
                            Modifier.background(filledColor, RoundedCornerShape(3.dp))
                        } else {
                            Modifier.border(1.dp, emptyBorder, RoundedCornerShape(3.dp))
                        },
                    ),
            ) {}
        }
    }
}

@Composable
private fun ZoneBlock(
    title: String,
    trailing: String,
    background: Color,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            Text(trailing, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        }
        content()
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        modifier = Modifier.padding(horizontal = 12.dp, top = 6.dp),
    )
}

@Composable
private fun CardRow(cards: List<Card>, cardContent: @Composable (Card) -> Unit) {
    if (cards.isEmpty()) {
        Text(
            "（なし）",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        cards.forEach { card -> cardContent(card) }
    }
}
