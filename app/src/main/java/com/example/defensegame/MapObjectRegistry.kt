package com.example.defensegame

// 타일 한 종류에 대한 메타 정보를 담는 데이터 클래스이다.
// isElevatedPlaceable : 일반 지형(B) — Archer·Mage·Cleric 배치 가능
// isGroundPlaceable   : 적 경로(R) — Warrior·Rogue 배치 가능 (적을 막아 섬)
// isWalkable          : 적이 이동할 수 있는 타일인지 여부
// resId               : 타일을 그릴 때 사용할 res/mipmap 리소스 ID
data class TileInfo(
    val isElevatedPlaceable: Boolean,  // 고지대 배치 (Archer/Mage/Cleric)
    val isGroundPlaceable:   Boolean,  // 길 위 배치  (Warrior/Rogue)
    val isWalkable:          Boolean,
    val resId:               Int,
)

object MapObjectRegistry {
    private val infos = mutableMapOf<Char, TileInfo>()

    fun register(ch: Char, info: TileInfo) {
        infos[ch] = info
    }

    fun getInfo(tile: Char): TileInfo? = infos[tile]
}
