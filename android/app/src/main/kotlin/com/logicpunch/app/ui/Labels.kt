package com.logicpunch.app.ui

import com.logicpunch.core.PlayerId

fun playerLabel(id: PlayerId): String = if (id == PlayerId.PLAYER_ONE) "プレイヤー1" else "プレイヤー2"
