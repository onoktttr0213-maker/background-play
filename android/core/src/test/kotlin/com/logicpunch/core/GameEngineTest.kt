package com.logicpunch.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameEngineTest {

    @Test
    fun cardDatabaseHas40UniqueCards() {
        assertEquals(40, CardDatabase.allCards.size)
        assertEquals(40, CardDatabase.allCards.map { it.id }.toSet().size)
    }

    @Test
    fun attributeAdvantageCycle() {
        assertTrue(hasAttributeAdvantage(Attribute.RED, Attribute.BLUE))
        assertTrue(hasAttributeAdvantage(Attribute.BLUE, Attribute.GREEN))
        assertTrue(hasAttributeAdvantage(Attribute.GREEN, Attribute.RED))
        assertFalse(hasAttributeAdvantage(Attribute.BLUE, Attribute.RED))
        assertFalse(hasAttributeAdvantage(Attribute.RED, Attribute.RED))
    }

    @Test
    fun setupDealsLifeAndHandsCorrectly() {
        val engine = GameEngine(Random(1))
        val state = engine.setupNewGame(firstPlayer = PlayerId.PLAYER_ONE)

        assertEquals(GameConfig.LIFE_COUNT, state.playerOne.life.size)
        assertEquals(GameConfig.LIFE_COUNT, state.playerTwo.life.size)
        assertEquals(GameConfig.STARTING_HAND_FIRST, state.playerOne.hand.size)
        assertEquals(GameConfig.STARTING_HAND_SECOND, state.playerTwo.hand.size)

        val dealt = state.playerOne.life.size + state.playerTwo.life.size +
            state.playerOne.hand.size + state.playerTwo.hand.size + state.deck.size
        assertEquals(GameConfig.TOTAL_CARDS, dealt)

        // No duplicate cards anywhere.
        val allIds = (state.playerOne.life + state.playerTwo.life + state.playerOne.hand +
            state.playerTwo.hand + state.deck).map { it.id }
        assertEquals(allIds.size, allIds.toSet().size)
    }

    @Test
    fun initialBenchPlacementMovesCardsOutOfHand() {
        val engine = GameEngine(Random(2))
        val state = engine.setupNewGame()
        val threeIds = state.playerOne.hand.take(3).map { it.id }
        engine.placeInitialBench(PlayerId.PLAYER_ONE, threeIds)

        assertEquals(GameConfig.BENCH_SIZE, state.playerOne.bench.size)
        assertEquals(GameConfig.STARTING_HAND_FIRST - 3, state.playerOne.hand.size)
        assertEquals(threeIds.toSet(), state.playerOne.bench.map { it.id }.toSet())
    }

    @Test
    fun placingBenchTwiceFails() {
        val engine = GameEngine(Random(3))
        val state = engine.setupNewGame()
        val ids = state.playerOne.hand.take(3).map { it.id }
        engine.placeInitialBench(PlayerId.PLAYER_ONE, ids)
        val moreIds = state.playerOne.hand.take(3).map { it.id }
        assertFailsWith<IllegalArgumentException> {
            engine.placeInitialBench(PlayerId.PLAYER_ONE, moreIds)
        }
    }

    private fun freshGameReadyForBattle(seed: Long = 42): Pair<GameEngine, GameState> {
        val engine = GameEngine(Random(seed))
        val state = engine.setupNewGame(firstPlayer = PlayerId.PLAYER_ONE)
        engine.placeInitialBench(PlayerId.PLAYER_ONE, state.playerOne.hand.take(3).map { it.id })
        engine.placeInitialBench(PlayerId.PLAYER_TWO, state.playerTwo.hand.take(3).map { it.id })
        engine.beginTurnBenchRecovery()
        engine.drawPhase()
        return engine to state
    }

    @Test
    fun drawPhaseFillsHandTo5ButNeverRemoves() {
        val (_, state) = freshGameReadyForBattle()
        assertTrue(state.playerOne.hand.size >= GameConfig.HAND_REFILL_TARGET)
    }

    @Test
    fun attackSucceedsWhenAttackExceedsDefenseAndMovesLifeToHand() {
        val (engine, state) = freshGameReadyForBattle()

        // Force a deterministic matchup: attacker 5000atk RED vs defender 1000def BLUE (RED beats BLUE -> advantage applies).
        val attacker = Card("TEST-ATK", "test", Role.ATTACK, Attribute.RED, 5000, 1000, 1000, "a", "e")
        val defender = Card("TEST-DEF", "test", Role.DEFENSE, Attribute.BLUE, 1000, 1000, 1000, "a", "e")
        state.playerOne.field.clear()
        state.playerOne.field.add(attacker)
        state.playerTwo.bench.clear()
        state.playerTwo.bench.add(defender)
        val lifeBefore = state.playerTwo.life.size
        val handBefore = state.playerTwo.hand.size

        engine.endMainPhaseForTest()
        val result = engine.declareAttack("TEST-ATK", "TEST-DEF")

        assertTrue(result.attackSucceeded)
        assertTrue(result.attributeAdvantage)
        assertEquals(5000 + 1000, result.attackTotal)
        assertEquals(1000, result.defenseTotal)
        assertNotNull(result.lifeCardMoved)
        assertEquals(lifeBefore - 1, state.playerTwo.life.size)
        assertEquals(handBefore + 1, state.playerTwo.hand.size)
        assertTrue(state.playerTwo.bench.none { it.id == "TEST-DEF" })
        assertTrue(state.playerTwo.discard.any { it.id == "TEST-DEF" })
        assertTrue(state.playerOne.discard.any { it.id == "TEST-ATK" })
    }

    @Test
    fun blockSucceedsDefenderCardStaysInPlay() {
        val (engine, state) = freshGameReadyForBattle()

        val attacker = Card("TEST-ATK2", "test", Role.DEFENSE, Attribute.GREEN, 1000, 1000, 1000, "a", "e")
        val defender = Card("TEST-DEF2", "test", Role.ATTACK, Attribute.RED, 5000, 5000, 1000, "a", "e")
        state.playerOne.field.clear()
        state.playerOne.field.add(attacker)
        state.playerTwo.field.clear()
        state.playerTwo.field.add(defender)
        val lifeBefore = state.playerTwo.life.size

        engine.endMainPhaseForTest()
        val result = engine.declareAttack("TEST-ATK2", "TEST-DEF2")

        assertFalse(result.attackSucceeded)
        assertEquals(lifeBefore, state.playerTwo.life.size)
        assertTrue(state.playerTwo.field.any { it.id == "TEST-DEF2" })
        assertTrue(state.playerOne.discard.any { it.id == "TEST-ATK2" })
    }

    @Test
    fun comboCardsAreDiscardedAndAddToTotal() {
        val (engine, state) = freshGameReadyForBattle()

        val attacker = Card("TEST-ATK3", "test", Role.COMBO, Attribute.RED, 3000, 3000, 3000, "a", "e")
        val defender = Card("TEST-DEF3", "test", Role.COMBO, Attribute.BLUE, 3000, 3000, 3000, "a", "e")
        val comboA = Card("COMBO-A", "test", Role.COMBO, Attribute.RED, 3000, 3000, 3000, "a", "e")
        val comboD = Card("COMBO-D", "test", Role.COMBO, Attribute.RED, 3000, 3000, 3000, "a", "e")
        state.playerOne.field.clear()
        state.playerOne.field.add(attacker)
        state.playerTwo.field.clear()
        state.playerTwo.field.add(defender)
        state.playerOne.hand.add(comboA)
        state.playerTwo.hand.add(comboD)

        engine.endMainPhaseForTest()
        // RED atk vs BLUE def -> no attribute advantage (RED beats BLUE is false per beats map: RED->BLUE actually IS an advantage)
        val result = engine.declareAttack(
            "TEST-ATK3", "TEST-DEF3",
            attackerComboHandCardIds = listOf("COMBO-A"),
            defenderComboHandCardIds = listOf("COMBO-D"),
        )

        // attack: 3000 + 1000(advantage RED>BLUE) + 3000(combo) = 7000; defense: 3000 + 3000(combo) = 6000
        assertEquals(7000, result.attackTotal)
        assertEquals(6000, result.defenseTotal)
        assertTrue(result.attackSucceeded)
        assertTrue(state.playerOne.discard.any { it.id == "COMBO-A" })
        assertTrue(state.playerTwo.discard.any { it.id == "COMBO-D" })
        assertFalse(state.playerOne.hand.any { it.id == "COMBO-A" })
    }

    @Test
    fun cannotAttackWithSameFieldCardTwiceInOneTurn() {
        val (engine, state) = freshGameReadyForBattle()
        val attacker = Card("TEST-ATK4", "test", Role.ATTACK, Attribute.RED, 5000, 1000, 1000, "a", "e")
        val defender1 = Card("TEST-DEF4", "test", Role.DEFENSE, Attribute.GREEN, 1000, 9000, 1000, "a", "e")
        val defender2 = Card("TEST-DEF5", "test", Role.DEFENSE, Attribute.GREEN, 1000, 9000, 1000, "a", "e")
        state.playerOne.field.clear()
        state.playerOne.field.add(attacker)
        state.playerTwo.bench.clear()
        state.playerTwo.bench.add(defender1)
        state.playerTwo.bench.add(defender2)

        engine.endMainPhaseForTest()
        engine.declareAttack("TEST-ATK4", "TEST-DEF4") // blocked (9000 def), attacker card discarded
        assertFailsWith<IllegalArgumentException> {
            engine.declareAttack("TEST-ATK4", "TEST-DEF5")
        }
    }

    @Test
    fun losingAllLifeEndsGame() {
        val (engine, state) = freshGameReadyForBattle()
        engine.endMainPhaseForTest()

        // Drain all 4 life. Max 3 attacks/turn, so simulate the turn handing back to
        // player one (without playing out player two's turn) once the limit is hit.
        repeat(GameConfig.LIFE_COUNT) { i ->
            if (state.attacksUsedThisTurn >= GameConfig.MAX_ATTACKS_PER_TURN) {
                state.attacksUsedThisTurn = 0
                state.attackedFieldCardIdsThisTurn.clear()
                state.currentPlayer = PlayerId.PLAYER_ONE
                state.phase = TurnPhase.BATTLE
            }
            val atk = Card("ATK-$i", "test", Role.ATTACK, Attribute.RED, 9000, 1000, 1000, "a", "e")
            val def = Card("DEF-$i", "test", Role.DEFENSE, Attribute.GREEN, 1000, 100, 1000, "a", "e")
            state.playerOne.field.add(atk)
            state.playerTwo.bench.add(def)
            engine.declareAttack(atk.id, def.id)
        }

        assertEquals(0, state.playerTwo.life.size)
        assertEquals(PlayerId.PLAYER_ONE, state.winner)
        assertEquals(TurnPhase.GAME_OVER, state.phase)
    }

    @Test
    fun deckReshufflesFromDiscardsWhenEmpty() {
        val engine = GameEngine(Random(7))
        val state = engine.setupNewGame()
        // Empty the deck entirely and move all its cards into both discard piles instead,
        // so the very next draw is forced to trigger a reshuffle.
        val remaining = state.deck.toList()
        state.deck.clear()
        state.playerOne.discard.addAll(remaining.take(remaining.size / 2))
        state.playerTwo.discard.addAll(remaining.drop(remaining.size / 2))

        engine.placeInitialBench(PlayerId.PLAYER_ONE, state.playerOne.hand.take(3).map { it.id })
        engine.placeInitialBench(PlayerId.PLAYER_TWO, state.playerTwo.hand.take(3).map { it.id })
        engine.beginTurnBenchRecovery()
        engine.drawPhase()

        assertTrue(state.log.any { it.contains("山札切れ") })
        assertTrue(state.playerOne.discard.isEmpty())
        assertTrue(state.playerTwo.discard.isEmpty())
    }

    @Test
    fun benchRecoveryDrawsExtraCardsAfterOpponentDestroysCards() {
        val (engine, state) = freshGameReadyForBattle()
        val attacker = Card("TEST-ATK7", "test", Role.ATTACK, Attribute.RED, 9000, 1000, 1000, "a", "e")
        val defender = Card("TEST-DEF7", "test", Role.DEFENSE, Attribute.GREEN, 1000, 100, 1000, "a", "e")
        state.playerOne.field.clear()
        state.playerOne.field.add(attacker)
        state.playerTwo.bench.clear()
        state.playerTwo.bench.add(defender)

        engine.endMainPhaseForTest()
        engine.declareAttack("TEST-ATK7", "TEST-DEF7")
        assertEquals(1, state.playerTwo.cardsDestroyedByOpponent)

        engine.endTurn()
        assertEquals(PlayerId.PLAYER_TWO, state.currentPlayer)
        val handBefore = state.playerTwo.hand.size
        val extra = engine.beginTurnBenchRecovery()
        assertEquals(1, extra)
        assertEquals(handBefore + 1, state.playerTwo.hand.size)
        assertEquals(0, state.playerTwo.cardsDestroyedByOpponent)
    }

    @Test
    fun endTurnSwitchesPlayerAndResetsAttackCounters() {
        val (engine, state) = freshGameReadyForBattle()
        engine.endMainPhaseForTest()
        assertEquals(PlayerId.PLAYER_ONE, state.currentPlayer)
        engine.endTurn()
        assertEquals(PlayerId.PLAYER_TWO, state.currentPlayer)
        assertEquals(0, state.attacksUsedThisTurn)
        assertTrue(state.attackedFieldCardIdsThisTurn.isEmpty())
        assertEquals(TurnPhase.BENCH_RECOVERY, state.phase)
    }
}

/** テスト専用: メインフェイズを経由せず直接バトルフェイズへ進める。 */
private fun GameEngine.endMainPhaseForTest() {
    if (state.phase == TurnPhase.MAIN) {
        endMainPhase()
    } else if (state.phase == TurnPhase.BATTLE) {
        // already there
    } else {
        error("unexpected phase ${state.phase} for test setup")
    }
}
