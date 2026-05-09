package com.example.defensegame

import a2dg.objects.GridBackground
import a2dg.scene.Scene
import a2dg.scene.World
import a2dg.view.GameContext
import android.view.MotionEvent

class MainScene(gctx : GameContext, private val stage: Int) : Scene(gctx) {

    enum class Layer {
        CONTROLLER,
        GRID,
    }

    override val clipsRect = true

    init {
        MapObjectCatalog.registerAll()
    }

    private val grid = GridBackground(gctx, 10, 30);

    override val world = World(Layer.entries.toTypedArray()).apply {
        add(MapLoader(gctx, this, stage), Layer.CONTROLLER)
        add(grid, Layer.GRID)
    }

    companion object {
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rawX = event.x
        val rawY = event.y

        val pt = gctx.metrics.fromScreen(rawX, rawY)

        if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN) {
            grid.onMouseMove(pt.x, pt.y)
        }

        return true
    }

}