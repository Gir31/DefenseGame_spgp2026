package a2dg.objects

import a2dg.view.GameContext
import android.graphics.Canvas

interface IGameObject {
    fun update(gctx: GameContext)
    fun draw(canvas: Canvas)
}
