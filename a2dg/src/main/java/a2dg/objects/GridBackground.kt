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

    // 그리기용 Paint 객체 (멤버 변수로 두어 성능 최적화)
    private val linePaint = Paint().apply {
        color = Color.rgb(50, 50, 50) // 약간 진한 회색 (완전 검은색보다 눈이 덜 아픕니다)
        style = Paint.Style.STROKE
        strokeWidth = 5f              // 두께를 1f에서 5f로 대폭 키움
        alpha = 255
    }

    override fun update(gctx: GameContext) {
        // 그리드 자체는 움직이지 않으므로 비워둡니다.
        // 만약 배경이 움직이는 그리드라면 여기서 좌표를 수정할 수 있습니다.
    }

    override fun draw(canvas: Canvas) {
        // 1. 세로선 그리기
        for (c in 0..cols) {
            val x = c * cellWidth
            canvas.drawLine(x, 0f, x, screenHeight, linePaint)
        }

        // 2. 가로선 그리기
        for (r in 0..rows) {
            val y = r * cellHeight
            canvas.drawLine(0f, y, screenWidth, y, linePaint)
        }
    }

    /**
     * 화면상의 터치 좌표(x, y)를 받아서
     * 그리드의 몇 번째 칸(Column, Row)인지 반환하는 헬퍼 함수
     */
    fun getCellIndex(touchX: Float, touchY: Float): Pair<Int, Int>? {
        val col = (touchX / cellWidth).toInt()
        val row = (touchY / cellHeight).toInt()

        // 화면 범위를 벗어난 경우 처리
        if (col in 0 until cols && row in 0 until rows) {
            return Pair(col, row)
        }
        return null
    }
}