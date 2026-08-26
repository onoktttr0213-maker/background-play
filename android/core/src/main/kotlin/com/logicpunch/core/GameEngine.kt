package com.logicpunch.core

import kotlin.random.Random

class IllegalMoveException(message: String) : IllegalStateException(message)

data class AttackResult(
    val attackerId: PlayerId,
    val attackerCard: Card,
    val defenderCard: Card,
    val defenderZone: Zone,
    val attributeAdvantage: Boolean,
    val attackerComboCards: List<Card>,
    val defenderComboCards: List<Card>,
    val attackTotal: Int,
    val defenseTotal: Int,
    /** true = 論破成功（攻撃側の勝ち、防御側はライフを1枚失う） */
    val attackSucceeded: Boolean,
    val lifeCardMoved: Card?,
)

/**
 * 「正論パンチ（仮）」Ver.0.7 のルールエンジン。
 * 純粋な Kotlin のみで書かれており、Android SDK に依存しない（JVM 単体テスト・UI 双方から利用できる）。
 */
class GameEngine(private val random: Random = Random.Default) {

    lateinit var state: GameState
        private set

    fun setupNewGame(
        cards: List<Card> = CardDatabase.allCards,
        firstPlayer: PlayerId = PlayerId.PLAYER_ONE,
    ): GameState {
        require(cards.size == GameConfig.TOTAL_CARDS) { "デッキは${GameConfig.TOTAL_CARDS}枚である必要があります" }
        val shuffled = cards.shuffled(random).toMutableList()

        val playerOne = PlayerState(PlayerId.PLAYER_ONE)
        val playerTwo = PlayerState(PlayerId.PLAYER_TWO)

        repeat(GameConfig.LIFE_COUNT) { playerOne.life.add(shuffled.removeAt(0)) }
        repeat(GameConfig.LIFE_COUNT) { playerTwo.life.add(shuffled.removeAt(0)) }

        val secondPlayer = firstPlayer.opponent()
        val firstCount = GameConfig.STARTING_HAND_FIRST
        val secondCount = GameConfig.STARTING_HAND_SECOND
        repeat(firstCount) { player(firstPlayer, playerOne, playerTwo).hand.add(shuffled.removeAt(0)) }
        repeat(secondCount) { player(secondPlayer, playerOne, playerTwo).hand.add(shuffled.removeAt(0)) }

        state = GameState(playerOne, playerTwo, shuffled, firstPlayer)
        state.log.add("ゲーム開始。先攻: $firstPlayer")
        return state
    }

    private fun player(id: PlayerId, p1: PlayerState, p2: PlayerState): PlayerState =
        if (id == PlayerId.PLAYER_ONE) p1 else p2

    fun playerState(id: PlayerId): PlayerState = state.player(id)
    fun currentPlayerState(): PlayerState = state.player(state.currentPlayer)
    fun opponentState(): PlayerState = state.player(state.currentPlayer.opponent())

    /** 準備フェーズ: 手札からベンチへ3枚配置する（開始時のみ）。 */
    fun placeInitialBench(playerId: PlayerId, cardIds: List<String>) {
        val p = state.player(playerId)
        require(p.bench.isEmpty()) { "ベンチは既に配置済みです" }
        require(cardIds.size == GameConfig.BENCH_SIZE) { "ベンチは${GameConfig.BENCH_SIZE}枚選んでください" }
        val cards = cardIds.map { id -> p.hand.firstOrNull { it.id == id } ?: throw IllegalMoveException("手札に $id がありません") }
        p.hand.removeAll(cards)
        p.bench.addAll(cards)
    }

    /** 1. ベンチ回復チェック: 直前の相手ターンで破壊された枚数だけ追加ドロー。戻り値は追加ドロー枚数。 */
    fun beginTurnBenchRecovery(): Int {
        check(state.phase == TurnPhase.BENCH_RECOVERY) { "ベンチ回復フェーズではありません" }
        val p = currentPlayerState()
        val destroyed = p.cardsDestroyedByOpponent
        p.cardsDestroyedByOpponent = 0
        repeat(destroyed) { drawOneCard(p) }
        if (destroyed > 0) state.log.add("${state.currentPlayer}: ベンチ回復で${destroyed}枚追加ドロー")
        state.phase = TurnPhase.DRAW
        return destroyed
    }

    /** 2. ドローフェイズ: 手札が5枚になるまでドロー（上限なし＝5枚以上なら何もしない）。 */
    fun drawPhase() {
        check(state.phase == TurnPhase.DRAW) { "ドローフェイズではありません" }
        val p = currentPlayerState()
        while (p.hand.size < GameConfig.HAND_REFILL_TARGET) {
            if (!drawOneCard(p)) break
        }
        state.phase = TurnPhase.MAIN
    }

    /** 3. メインフェイズ: ベンチ→場 */
    fun moveBenchToField(cardId: String) {
        check(state.phase == TurnPhase.MAIN) { "メインフェイズではありません" }
        val p = currentPlayerState()
        require(p.field.size < GameConfig.MAX_ATTACKS_PER_TURN) { "場はこれ以上出せません（最大${GameConfig.MAX_ATTACKS_PER_TURN}枚）" }
        val card = p.bench.firstOrNull { it.id == cardId } ?: throw IllegalMoveException("ベンチに $cardId がありません")
        p.bench.remove(card)
        p.field.add(card)
    }

    /** 3. メインフェイズ: 手札→ベンチ */
    fun moveHandToBench(cardId: String) {
        check(state.phase == TurnPhase.MAIN) { "メインフェイズではありません" }
        val p = currentPlayerState()
        require(p.bench.size < GameConfig.BENCH_SIZE) { "ベンチはこれ以上補充できません（最大${GameConfig.BENCH_SIZE}枚）" }
        val card = p.hand.firstOrNull { it.id == cardId } ?: throw IllegalMoveException("手札に $cardId がありません")
        p.hand.remove(card)
        p.bench.add(card)
    }

    fun endMainPhase() {
        check(state.phase == TurnPhase.MAIN) { "メインフェイズではありません" }
        state.phase = TurnPhase.BATTLE
    }

    /** 対象として選べる相手のベンチ/場のカードID一覧。 */
    fun availableTargets(): List<Card> {
        val opp = opponentState()
        return opp.bench + opp.field
    }

    /**
     * 4. バトル（論破）フェイズ: 1回の攻撃を宣言・即時解決する。
     * defenderComboHandCardIds は防御側が上乗せするコンボカード（呼び出し側が事前に確認して渡す）。
     */
    fun declareAttack(
        attackerFieldCardId: String,
        defenderTargetCardId: String,
        attackerComboHandCardIds: List<String> = emptyList(),
        defenderComboHandCardIds: List<String> = emptyList(),
    ): AttackResult {
        check(state.phase == TurnPhase.BATTLE) { "バトルフェイズではありません" }
        require(state.attacksUsedThisTurn < GameConfig.MAX_ATTACKS_PER_TURN) { "このターンの攻撃回数上限です" }
        require(attackerFieldCardId !in state.attackedFieldCardIdsThisTurn) { "このカードは今ターン既に攻撃済みです" }
        require(attackerComboHandCardIds.size <= GameConfig.COMBO_MAX_CARDS_FROM_HAND)
        require(defenderComboHandCardIds.size <= GameConfig.COMBO_MAX_CARDS_FROM_HAND)

        val attacker = currentPlayerState()
        val defender = opponentState()

        val attackerCard = attacker.field.firstOrNull { it.id == attackerFieldCardId }
            ?: throw IllegalMoveException("場に $attackerFieldCardId がありません")
        val (defenderCard, defenderZone) = defender.cardInPlay(defenderTargetCardId)
            ?: throw IllegalMoveException("相手のベンチ/場に $defenderTargetCardId がありません")

        val attackerComboCards = attackerComboHandCardIds.map { id ->
            attacker.hand.firstOrNull { it.id == id } ?: throw IllegalMoveException("手札に $id がありません")
        }
        val defenderComboCards = defenderComboHandCardIds.map { id ->
            defender.hand.firstOrNull { it.id == id } ?: throw IllegalMoveException("手札に $id がありません")
        }

        val attrAdvantage = hasAttributeAdvantage(attackerCard.attribute, defenderCard.attribute)
        val attrBonus = if (attrAdvantage) GameConfig.ATTRIBUTE_ADVANTAGE_BONUS else 0
        val attackTotal = attackerCard.attack + attrBonus + attackerComboCards.sumOf { it.combo }
        val defenseTotal = defenderCard.defense + defenderComboCards.sumOf { it.combo }
        val attackSucceeded = attackTotal > defenseTotal

        // コンボで捨てたカードは両者とも手札から取り除き捨て札へ
        attacker.hand.removeAll(attackerComboCards)
        defender.hand.removeAll(defenderComboCards)
        attacker.discard.addAll(attackerComboCards)
        defender.discard.addAll(defenderComboCards)

        // 攻撃側の場カードは結果に関わらず捨て札へ
        attacker.field.remove(attackerCard)
        attacker.discard.add(attackerCard)

        var lifeCardMoved: Card? = null
        if (attackSucceeded) {
            when (defenderZone) {
                Zone.BENCH -> defender.bench.remove(defenderCard)
                Zone.FIELD -> defender.field.remove(defenderCard)
            }
            defender.discard.add(defenderCard)
            if (defender.life.isNotEmpty()) {
                lifeCardMoved = defender.life.removeAt(defender.life.size - 1)
                defender.hand.add(lifeCardMoved)
            }
            defender.cardsDestroyedByOpponent += 1
        }
        // ブロック成功時: defenderCard はそのまま場/ベンチに残る（何もしない）

        state.attackedFieldCardIdsThisTurn.add(attackerFieldCardId)
        state.attacksUsedThisTurn += 1

        val result = AttackResult(
            attackerId = state.currentPlayer,
            attackerCard = attackerCard,
            defenderCard = defenderCard,
            defenderZone = defenderZone,
            attributeAdvantage = attrAdvantage,
            attackerComboCards = attackerComboCards,
            defenderComboCards = defenderComboCards,
            attackTotal = attackTotal,
            defenseTotal = defenseTotal,
            attackSucceeded = attackSucceeded,
            lifeCardMoved = lifeCardMoved,
        )
        state.log.add(
            "${state.currentPlayer}: ${attackerCard.id}(${attackTotal}) vs ${defenderCard.id}(${defenseTotal}) " +
                if (attackSucceeded) "-> 論破成功" else "-> ブロック成功",
        )

        checkWinner()
        return result
    }

    fun canEndTurn(): Boolean =
        state.phase == TurnPhase.BATTLE || state.phase == TurnPhase.MAIN

    /** 5. 手番終了: ターン交代。使用済み場カードの捨て札処理は declareAttack 内で完結済み。 */
    fun endTurn() {
        check(canEndTurn()) { "このフェイズでは手番を終了できません" }
        if (state.phase == TurnPhase.GAME_OVER) return
        state.attacksUsedThisTurn = 0
        state.attackedFieldCardIdsThisTurn.clear()
        state.currentPlayer = state.currentPlayer.opponent()
        state.turnNumber += 1
        state.phase = TurnPhase.BENCH_RECOVERY
    }

    private fun drawOneCard(p: PlayerState): Boolean {
        if (state.deck.isEmpty()) {
            val combined = (state.playerOne.discard + state.playerTwo.discard).shuffled(random)
            if (combined.isEmpty()) return false
            state.playerOne.discard.clear()
            state.playerTwo.discard.clear()
            state.deck = combined.toMutableList()
            state.log.add("山札切れ: 捨て札をシャッフルして新しい山札にしました（${combined.size}枚）")
        }
        p.hand.add(state.deck.removeAt(0))
        return true
    }

    private fun checkWinner() {
        for (id in listOf(PlayerId.PLAYER_ONE, PlayerId.PLAYER_TWO)) {
            if (state.player(id).life.isEmpty()) {
                state.winner = id.opponent()
                state.phase = TurnPhase.GAME_OVER
                state.log.add("${state.winner} の勝利！")
            }
        }
    }
}
