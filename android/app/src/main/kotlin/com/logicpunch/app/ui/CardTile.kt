package com.logicpunch.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logicpunch.app.ui.theme.LocalAttributeColors
import com.logicpunch.core.Attribute
import com.logicpunch.core.Card

enum class CardStatMode { ATTACK, DEFENSE, BOTH }

@Composable
fun attributeColor(attribute: Attribute): Color {
    val colors = LocalAttributeColors.current
    return when (attribute) {
        Attribute.RED -> colors.red
        Attribute.BLUE -> colors.blue
        Attribute.GREEN -> colors.green
    }
}

@Composable
fun CardTile(
    card: Card,
    modifier: Modifier = Modifier,
    mode: CardStatMode = CardStatMode.BOTH,
    selected: Boolean = false,
    dim: Boolean = false,
    showExcuse: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
    val borderWidth = if (selected) 3.dp else 2.dp
    Column(
        modifier = modifier
            .width(136.dp)
            .alpha(if (dim) 0.4f else 1f)
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when (card.attribute) {
                    Attribute.RED -> "赤"
                    Attribute.BLUE -> "青"
                    Attribute.GREEN -> "緑"
                },
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(attributeColor(card.attribute), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 1.dp),
            )
            Text(
                text = card.role.label.removeSuffix("型"),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
        Text(
            text = card.theme,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = "「" + (if (showExcuse) card.excuseText else card.attackText) + "」",
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            when (mode) {
                CardStatMode.ATTACK -> {
                    StatCell("攻撃", card.attack, Modifier.weight(1f), highlight = true)
                    StatCell("コンボ", card.combo, Modifier.weight(1f))
                }
                CardStatMode.DEFENSE -> {
                    StatCell("防御", card.defense, Modifier.weight(1f), highlight = true)
                    StatCell("コンボ", card.combo, Modifier.weight(1f))
                }
                CardStatMode.BOTH -> {
                    StatCell("攻", card.attack, Modifier.weight(1f))
                    StatCell("防", card.defense, Modifier.weight(1f))
                    StatCell("combo", card.combo, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: Int, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Text(
            value.toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Role の日本語ラベル（"攻撃型" など）。core モジュールは enum のみ保持するため UI 側でマッピングする。 */
val com.logicpunch.core.Role.label: String
    get() = when (this) {
        com.logicpunch.core.Role.ATTACK -> "攻撃型"
        com.logicpunch.core.Role.BALANCE -> "バランス型"
        com.logicpunch.core.Role.DEFENSE -> "防御型"
        com.logicpunch.core.Role.COMBO -> "コンボ型"
    }
