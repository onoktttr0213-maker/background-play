package com.logicpunch.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.logicpunch.core.AttackResult
import com.logicpunch.core.Card
import com.logicpunch.core.GameConfig
import com.logicpunch.core.GameEngine
import com.logicpunch.core.GameState
import com.logicpunch.core.PlayerId
import com.logicpunch.core.TurnPhase

enum class Screen { TITLE, PASS, BENCH_SELECT, TURN, GAME_OVER }

enum class BattleStep { PICK_TARGET, ATTACKER_COMBO, PASS_DEFENDER, DEFENDER_COMBO, RESULT, PASS_BACK }

data class PassInfo(val forPlayer: PlayerId, val message: String, val onReveal: () -> Unit)

/** 攻撃 1 回分のドラフト状態。各更新は新しいインスタンスを作る（Compose の状態変化検知のため）。 */
data class BattleDraft(
    val attackerCard: Card,
    val targetCard: Card? = null,
    val atkCombo: List<String> = emptyList(),
    val defCombo: List<String> = emptyList(),
    val atkComboSnapshot: List<Card> = emptyList(),
    val result: AttackResult? = null,
    val step: BattleStep = BattleStep.PICK_TARGET,
)

/**
 * GameEngine（純Kotlin・非Compose）をラップし、Compose から観測できる状態に変換するViewModel。
 * engine.state 内のリストは通常のMutableListなので、変更のたびに [revision] をインクリメントして
 * 画面の再コンポーズを促す。
 */
class GameViewModel : ViewModel() {
    private val engine = GameEngine()

    var revision by mutableIntStateOf(0)
        private set

    val state: GameState get() = engine.state

    var screen by mutableStateOf(Screen.TITLE)
        private set
    var passInfo by mutableStateOf<PassInfo?>(null)
        private set
    var benchSelectPlayer by mutableStateOf<PlayerId?>(null)
        private set
    var benchSelection by mutableStateOf<List<String>>(emptyList())
        private set
    var battleDraft by mutableStateOf<BattleDraft?>(null)
        private set

    private fun bump() {
        revision++
    }

    private fun goPass(forPlayer: PlayerId, message: String, onReveal: () -> Unit) {
        passInfo = PassInfo(forPlayer, message, onReveal)
        screen = Screen.PASS
        bump()
    }

    fun startGame() {
        engine.setupNewGame(firstPlayer = PlayerId.PLAYER_ONE)
        goPass(PlayerId.PLAYER_ONE, "ベンチに置くカードを選びます。") {
            screen = Screen.BENCH_SELECT
            benchSelectPlayer = PlayerId.PLAYER_ONE
            benchSelection = emptyList()
        }
    }

    fun revealPass() {
        val info = passInfo ?: return
        passInfo = null
        info.onReveal()
        bump()
    }

    fun toggleBenchSelection(cardId: String) {
        benchSelection = when {
            benchSelection.contains(cardId) -> benchSelection - cardId
            benchSelection.size < GameConfig.BENCH_SIZE -> benchSelection + cardId
            else -> benchSelection
        }
    }

    fun confirmBenchSelection() {
        val pid = benchSelectPlayer ?: return
        engine.placeInitialBench(pid, benchSelection)
        if (pid == PlayerId.PLAYER_ONE) {
            goPass(PlayerId.PLAYER_TWO, "ベンチに置くカードを選びます。") {
                screen = Screen.BENCH_SELECT
                benchSelectPlayer = PlayerId.PLAYER_TWO
                benchSelection = emptyList()
            }
        } else {
            goPass(engine.state.currentPlayer, "あなたのターンです。") {
                engine.beginTurnBenchRecovery()
                engine.drawPhase()
                screen = Screen.TURN
            }
        }
    }

    fun moveBenchToField(cardId: String) {
        engine.moveBenchToField(cardId)
        bump()
    }

    fun moveHandToBench(cardId: String) {
        engine.moveHandToBench(cardId)
        bump()
    }

    fun toBattlePhase() {
        engine.endMainPhase()
        bump()
    }

    fun endTurn() {
        engine.endTurn()
        if (engine.state.phase == TurnPhase.GAME_OVER) {
            screen = Screen.GAME_OVER
            bump()
            return
        }
        goPass(engine.state.currentPlayer, "あなたのターンです。") {
            engine.beginTurnBenchRecovery()
            engine.drawPhase()
            screen = Screen.TURN
        }
    }

    fun startAttack(fieldCardId: String) {
        if (engine.state.phase != TurnPhase.BATTLE) return
        if (engine.state.attackedFieldCardIdsThisTurn.contains(fieldCardId)) return
        if (engine.state.attacksUsedThisTurn >= GameConfig.MAX_ATTACKS_PER_TURN) return
        val card = engine.currentPlayerState().field.firstOrNull { it.id == fieldCardId } ?: return
        battleDraft = BattleDraft(attackerCard = card)
        bump()
    }

    fun cancelBattle() {
        battleDraft = null
        bump()
    }

    fun pickTarget(cardId: String) {
        val draft = battleDraft ?: return
        val opp = engine.opponentState()
        val target = opp.bench.firstOrNull { it.id == cardId } ?: opp.field.firstOrNull { it.id == cardId } ?: return
        battleDraft = draft.copy(targetCard = target, step = BattleStep.ATTACKER_COMBO)
    }

    fun toggleAtkCombo(cardId: String) {
        val draft = battleDraft ?: return
        val newList = when {
            draft.atkCombo.contains(cardId) -> draft.atkCombo - cardId
            draft.atkCombo.size < GameConfig.COMBO_MAX_CARDS_FROM_HAND -> draft.atkCombo + cardId
            else -> draft.atkCombo
        }
        battleDraft = draft.copy(atkCombo = newList)
    }

    fun confirmAttackerCombo() {
        val draft = battleDraft ?: return
        val hand = engine.currentPlayerState().hand
        val snapshot = draft.atkCombo.mapNotNull { id -> hand.firstOrNull { it.id == id } }
        battleDraft = draft.copy(atkComboSnapshot = snapshot, step = BattleStep.PASS_DEFENDER)
    }

    fun revealDefender() {
        val draft = battleDraft ?: return
        battleDraft = draft.copy(step = BattleStep.DEFENDER_COMBO)
    }

    fun toggleDefCombo(cardId: String) {
        val draft = battleDraft ?: return
        val newList = when {
            draft.defCombo.contains(cardId) -> draft.defCombo - cardId
            draft.defCombo.size < GameConfig.COMBO_MAX_CARDS_FROM_HAND -> draft.defCombo + cardId
            else -> draft.defCombo
        }
        battleDraft = draft.copy(defCombo = newList)
    }

    fun confirmDefense() {
        val draft = battleDraft ?: return
        val target = draft.targetCard ?: return
        val result = engine.declareAttack(
            attackerFieldCardId = draft.attackerCard.id,
            defenderTargetCardId = target.id,
            attackerComboHandCardIds = draft.atkCombo,
            defenderComboHandCardIds = draft.defCombo,
        )
        battleDraft = draft.copy(result = result, step = BattleStep.RESULT)
        bump()
    }

    fun ackResult() {
        if (engine.state.phase == TurnPhase.GAME_OVER) {
            battleDraft = null
            screen = Screen.GAME_OVER
            bump()
            return
        }
        val draft = battleDraft ?: return
        battleDraft = draft.copy(step = BattleStep.PASS_BACK)
    }

    fun backToAttacker() {
        battleDraft = null
        bump()
    }

    fun restart() {
        screen = Screen.TITLE
        battleDraft = null
        passInfo = null
        bump()
    }
}
