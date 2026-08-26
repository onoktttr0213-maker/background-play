package com.logicpunch.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.logicpunch.core.AttackResult
import com.logicpunch.core.Card
import com.logicpunch.core.CpuPlayer
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
    /** CPU自身が仕掛けた攻撃（防御側が人間）かどうか。結果確認後にCPUの手番へ自動で戻る。 */
    val cpuAttack: Boolean = false,
    /** 防御側がCPUで即座に解決された等、パス画面を挟む必要が無い場合。 */
    val skipPassBack: Boolean = false,
)

/**
 * GameEngine（純Kotlin・非Compose）をラップし、Compose から観測できる状態に変換するViewModel。
 * engine.state 内のリストは通常のMutableListなので、変更のたびに [revision] をインクリメントして
 * 画面の再コンポーズを促す。
 */
class GameViewModel : ViewModel() {
    private val engine = GameEngine()

    /** CPUは常にPLAYER_TWOとして参加する。 */
    private val cpuId = PlayerId.PLAYER_TWO

    /** CPUが勝てる手を何ターン連続で見送ったか（手詰まり回避のフォールバック判定用）。 */
    private var cpuPassiveStreak = 0

    var revision by mutableIntStateOf(0)
        private set

    val state: GameState get() = engine.state

    var vsCpu by mutableStateOf(false)
        private set
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

    fun startGame(vsCpu: Boolean) {
        this.vsCpu = vsCpu
        cpuPassiveStreak = 0
        engine.setupNewGame(firstPlayer = PlayerId.PLAYER_ONE)
        if (vsCpu) {
            screen = Screen.BENCH_SELECT
            benchSelectPlayer = PlayerId.PLAYER_ONE
            benchSelection = emptyList()
            bump()
        } else {
            goPass(PlayerId.PLAYER_ONE, "ベンチに置くカードを選びます。") {
                screen = Screen.BENCH_SELECT
                benchSelectPlayer = PlayerId.PLAYER_ONE
                benchSelection = emptyList()
            }
        }
    }

    /**
     * ベンチ回復＋ドローを行い、次のプレイヤーのターン画面へ進める。
     * CPU対戦中でCPUの番なら、そのままCPUのターンを自動で進行する。
     * CPU対戦中の人間の番は、画面を渡す相手がいないのでパス画面を省略する。
     */
    private fun startNextTurn() {
        if (vsCpu && engine.state.currentPlayer == cpuId) {
            engine.beginTurnBenchRecovery()
            engine.drawPhase()
            runCpuTurn()
        } else if (vsCpu) {
            engine.beginTurnBenchRecovery()
            engine.drawPhase()
            screen = Screen.TURN
            bump()
        } else {
            goPass(engine.state.currentPlayer, "あなたのターンです。") {
                engine.beginTurnBenchRecovery()
                engine.drawPhase()
                screen = Screen.TURN
            }
        }
    }

    private fun runCpuTurn() {
        CpuPlayer.runMainPhase(engine, cpuId)
        advanceCpuBattle()
    }

    /** CPUの番のバトルフェイズを1手ずつ進める。攻撃があれば人間の防御コンボ入力へ、無ければ手番終了。 */
    private fun advanceCpuBattle() {
        var plan = CpuPlayer.pickNextAttack(engine, cpuId)
        if (plan != null) {
            cpuPassiveStreak = 0
        } else {
            val cpu = engine.playerState(cpuId)
            val human = engine.playerState(cpuId.opponent())
            val hasUnusedField = cpu.field.any { it.id !in engine.state.attackedFieldCardIdsThisTurn }
            val hasTarget = human.bench.isNotEmpty() || human.field.isNotEmpty()
            if (hasUnusedField && hasTarget) {
                cpuPassiveStreak++
                if (cpuPassiveStreak >= 2) {
                    plan = CpuPlayer.worstCaseFallback(engine, cpuId)
                    cpuPassiveStreak = 0
                }
            } else {
                cpuPassiveStreak = 0
            }
        }

        if (plan == null) {
            engine.endTurn()
            if (engine.state.phase == TurnPhase.GAME_OVER) {
                screen = Screen.GAME_OVER
                bump()
                return
            }
            startNextTurn()
            return
        }

        val cpu = engine.playerState(cpuId)
        val attackerCard = cpu.field.first { it.id == plan.attackerCardId }
        val comboSnapshot = plan.comboCardIds.mapNotNull { id -> cpu.hand.firstOrNull { it.id == id } }
        val targetInfo = engine.playerState(cpuId.opponent()).let { human ->
            human.bench.firstOrNull { it.id == plan.targetCardId } ?: human.field.firstOrNull { it.id == plan.targetCardId }
        }
        battleDraft = BattleDraft(
            attackerCard = attackerCard,
            targetCard = targetInfo,
            atkCombo = plan.comboCardIds,
            atkComboSnapshot = comboSnapshot,
            step = BattleStep.DEFENDER_COMBO,
            cpuAttack = true,
        )
        screen = Screen.TURN
        bump()
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
            if (vsCpu) {
                engine.placeInitialBench(cpuId, CpuPlayer.chooseBenchCardIds(engine.playerState(cpuId).hand))
                startNextTurn()
            } else {
                goPass(PlayerId.PLAYER_TWO, "ベンチに置くカードを選びます。") {
                    screen = Screen.BENCH_SELECT
                    benchSelectPlayer = PlayerId.PLAYER_TWO
                    benchSelection = emptyList()
                }
            }
        } else {
            startNextTurn()
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
        startNextTurn()
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
        val target = draft.targetCard ?: return
        val hand = engine.currentPlayerState().hand
        val snapshot = draft.atkCombo.mapNotNull { id -> hand.firstOrNull { it.id == id } }
        val defenderId = engine.state.currentPlayer.opponent()
        if (vsCpu && defenderId == cpuId) {
            // CPUの防御はパス画面なしで即座に決着する（人間はずっと画面を持ったまま）。
            val defCombo = CpuPlayer.chooseDefenseCombo(draft.attackerCard, target, snapshot, engine.playerState(cpuId).hand)
            val result = engine.declareAttack(
                attackerFieldCardId = draft.attackerCard.id,
                defenderTargetCardId = target.id,
                attackerComboHandCardIds = draft.atkCombo,
                defenderComboHandCardIds = defCombo,
            )
            battleDraft = draft.copy(
                atkComboSnapshot = snapshot,
                defCombo = defCombo,
                result = result,
                step = BattleStep.RESULT,
                skipPassBack = true,
            )
            bump()
        } else {
            battleDraft = draft.copy(atkComboSnapshot = snapshot, step = BattleStep.PASS_DEFENDER)
        }
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
        if (draft.cpuAttack) {
            battleDraft = null
            advanceCpuBattle()
            return
        }
        if (draft.skipPassBack) {
            battleDraft = null
            bump()
            return
        }
        battleDraft = draft.copy(step = BattleStep.PASS_BACK)
    }

    fun backToAttacker() {
        battleDraft = null
        bump()
    }

    fun restart() {
        screen = Screen.TITLE
        vsCpu = false
        cpuPassiveStreak = 0
        battleDraft = null
        passInfo = null
        bump()
    }
}
