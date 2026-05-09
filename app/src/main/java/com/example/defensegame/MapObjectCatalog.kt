package com.example.defensegame

import android.graphics.Color

object MapObjectCatalog {
    private var registered = false

    fun registerAll() {
        MapObjectRegistry.register('W') { TileInfo(false, false, Color.DKGRAY) }
        MapObjectRegistry.register('B') { TileInfo(true, true, Color.rgb(139, 69, 19)) }
        MapObjectRegistry.register('R') { TileInfo(false, true, Color.LTGRAY) }
        MapObjectRegistry.register('S') { TileInfo(false, true, Color.RED) }
        MapObjectRegistry.register('E') { TileInfo(false, true, Color.BLUE) }
    }
}