package com.logicpunch.core

/**
 * 仕様書 Ver.0.7 で「未確定・調整予定」とされている数値パラメータ。
 * バランス調整はここだけを変更すれば良いようにまとめてある。
 */
object GameConfig {
    const val TOTAL_CARDS = 40
    const val ATTRIBUTE_ADVANTAGE_BONUS = 1000
    const val COMBO_MAX_CARDS_FROM_HAND = 3
    const val LIFE_COUNT = 4
    const val STARTING_HAND_FIRST = 7
    const val STARTING_HAND_SECOND = 8
    const val BENCH_SIZE = 3
    const val HAND_REFILL_TARGET = 5
    const val MAX_ATTACKS_PER_TURN = 3
}
