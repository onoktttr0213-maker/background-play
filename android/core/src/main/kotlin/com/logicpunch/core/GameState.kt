package com.logicpunch.core

enum class PlayerId {
    PLAYER_ONE, PLAYER_TWO;

    fun opponent(): PlayerId = if (this == PLAYER_ONE) PLAYER_TWO else PLAYER_ONE
}

enum class Zone { BENCH, FIELD }

enum class TurnPhase {
    BENCH_RECOVERY, DRAW, MAIN, BATTLE, END, GAME_OVER
}

/**
 * 1人分の場・手札・ライフ・捨て札の状態。
 * life は裏向きだが、実装上は中身を保持しておき、失われた際に手札へ移す。
 */
class PlayerState(val id: PlayerId) {
    val life: MutableList<Card> = mutableListOf()
    val hand: MutableList<Card> = mutableListOf()
    val bench: MutableList<Card> = mutableListOf()
    val field: MutableList<Card> = mutableListOf()
    val discard: MutableList<Card> = mutableListOf()

    /** 相手ターン中にベンチ/場のカードが破壊された枚数。自ターン開始時のベンチ回復で消費される。 */
    var cardsDestroyedByOpponent: Int = 0

    fun cardInPlay(cardId: String): Pair<Card, Zone>? {
        bench.firstOrNull { it.id == cardId }?.let { return it to Zone.BENCH }
        field.firstOrNull { it.id == cardId }?.let { return it to Zone.FIELD }
        return null
    }
}

class GameState(
    val playerOne: PlayerState,
    val playerTwo: PlayerState,
    var deck: MutableList<Card>,
    var currentPlayer: PlayerId,
) {
    var turnNumber: Int = 1
    var phase: TurnPhase = TurnPhase.BENCH_RECOVERY
    var attacksUsedThisTurn: Int = 0
    val attackedFieldCardIdsThisTurn: MutableSet<String> = mutableSetOf()
    var winner: PlayerId? = null
    val log: MutableList<String> = mutableListOf()

    fun player(id: PlayerId): PlayerState = if (id == PlayerId.PLAYER_ONE) playerOne else playerTwo
}
