package com.example.defensegame

import a2dg.scene.Scene
import a2dg.view.GameContext
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import androidx.core.graphics.toColorInt

class ResultScene(
    gctx: GameContext,
    private val victory: Boolean,
    private val elapsedTime: Float,
    private val killCount: Int,
    private val remainHp: Int,
) : Scene(gctx) {

    private val retryBtn  = RectF(320f, 650f, 720f, 740f)
    private val titleBtn  = RectF(880f, 650f, 1280f, 740f)

    // ───── 드로우 ────────────────────────────────────────────────────────────
    override fun draw(canvas: Canvas) {
        canvas.drawColor(BG_COLOR)

        // 결과 타이틀
        if (victory) {
            canvas.drawText("임무 완료", 800f, 200f, victoryPaint)
            canvas.drawText("기지를 성공적으로 지켜냈습니다!", 800f, 268f, descPaint)
        } else {
            canvas.drawText("임무 실패", 800f, 200f, defeatPaint)
            canvas.drawText("기지가 함락되었습니다...", 800f, 268f, descPaint)
        }

        // 구분선
        canvas.drawLine(300f, 310f, 1300f, 310f, dividerPaint)

        // 스탯
        drawStat(canvas, "경과 시간", formatTime(elapsedTime), 400f)
        drawStat(canvas, "적 처치 수", "$killCount 마리", 490f)
        drawStat(canvas, "남은 기지 HP", "♥".repeat(remainHp) + "♡".repeat((Balance.Game.baseHp - remainHp).coerceAtLeast(0)), 580f)

        // 버튼
        drawButton(canvas, retryBtn,  "다시 시작")
        drawButton(canvas, titleBtn, "타이틀로")
    }

    private fun drawStat(canvas: Canvas, label: String, value: String, y: Float) {
        canvas.drawText(label, 550f, y, labelPaint)
        canvas.drawText(value, 1050f, y, valuePaint)
        canvas.drawLine(400f, y + 12f, 1200f, y + 12f, statLinePaint)
    }

    private fun drawButton(canvas: Canvas, rect: RectF, text: String) {
        canvas.drawRoundRect(rect, 20f, 20f, btnPaint)
        canvas.drawRoundRect(rect, 20f, 20f, btnBorderPaint)
        canvas.drawText(text, rect.centerX(), rect.centerY() + 14f, btnTextPaint)
    }

    private fun formatTime(sec: Float): String {
        val m = (sec / 60).toInt(); val s = (sec % 60).toInt()
        return "%02d:%02d".format(m, s)
    }

    // ───── 터치 ──────────────────────────────────────────────────────────────
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val pt = gctx.metrics.fromScreen(event.x, event.y)
            when {
                retryBtn.contains(pt.x, pt.y) -> gctx.sceneStack.change(MainScene(gctx, 1))
                titleBtn.contains(pt.x, pt.y) -> gctx.sceneStack.change(TitleScene(gctx))
            }
        }
        return true
    }

    companion object {
        private val BG_COLOR = "#FF0A0A18".toColorInt()

        private val victoryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FFEEBB44".toColorInt(); textSize = 90f; textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        private val defeatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FFCC3344".toColorInt(); textSize = 90f; textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        private val descPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FF8899BB".toColorInt(); textSize = 28f; textAlign = Paint.Align.CENTER
        }
        private val dividerPaint = Paint().apply {
            color = "#FF223355".toColorInt(); strokeWidth = 2f
        }
        private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FF7788AA".toColorInt(); textSize = 30f; textAlign = Paint.Align.RIGHT
        }
        private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 30f; textAlign = Paint.Align.LEFT
        }
        private val statLinePaint = Paint().apply {
            color = "#FF1A2240".toColorInt(); strokeWidth = 1f
        }
        private val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FF1A2A6C".toColorInt()
        }
        private val btnBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 2f; color = "#FF44BBFF".toColorInt()
        }
        private val btnTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 36f; textAlign = Paint.Align.CENTER
        }
    }
}
