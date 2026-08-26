package com.logicpunch.app

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.logicpunch.app.ui.AttackerComboScreen
import com.logicpunch.app.ui.BattleResultScreen
import com.logicpunch.app.ui.BenchSelectScreen
import com.logicpunch.app.ui.DefenderComboScreen
import com.logicpunch.app.ui.GameOverScreen
import com.logicpunch.app.ui.PassScreen
import com.logicpunch.app.ui.PassToDefenderScreen
import com.logicpunch.app.ui.PickTargetScreen
import com.logicpunch.app.ui.TitleScreen
import com.logicpunch.app.ui.TurnScreen
import com.logicpunch.core.PlayerId

@Composable
fun GameApp(viewModel: GameViewModel = viewModel()) {
    // Reading `revision` here ties this whole composable's recomposition to every
    // engine mutation, since GameEngine's internal lists are plain (non-Compose) state.
    @Suppress("UNUSED_VARIABLE") val revision = viewModel.revision

    when (viewModel.screen) {
        Screen.TITLE -> TitleScreen(onStart = viewModel::startGame)

        Screen.PASS -> {
            val info = viewModel.passInfo
            if (info != null) {
                PassScreen(forPlayer = info.forPlayer, message = info.message, onReveal = viewModel::revealPass)
            }
        }

        Screen.BENCH_SELECT -> {
            val pid = viewModel.benchSelectPlayer
            if (pid != null) {
                BenchSelectScreen(
                    player = pid,
                    hand = viewModel.state.player(pid).hand,
                    selected = viewModel.benchSelection,
                    onToggle = viewModel::toggleBenchSelection,
                    onConfirm = viewModel::confirmBenchSelection,
                )
            }
        }

        Screen.TURN -> {
            val draft = viewModel.battleDraft
            if (draft == null) {
                TurnScreen(
                    state = viewModel.state,
                    onFieldCardTap = viewModel::startAttack,
                    onBenchToField = viewModel::moveBenchToField,
                    onHandToBench = viewModel::moveHandToBench,
                    onToBattle = viewModel::toBattlePhase,
                    onEndTurn = viewModel::endTurn,
                )
            } else {
                BattleDraftFlow(viewModel, draft)
            }
        }

        Screen.GAME_OVER -> {
            val winner = viewModel.state.winner
            if (winner != null) {
                GameOverScreen(winner = winner, onRestart = viewModel::restart)
            }
        }
    }
}

@Composable
private fun BattleDraftFlow(viewModel: GameViewModel, draft: BattleDraft) {
    val attackerId = viewModel.state.currentPlayer
    val defenderId = if (attackerId == PlayerId.PLAYER_ONE) PlayerId.PLAYER_TWO else PlayerId.PLAYER_ONE

    when (draft.step) {
        BattleStep.PICK_TARGET -> {
            val opp = viewModel.state.player(defenderId)
            PickTargetScreen(
                draft = draft,
                targets = opp.bench + opp.field,
                onPick = viewModel::pickTarget,
                onCancel = viewModel::cancelBattle,
            )
        }

        BattleStep.ATTACKER_COMBO -> {
            AttackerComboScreen(
                attackerId = attackerId,
                draft = draft,
                hand = viewModel.state.player(attackerId).hand,
                onToggle = viewModel::toggleAtkCombo,
                onConfirm = viewModel::confirmAttackerCombo,
                onCancel = viewModel::cancelBattle,
            )
        }

        BattleStep.PASS_DEFENDER -> {
            PassToDefenderScreen(
                attacker = attackerId,
                defender = defenderId,
                attackerCardId = draft.attackerCard.id,
                onReveal = viewModel::revealDefender,
            )
        }

        BattleStep.DEFENDER_COMBO -> {
            DefenderComboScreen(
                draft = draft,
                defenderHand = viewModel.state.player(defenderId).hand,
                defenderId = defenderId,
                onToggle = viewModel::toggleDefCombo,
                onConfirm = viewModel::confirmDefense,
            )
        }

        BattleStep.RESULT -> {
            val result = draft.result
            if (result != null) {
                BattleResultScreen(result = result, defenderId = defenderId, onAck = viewModel::ackResult)
            }
        }

        BattleStep.PASS_BACK -> {
            PassScreen(
                forPlayer = attackerId,
                message = "結果を確認しました。画面を " + com.logicpunch.app.ui.playerLabel(attackerId) + " に返してください。",
                onReveal = viewModel::backToAttacker,
            )
        }
    }
}
