package com.example.defensegame

import a2dg.scene.Scene
import a2dg.view.GameContext
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import androidx.core.graphics.toColorInt

class TitleScene(gctx: GameContext) : Scene(gctx) {

    // 버튼 영역 (게임 좌표계 기준)
    private val startBtn = RectF(550f, 540f, 1050f, 630f)

    // ───── 드로우 ────────────────────────────────────────────────────────────
    override fun draw(canvas: Canvas) {
        // 배경
        canvas.drawColor(BG_COLOR)

        // 타이틀
        canvas.drawText("에테르 가디언즈", 800f, 240f, titlePaint)
        canvas.drawText("AETHER  GUARDIANS", 800f, 300f, subtitlePaint)

        // 부제
        canvas.drawText("마법과 전략으로 기지를 지켜라", 800f, 390f, descPaint)

        // 장식 선
        canvas.drawLine(300f, 430f, 1300f, 430f, dividerPaint)

        // 게임 시작 버튼
        canvas.drawRoundRect(startBtn, 24f, 24f, btnPaint)
        canvas.drawRoundRect(startBtn, 24f, 24f, btnBorderPaint)
        canvas.drawText("게임 시작", 800f, 598f, btnTextPaint)

        // 하단 안내
        canvas.drawText("유닛을 드래그해 배치하고 적의 진격을 막으세요", 800f, 820f, hintPaint)
    }

    // ───── 터치 ──────────────────────────────────────────────────────────────
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val pt = gctx.metrics.fromScreen(event.x, event.y)
            if (startBtn.contains(pt.x, pt.y)) {
                gctx.sceneStack.change(MainScene(gctx, 1))
            }
        }
        return true
    }

    companion object {
        private val BG_COLOR = "#FF0D0D1E".toColorInt()

        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FFEECCAA".toColorInt(); textSize = 80f; textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FF7788AA".toColorInt(); textSize = 32f; textAlign = Paint.Align.CENTER
            letterSpacing = 0.3f
        }
        private val descPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FF8899BB".toColorInt(); textSize = 26f; textAlign = Paint.Align.CENTER
        }
        private val dividerPaint = Paint().apply {
            color = "#FF2233AA".toColorInt(); strokeWidth = 2f
        }
        private val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FF1A2A6C".toColorInt()
        }
        private val btnBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 2f; color = "#FF44BBFF".toColorInt()
        }
        private val btnTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 44f; textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FF445566".toColorInt(); textSize = 22f; textAlign = Paint.Align.CENTER
        }
    }
}
