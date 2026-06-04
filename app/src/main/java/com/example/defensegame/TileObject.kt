package com.example.defensegame

import a2dg.objects.Sprite
import a2dg.view.GameContext

// TileObject 는 맵의 타일 한 칸을 나타내는 정적 오브젝트이다.
// Sprite 를 상속받아 비트맵 렌더링을 그대로 재사용하고,
// update() 는 지형이 움직이지 않으므로 오버라이드하지 않는다.
// 중심점은 (col + 0.5) * tileWidth, (row + 0.5) * tileHeight 로 계산한다.
class TileObject(
    gctx: GameContext,
    col: Int,
    row: Int,
    tileWidth: Float,
    tileHeight: Float,
    resId: Int,
) : Sprite(gctx, resId) {
    init {
        val cx = col * tileWidth + tileWidth / 2f
        val cy = row * tileHeight + tileHeight / 2f
        setSize(tileWidth, tileHeight)
        setCenter(cx, cy)
    }
}
