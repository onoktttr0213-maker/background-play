package com.logicpunch.core

/** 3すくみ属性。赤 > 青 > 緑 > 赤 の順で有利。 */
enum class Attribute {
    RED, BLUE, GREEN
}

enum class Role {
    ATTACK, BALANCE, DEFENSE, COMBO
}

data class Card(
    val id: String,
    val theme: String,
    val role: Role,
    val attribute: Attribute,
    val attack: Int,
    val defense: Int,
    val combo: Int,
    val attackText: String,
    val excuseText: String,
)

/** attacker が defender に対して属性有利を取れるか（攻撃側のみ判定対象）。 */
fun hasAttributeAdvantage(attacker: Attribute, defender: Attribute): Boolean {
    val beats = mapOf(
        Attribute.RED to Attribute.BLUE,
        Attribute.BLUE to Attribute.GREEN,
        Attribute.GREEN to Attribute.RED,
    )
    return beats[attacker] == defender
}
