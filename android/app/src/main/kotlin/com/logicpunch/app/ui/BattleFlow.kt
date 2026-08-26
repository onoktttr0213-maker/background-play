package com.logicpunch.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logicpunch.app.BattleDraft
import com.logicpunch.core.AttackResult
import com.logicpunch.core.Card
import com.logicpunch.core.GameConfig
import com.logicpunch.core.PlayerId
import com.logicpunch.core.hasAttributeAdvantage

@Composable
fun PickTargetScreen(draft: BattleDraft, targets: List<Card>, onPick: (String) -> Unit, onCancel: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("攻撃対象を選択", fontSize = 18.sp)
            Text(
                "攻撃カード: " + draft.attackerCard.id + "「" + draft.attackerCard.attackText + "」",
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
            items(targets, key = { it.id }) { card ->
                CardTile(card, mode = CardStatMode.DEFENSE, modifier = Modifier.fillMaxWidth(), onClick = { onPick(card.id) })
            }
        }
        TextButton(onClick = onCancel, modifier = Modifier.padding(16.dp)) { Text("キャンセル") }
    }
}

@Composable
fun AttackerComboScreen(
    attackerId: PlayerId,
    draft: BattleDraft,
    hand: List<Card>,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val target = draft.targetCard ?: return
    val advantage = hasAttributeAdvantage(draft.attackerCard.attribute, target.attribute)
    val comboSum = draft.atkCombo.sumOf { id -> hand.firstOrNull { it.id == id }?.combo ?: 0 }
    val total = draft.attackerCard.attack + (if (advantage) GameConfig.ATTRIBUTE_ADVANTAGE_BONUS else 0) + comboSum

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(playerLabel(attackerId) + ": コンボを追加（任意・最大${GameConfig.COMBO_MAX_CARDS_FROM_HAND}枚）", fontSize = 18.sp)
            Text(
                "対象: ${target.id}" + if (advantage) "（属性有利 +${GameConfig.ATTRIBUTE_ADVANTAGE_BONUS}）" else "",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            TotalBanner("攻撃合計: $total")
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(hand, key = { it.id }) { card ->
                val selected = draft.atkCombo.contains(card.id)
                val dim = !selected && draft.atkCombo.size >= GameConfig.COMBO_MAX_CARDS_FROM_HAND
                CardTile(
                    card = card,
                    mode = CardStatMode.ATTACK,
                    modifier = Modifier.fillMaxWidth(),
                    selected = selected,
                    dim = dim,
                    onClick = if (dim) null else ({ onToggle(card.id) }),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel) { Text("キャンセル") }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) { Text("攻撃を宣言 →") }
        }
    }
}

@Composable
fun PassToDefenderScreen(attacker: PlayerId, defender: PlayerId, attackerCardId: String, onReveal: () -> Unit) {
    PassScreen(
        forPlayer = defender,
        message = playerLabel(attacker) + " が「" + attackerCardId + "」で攻撃を宣言しました。防御を選んでください。",
        onReveal = onReveal,
    )
}

@Composable
fun DefenderComboScreen(
    draft: BattleDraft,
    defenderHand: List<Card>,
    defenderId: PlayerId,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val target = draft.targetCard ?: return
    val advantage = hasAttributeAdvantage(draft.attackerCard.attribute, target.attribute)
    val atkComboSum = draft.atkComboSnapshot.sumOf { it.combo }
    val attackTotalPreview = draft.attackerCard.attack + (if (advantage) GameConfig.ATTRIBUTE_ADVANTAGE_BONUS else 0) + atkComboSum
    val defComboSum = draft.defCombo.sumOf { id -> defenderHand.firstOrNull { it.id == id }?.combo ?: 0 }
    val defTotal = target.defense + defComboSum

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(playerLabel(defenderId) + ": 防御コンボを追加（任意・最大${GameConfig.COMBO_MAX_CARDS_FROM_HAND}枚）", fontSize = 18.sp)
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Matchup(draft.attackerCard, target)
            TotalBanner("攻撃 $attackTotalPreview  :  防御 $defTotal")
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(defenderHand, key = { it.id }) { card ->
                val selected = draft.defCombo.contains(card.id)
                val dim = !selected && draft.defCombo.size >= GameConfig.COMBO_MAX_CARDS_FROM_HAND
                CardTile(
                    card = card,
                    mode = CardStatMode.DEFENSE,
                    modifier = Modifier.fillMaxWidth(),
                    selected = selected,
                    dim = dim,
                    onClick = if (dim) null else ({ onToggle(card.id) }),
                )
            }
        }
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) { Text("防御を確定 → 判定") }
    }
}

@Composable
fun BattleResultScreen(result: AttackResult, defenderId: PlayerId, vsCpu: Boolean = false, onAck: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("バトル結果", fontSize = 18.sp, modifier = Modifier.padding(16.dp))
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Matchup(result.attackerCard, result.defenderCard)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (result.attributeAdvantage) Badge("属性有利 +${GameConfig.ATTRIBUTE_ADVANTAGE_BONUS}")
                if (result.attackerComboCards.isNotEmpty()) Badge("攻コンボ +${result.attackerComboCards.sumOf { it.combo }}")
                if (result.defenderComboCards.isNotEmpty()) Badge("防コンボ +${result.defenderComboCards.sumOf { it.combo }}")
            }
            TotalBanner("${result.attackTotal} vs ${result.defenseTotal}")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (result.attackSucceeded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(14.dp),
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (result.attackSucceeded) "論破成功！" else "ブロック成功",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (result.attackSucceeded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                if (result.attackSucceeded) {
                    seatLabel(defenderId, vsCpu) + " はライフを1枚失いました（相手の手札に加わりました）"
                } else {
                    result.defenderCard.id + " は場に残りました"
                },
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        Button(
            onClick = onAck,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) { Text("OK") }
    }
}

@Composable
private fun Matchup(attacker: Card, defender: Card) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .horizontalScroll(rememberScrollState())
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CardTile(attacker, mode = CardStatMode.ATTACK)
        Text("VS", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        CardTile(defender, mode = CardStatMode.DEFENSE)
    }
}

@Composable
private fun TotalBanner(text: String) {
    Text(
        text,
        fontSize = 22.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(10.dp),
    )
}

@Composable
private fun Badge(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}
