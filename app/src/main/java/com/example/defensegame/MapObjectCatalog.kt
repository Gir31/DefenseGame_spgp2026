package com.example.defensegame

object MapObjectCatalog {
    // MapLoader 초기화보다 먼저 불려야 한다.
    // 중복 등록을 막기 위해 guard 를 둔다.
    private var registered = false

    fun registerAll() {
        if (registered) return
        registered = true
        // isElevatedPlaceable / isGroundPlaceable / isWalkable
        MapObjectRegistry.register('W', TileInfo(false, false, false, R.mipmap.tile_wall))
        MapObjectRegistry.register('B', TileInfo(true,  false, false, R.mipmap.tile_ground))  // 고지대
        MapObjectRegistry.register('R', TileInfo(false, true,  true,  R.mipmap.tile_road))    // 길 위 배치 가능
        MapObjectRegistry.register('S', TileInfo(false, false, true,  R.mipmap.tile_start))   // 스폰 — 배치 불가
        MapObjectRegistry.register('E', TileInfo(false, false, true,  R.mipmap.tile_end))     // 도착 — 배치 불가
    }
}