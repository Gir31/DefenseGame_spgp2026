package com.example.defensegame

import a2dg.scene.Scene
import a2dg.view.GameContext
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import androidx.core.graphics.toColorInt

class PauseScene(gctx: GameContext) : Scene(gctx) {

    private val resumeBtn = RectF(500f, 430f, 900f, 520f)
    private val quitBtn   = RectF(700f, 560f, 1100f, 650f)

    override fun draw(canvas: Canvas) {
        // 반투명 오버레이 (아래 MainScene 이 일시정지된 채로 보임)
        canvas.drawColor(0xCC000000.toInt())

        canvas.drawText("일시정지", 800f, 300f, titlePaint)

        drawButton(canvas, resumeBtn, "재개", btnPaint)
        drawButton(canvas, quitBtn,   "포기 (타이틀로)", quitBtnPaint)
    }

    private fun drawButton(canvas: Canvas, rect: RectF, text: String, bg: Paint) {
        canvas.drawRoundRect(rect, 20f, 20f, bg)
        canvas.drawRoundRect(rect, 20f, 20f, borderPaint)
        canvas.drawText(text, rect.centerX(), rect.centerY() + 14f, btnTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val pt = gctx.metrics.fromScreen(event.x, event.y)
            when {
                resumeBtn.contains(pt.x, pt.y) -> pop()   // MainScene 으로 복귀
                quitBtn.contains(pt.x, pt.y)   -> {
                    GameSpeed.reset()
                    gctx.sceneStack.change(TitleScene(gctx))
                }
            }
        }
        return true
    }

    override fun onBackPressed(): Boolean {
        pop()
        return true
    }

    companion object {
        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 72f; textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        private val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FF1A2A6C".toColorInt()
        }
        private val quitBtnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FF4A1A1A".toColorInt()
        }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 2f; color = "#FF44BBFF".toColorInt()
        }
        private val btnTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 36f; textAlign = Paint.Align.CENTER
        }
    }
}
