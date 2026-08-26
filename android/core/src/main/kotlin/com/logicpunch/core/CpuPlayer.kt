package com.logicpunch.core

/** CPUが選んだ1回分の攻撃プラン。 */
data class CpuAttackPlan(val attackerCardId: String, val targetCardId: String, val comboCardIds: List<String>)

/**
 * テストプレイ用のシンプルなヒューリスティックAI。常に公開情報（盤面）と自分の手札だけを見て
 * 意思決定し、相手の手札を覗き見ることはしない（人間のプレイヤーと同じ情報量でフェアに戦う）。
 * GameEngine の外側から呼び出す、ステートレスなユーティリティ。
 */
object CpuPlayer {

    private fun cardValue(c: Card): Int = c.attack + c.defense + c.combo

    /** 準備フェーズ: 手札のうち評価値が低いカードからベンチへ。 */
    fun chooseBenchCardIds(hand: List<Card>): List<String> =
        hand.sortedBy { cardValue(it) }.take(GameConfig.BENCH_SIZE).map { it.id }

    /**
     * メインフェイズ: ベンチ→場は攻撃力の高い順に、手札→ベンチは評価値の低い順に補充する。
     * GameEngine のベンチ/場移動系メソッドは常に「今の手番のプレイヤー」に対して作用するため、
     * [playerId] は必ず engine.state.currentPlayer と一致していなければならない。
     */
    fun runMainPhase(engine: GameEngine, playerId: PlayerId) {
        require(engine.state.currentPlayer == playerId) {
            "runMainPhase must be called on the current player's own turn (current=${engine.state.currentPlayer}, requested=$playerId)"
        }
        val p = engine.playerState(playerId)
        while (p.field.size < GameConfig.MAX_ATTACKS_PER_TURN && p.bench.isNotEmpty()) {
            val strongest = p.bench.maxByOrNull { it.attack } ?: break
            engine.moveBenchToField(strongest.id)
        }
        while (p.bench.size < GameConfig.BENCH_SIZE && p.hand.isNotEmpty()) {
            val weakest = p.hand.minByOrNull { cardValue(it) } ?: break
            engine.moveHandToBench(weakest.id)
        }
        engine.endMainPhase()
    }

    private data class Feasibility(val comboIds: List<String>, val margin: Int)

    /** attacker で target を攻略するのに必要な最小コンボ（0～3枚）。勝てなければ null。 */
    private fun attackFeasibility(attacker: Card, target: Card, availableHand: List<Card>): Feasibility? {
        val attrBonus = if (hasAttributeAdvantage(attacker.attribute, target.attribute)) GameConfig.ATTRIBUTE_ADVANTAGE_BONUS else 0
        val base = attacker.attack + attrBonus
        if (base > target.defense) return Feasibility(emptyList(), base - target.defense)
        val sorted = availableHand.sortedByDescending { it.combo }
        var total = base
        val used = mutableListOf<String>()
        for (c in sorted.take(GameConfig.COMBO_MAX_CARDS_FROM_HAND)) {
            total += c.combo
            used.add(c.id)
            if (total > target.defense) return Feasibility(used.toList(), total - target.defense)
        }
        return null
    }

    /** 今ターンまだ使っていない場カードの中から、最も効率よく（少ないコンボで）勝てる攻撃を1つ選ぶ。 */
    fun pickNextAttack(engine: GameEngine, playerId: PlayerId): CpuAttackPlan? {
        val p = engine.playerState(playerId)
        val o = engine.playerState(playerId.opponent())
        val attackedIds = engine.state.attackedFieldCardIdsThisTurn
        val targets = o.bench + o.field
        val attackers = p.field.filter { it.id !in attackedIds }

        var best: CpuAttackPlan? = null
        var bestComboCount = Int.MAX_VALUE
        var bestTargetValue = Int.MIN_VALUE
        for (atk in attackers) {
            for (tgt in targets) {
                val feas = attackFeasibility(atk, tgt, p.hand) ?: continue
                val comboCount = feas.comboIds.size
                val better = best == null || comboCount < bestComboCount ||
                    (comboCount == bestComboCount && tgt.defense > bestTargetValue)
                if (better) {
                    best = CpuAttackPlan(atk.id, tgt.id, feas.comboIds)
                    bestComboCount = comboCount
                    bestTargetValue = tgt.defense
                }
            }
        }
        return best
    }

    /**
     * 勝てる攻撃が無いまま手詰まりが続いたときの回避策。勝てなくても一番負け幅の小さい
     * 攻撃を選ぶ（コンボは使わない、盤面を動かすためだけの捨て身の一手）。
     */
    fun worstCaseFallback(engine: GameEngine, playerId: PlayerId): CpuAttackPlan? {
        val p = engine.playerState(playerId)
        val o = engine.playerState(playerId.opponent())
        val attackedIds = engine.state.attackedFieldCardIdsThisTurn
        val targets = o.bench + o.field
        val attackers = p.field.filter { it.id !in attackedIds }

        var best: CpuAttackPlan? = null
        var bestMargin = Int.MIN_VALUE
        for (atk in attackers) {
            for (tgt in targets) {
                val attrBonus = if (hasAttributeAdvantage(atk.attribute, tgt.attribute)) GameConfig.ATTRIBUTE_ADVANTAGE_BONUS else 0
                val margin = (atk.attack + attrBonus) - tgt.defense
                if (best == null || margin > bestMargin) {
                    best = CpuAttackPlan(atk.id, tgt.id, emptyList())
                    bestMargin = margin
                }
            }
        }
        return best
    }

    /** 防御側として、なるべく少ないコンボでブロックを狙う。届かないなら何も使わない（手札を無駄にしない）。 */
    fun chooseDefenseCombo(attacker: Card, target: Card, attackerComboCards: List<Card>, hand: List<Card>): List<String> {
        val attrBonus = if (hasAttributeAdvantage(attacker.attribute, target.attribute)) GameConfig.ATTRIBUTE_ADVANTAGE_BONUS else 0
        val attackTotal = attacker.attack + attrBonus + attackerComboCards.sumOf { it.combo }
        if (target.defense >= attackTotal) return emptyList()
        val sorted = hand.sortedByDescending { it.combo }
        var total = target.defense
        val used = mutableListOf<String>()
        for (c in sorted.take(GameConfig.COMBO_MAX_CARDS_FROM_HAND)) {
            total += c.combo
            used.add(c.id)
            if (total >= attackTotal) return used.toList()
        }
        return emptyList()
    }
}
