package com.example.defensegame

import a2dg.view.GameContext

fun interface MapObjectCreator {
    fun create(gctx: GameContext, tile: Char, left: Float, top: Float): MapObject?
}

// 디버깅을 위한 타일 정보
data class TileInfo(
    val isPlaceable: Boolean, // 유닛 배치 가능 여부
    val isWalkable: Boolean,  // 적 이동 가능 여부
    val color: Int            // 출력할 색상
)

object MapObjectRegistry {
    /*private val creators = mutableMapOf<Char, com.example.defensegame.MapObjectCreator>()

    fun register(ch: Char, creator: com.example.defensegame.MapObjectCreator) {
        creators[ch] = creator
    }

    fun register(chars: CharRange, creator: com.example.defensegame.MapObjectCreator) {
        for (ch in chars) {
            creators[ch] = creator
        }
    }

    fun create(gctx: GameContext, tile: Char, left: Float, top: Float): MapObject? {
        return creators[tile]?.create(gctx, tile, left, top)
    }*/
    private val creators = mutableMapOf<Char, (Char) -> TileInfo>()

    fun register(ch: Char, creator: (Char) -> TileInfo) {
        creators[ch] = creator
    }

    fun getInfo(tile: Char): TileInfo? {
        // 등록된 생성 함수를 실행해서 정보를 가져옴
        return creators[tile]?.invoke(tile)
    }
}