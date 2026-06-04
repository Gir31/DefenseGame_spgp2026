package com.example.defensegame

import a2dg.objects.IGameObject
import a2dg.view.GameContext
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.toColorInt

class EnergySystem : IGameObject {

    var energy = INITIAL.toFloat()
        private set

    fun canAfford(cost: Int) = energy.toInt() >= cost

    fun spend(cost: Int): Boolean {
        if (!canAfford(cost)) return false
        energy -= cost
        return true
    }

    /** 적 처치 보상 등으로 에너지를 획득할 때 사용 */
    fun add(amount: Float) {
        energy = (energy + amount).coerceAtMost(MAX.toFloat())
    }

    // ───── 업데이트 ──────────────────────────────────────────────────────────
    override fun update(gctx: GameContext) {
        energy = (energy + REGEN_PER_SEC * gctx.frameTime).coerceAtMost(MAX.toFloat())
    }

    // ───── HUD 그리기 (좌상단) ───────────────────────────────────────────────
    override fun draw(canvas: Canvas) {
        val cur = energy.toInt()
        val label = "⚡  $cur / $MAX"

        // 배경 패널
        canvas.drawRoundRect(PANEL_RECT, 14f, 14f, panelPaint)

        // 텍스트
        canvas.drawText(label, 20f, 40f, textPaint)

        // 에너지 바
        val fillW = BAR_W * (energy / MAX)
        canvas.drawRoundRect(RectF(16f, 50f, 16f + BAR_W, 62f), 5f, 5f, barBgPaint)
        if (fillW > 0f)
            canvas.drawRoundRect(RectF(16f, 50f, 16f + fillW, 62f), 5f, 5f, barFgPaint)
    }

    companion object {
        const val INITIAL      = 10
        const val MAX          = 99
        const val REGEN_PER_SEC = 1.5f

        private const val BAR_W = 168f
        private val PANEL_RECT = RectF(8f, 8f, 200f, 70f)

        private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#CC000000".toColorInt()
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 30f
        }
        private val barBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#55FFFFFF".toColorInt()
        }
        private val barFgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FF44BBFF".toColorInt()
        }
    }
}
