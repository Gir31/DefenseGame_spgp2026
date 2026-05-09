package com.example.defensegame

import a2dg.objects.IGameObject
import a2dg.view.GameContext
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

class TileObject(
    val col: Int,
    val row: Int,
    val width: Float,
    val height: Float,
    val color: Int
) : IGameObject {
    private val rect = RectF(
        col * width,
        row * height,
        (col + 1) * width,
        (row + 1) * height
    )
    private val paint = Paint().apply {
        this.color = this@TileObject.color
        style = Paint.Style.FILL
    }

    override fun update(gctx: GameContext) {
        // 지형은 보통 움직이지 않으므로 비워둡니다.
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(rect, paint)
    }
}