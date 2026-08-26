package com.logicpunch.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TitleScreen(onStartCpu: () -> Unit, onStartPvp: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "正論パンチ（仮）",
            fontSize = 30.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            "日常の小言バトル。5つのテーマ40枚のカードで、相手を正論で追い詰めろ。" +
                "2人で1台のスマホを交互に渡すローカル対戦と、1人でも遊べるCPU対戦があります。",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Button(
            onClick = onStartCpu,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("CPU対戦（1人でテストプレイ）", fontSize = 16.sp)
        }
        Button(
            onClick = onStartPvp,
            modifier = Modifier.padding(top = 10.dp),
        ) {
            Text("2人対戦（パス&プレイ）", fontSize = 15.sp)
        }
        Column(
            modifier = Modifier
                .padding(top = 20.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .padding(16.dp),
        ) {
            Text("ルール要約", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 13.sp)
            Text(
                "・共通山札40枚 / ライフ4枚（裏向き）を奪い合う2人対戦。\n" +
                    "・攻撃力 > 防御力なら「論破成功」→ 相手はライフを1枚失い（自分の手札に加わる）、双方の使用カードは捨て札。\n" +
                    "・防御力 ≧ 攻撃力なら「ブロック成功」→ 防御側カードは場に残る。\n" +
                    "・属性は 赤>青>緑>赤 の三すくみ。攻撃側が有利な属性なら攻撃力+1000（防御側にボーナスなし）。\n" +
                    "・手札から最大3枚を捨てて「コンボ」値を上乗せできる（攻守どちらも）。\n" +
                    "・1ターンに出せる場のカードは最大3枚、攻撃も1ターン最大3回（1枚1回まで）。",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            )
        }
    }
}
