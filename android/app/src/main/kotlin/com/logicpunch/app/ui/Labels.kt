package com.logicpunch.app.ui

import com.logicpunch.core.PlayerId

fun playerLabel(id: PlayerId): String = if (id == PlayerId.PLAYER_ONE) "プレイヤー1" else "プレイヤー2"

/** CPU対戦中はPLAYER_TWOを「CPU」と表示する。 */
fun seatLabel(id: PlayerId, vsCpu: Boolean): String =
    if (vsCpu && id == PlayerId.PLAYER_TWO) "CPU" else playerLabel(id)
