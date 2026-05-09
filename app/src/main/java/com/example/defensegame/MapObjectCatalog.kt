package com.example.defensegame

import android.graphics.Color

object MapObjectCatalog {
    private var registered = false

    fun registerAll() {
        // 'B' (Block): 일반 바닥 - 갈색
        MapObjectRegistry.register('W') { TileInfo(false, false, Color.DKGRAY) }

        // 'B' (Block): 유닛 배치 가능 바닥 - 갈색계열
        MapObjectRegistry.register('B') { TileInfo(true, true, Color.rgb(139, 69, 19)) }

        // 'R' (Road): 적이 이동하는 길 - 밝은 회색
        MapObjectRegistry.register('R') { TileInfo(false, true, Color.LTGRAY) }

        // 'S' (Start): 시작점 - 빨간색
        MapObjectRegistry.register('S') { TileInfo(false, true, Color.RED) }

        // 'E' (End): 종료점 - 파란색
        MapObjectRegistry.register('E') { TileInfo(false, true, Color.BLUE) }
    }
}