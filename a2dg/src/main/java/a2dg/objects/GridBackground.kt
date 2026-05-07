package a2dg.objects

import a2dg.view.GameContext
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class GridBackground(
    gctx: GameContext,
    private val rows: Int = 10,
    private val cols: Int = 30
) : IGameObject {
    private val screenWidth = gctx.metrics.width.toFloat()
    private val screenHeight = gctx.metrics.height.toFloat()
    private val cellWidth = screenWidth / cols
    private val cellHeight = screenHeight / rows

    private var hoveredCell: Pair<Int, Int>? = null

    private val linePaint = Paint().apply {
        color = Color.rgb(50, 50, 50)
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val highlightPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
        alpha = 100
    }

    private val rect = RectF()

    override fun update(gctx: GameContext) {

    }

    fun onMouseMove(x: Float, y: Float) {
        hoveredCell = getCellIndex(x, y)
    }

    override fun draw(canvas: Canvas) {
        hoveredCell?.let { (c, r) ->
            rect.set(
                c * cellWidth,
                r * cellHeight,
                (c + 1) * cellWidth,
                (r + 1) * cellHeight
            )
            canvas.drawRect(rect, highlightPaint)
        }

        for (c in 0..cols) {
            val x = c * cellWidth
            canvas.drawLine(x, 0f, x, screenHeight, linePaint)
        }
        for (r in 0..rows) {
            val y = r * cellHeight
            canvas.drawLine(0f, y, screenWidth, y, linePaint)
        }
    }

    fun getCellIndex(touchX: Float, touchY: Float): Pair<Int, Int>? {
        val col = (touchX / cellWidth).toInt()
        val row = (touchY / cellHeight).toInt()
        if (col in 0 until cols && row in 0 until rows) {
            return Pair(col, row)
        }
        return null
    }
}