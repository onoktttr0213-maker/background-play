package com.logicpunch.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CpuPlayerTest {

    /** セットアップを終え、CPU（PLAYER_TWO）の手番のメインフェイズまで進めた状態を返す。 */
    private fun freshGame(seed: Long = 1): GameEngine {
        val engine = GameEngine(Random(seed))
        val state = engine.setupNewGame(firstPlayer = PlayerId.PLAYER_ONE)
        engine.placeInitialBench(PlayerId.PLAYER_ONE, state.playerOne.hand.take(3).map { it.id })
        val cpuBench = CpuPlayer.chooseBenchCardIds(state.playerTwo.hand)
        engine.placeInitialBench(PlayerId.PLAYER_TWO, cpuBench)
        engine.beginTurnBenchRecovery()
        engine.drawPhase()
        // Skip player one's own turn so engine.state.currentPlayer is PLAYER_TWO (the CPU) —
        // CpuPlayer's mutating calls always act on whoever is currently the active player.
        engine.endTurn()
        engine.beginTurnBenchRecovery()
        engine.drawPhase()
        return engine
    }

    @Test
    fun chooseBenchPicksLowestValueCards() {
        val distinctHand = listOf(
            Card("LOW1", "t", Role.ATTACK, Attribute.RED, 1000, 1000, 1000, "a", "e"), // sum 3000
            Card("LOW2", "t", Role.ATTACK, Attribute.RED, 1000, 1000, 2000, "a", "e"), // sum 4000
            Card("HIGH1", "t", Role.ATTACK, Attribute.RED, 5000, 5000, 5000, "a", "e"), // sum 15000
            Card("HIGH2", "t", Role.ATTACK, Attribute.RED, 5000, 5000, 4000, "a", "e"), // sum 14000
        )
        val chosen = CpuPlayer.chooseBenchCardIds(distinctHand.take(GameConfig.BENCH_SIZE + 1))
        assertEquals(GameConfig.BENCH_SIZE, chosen.size)
        assertTrue(chosen.contains("LOW1"))
        assertTrue(chosen.contains("LOW2"))
        assertFalse(chosen.contains("HIGH1"))
    }

    @Test
    fun pickNextAttackFindsZeroComboWinWhenAvailable() {
        val engine = freshGame()
        val state = engine.state
        val strongAttacker = Card("CPU-ATK", "t", Role.ATTACK, Attribute.RED, 5000, 1000, 1000, "a", "e")
        val weakTarget = Card("HUMAN-DEF", "t", Role.DEFENSE, Attribute.GREEN, 1000, 1000, 1000, "a", "e")
        state.playerTwo.field.clear()
        state.playerTwo.field.add(strongAttacker)
        state.playerOne.bench.clear()
        state.playerOne.bench.add(weakTarget)

        val plan = CpuPlayer.pickNextAttack(engine, PlayerId.PLAYER_TWO)
        assertEquals("CPU-ATK", plan?.attackerCardId)
        assertEquals("HUMAN-DEF", plan?.targetCardId)
        assertTrue(plan!!.comboCardIds.isEmpty())
    }

    @Test
    fun pickNextAttackReturnsNullWhenNoWinPossible() {
        val engine = freshGame()
        val state = engine.state
        val weakAttacker = Card("CPU-WEAK", "t", Role.DEFENSE, Attribute.GREEN, 1000, 1000, 1000, "a", "e")
        val strongTarget = Card("HUMAN-WALL", "t", Role.DEFENSE, Attribute.RED, 1000, 9000, 1000, "a", "e")
        state.playerTwo.field.clear()
        state.playerTwo.field.add(weakAttacker)
        state.playerTwo.hand.clear() // no combo fuel at all
        state.playerOne.bench.clear()
        state.playerOne.bench.add(strongTarget)

        assertNull(CpuPlayer.pickNextAttack(engine, PlayerId.PLAYER_TWO))
    }

    @Test
    fun pickNextAttackUsesMinimalComboNeeded() {
        val engine = freshGame()
        val state = engine.state
        val attacker = Card("CPU-MID", "t", Role.COMBO, Attribute.GREEN, 3000, 3000, 3000, "a", "e")
        val target = Card("HUMAN-DEF2", "t", Role.DEFENSE, Attribute.RED, 1000, 4000, 1000, "a", "e")
        state.playerTwo.field.clear()
        state.playerTwo.field.add(attacker)
        state.playerTwo.hand.clear()
        state.playerTwo.hand.add(Card("COMBO1", "t", Role.COMBO, Attribute.GREEN, 3000, 3000, 3000, "a", "e"))
        state.playerOne.bench.clear()
        state.playerOne.bench.add(target)

        // 3000 base < 4000 defense, needs exactly 1 combo card (3000) to reach 6000 > 4000.
        val plan = CpuPlayer.pickNextAttack(engine, PlayerId.PLAYER_TWO)
        assertEquals(listOf("COMBO1"), plan?.comboCardIds)
    }

    @Test
    fun worstCaseFallbackPicksSmallestLossMargin() {
        val engine = freshGame()
        val state = engine.state
        val attacker = Card("CPU-SAC", "t", Role.DEFENSE, Attribute.GREEN, 1000, 1000, 1000, "a", "e")
        val closeTarget = Card("HUMAN-CLOSE", "t", Role.DEFENSE, Attribute.RED, 1000, 1500, 1000, "a", "e")
        val farTarget = Card("HUMAN-FAR", "t", Role.DEFENSE, Attribute.RED, 1000, 9000, 1000, "a", "e")
        state.playerTwo.field.clear()
        state.playerTwo.field.add(attacker)
        state.playerOne.bench.clear()
        state.playerOne.bench.add(closeTarget)
        state.playerOne.bench.add(farTarget)

        val plan = CpuPlayer.worstCaseFallback(engine, PlayerId.PLAYER_TWO)
        assertEquals("HUMAN-CLOSE", plan?.targetCardId)
        assertTrue(plan!!.comboCardIds.isEmpty())
    }

    @Test
    fun chooseDefenseComboSkipsWhenAlreadySafe() {
        val attacker = Card("ATK", "t", Role.ATTACK, Attribute.RED, 5000, 1000, 1000, "a", "e")
        val target = Card("DEF", "t", Role.DEFENSE, Attribute.GREEN, 1000, 9000, 1000, "a", "e")
        val combo = CpuPlayer.chooseDefenseCombo(attacker, target, emptyList(), listOf())
        assertTrue(combo.isEmpty())
    }

    @Test
    fun chooseDefenseComboSkipsWhenHopelessEvenWithMaxCombo() {
        val attacker = Card("ATK", "t", Role.ATTACK, Attribute.RED, 5000, 1000, 1000, "a", "e")
        val target = Card("DEF", "t", Role.DEFENSE, Attribute.GREEN, 1000, 1000, 1000, "a", "e")
        val weakHand = listOf(
            Card("H1", "t", Role.DEFENSE, Attribute.RED, 1000, 1000, 500, "a", "e"),
        )
        // attackTotal = 5000, defense with all combo = 1000 + 500 = 1500, still hopeless.
        val combo = CpuPlayer.chooseDefenseCombo(attacker, target, emptyList(), weakHand)
        assertTrue(combo.isEmpty())
    }

    @Test
    fun chooseDefenseComboUsesJustEnoughToBlock() {
        val attacker = Card("ATK", "t", Role.ATTACK, Attribute.RED, 5000, 1000, 1000, "a", "e")
        val target = Card("DEF", "t", Role.DEFENSE, Attribute.GREEN, 1000, 3000, 1000, "a", "e")
        val hand = listOf(
            Card("BIG", "t", Role.COMBO, Attribute.RED, 3000, 3000, 3000, "a", "e"),
            Card("SMALL", "t", Role.COMBO, Attribute.RED, 3000, 3000, 500, "a", "e"),
        )
        // attackTotal = 5000, base defense 3000 -> needs +2000. BIG(3000) alone reaches 6000 >= 5000.
        val combo = CpuPlayer.chooseDefenseCombo(attacker, target, emptyList(), hand)
        assertEquals(listOf("BIG"), combo)
    }

    @Test
    fun fullCpuTurnPlaysOutWithoutThrowing() {
        val engine = freshGame()
        CpuPlayer.runMainPhase(engine, PlayerId.PLAYER_TWO)
        assertEquals(TurnPhase.BATTLE, engine.state.phase)
        var attacks = 0
        while (attacks < GameConfig.MAX_ATTACKS_PER_TURN) {
            val plan = CpuPlayer.pickNextAttack(engine, PlayerId.PLAYER_TWO) ?: break
            engine.declareAttack(plan.attackerCardId, plan.targetCardId, plan.comboCardIds, emptyList())
            attacks++
        }
        // Should never throw regardless of how many attacks it found (0..3).
        assertTrue(attacks <= GameConfig.MAX_ATTACKS_PER_TURN)
    }
}
