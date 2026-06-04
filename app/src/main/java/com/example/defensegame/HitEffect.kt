package com.example.defensegame

import a2dg.objects.IGameObject
import a2dg.objects.IRecyclable
import a2dg.view.GameContext
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.toColorInt
import kotlin.math.cos
import kotlin.math.sin

// 공격 시 발생하는 짧은 시각 이펙트.
// 근접 슬래시, 도적 찌르기, 투사체 충돌 등 모두 여기서 관리한다.
// 객체 풀링을 위해 IRecyclable 구현, 생성자 파라미터 없음.
class HitEffect private constructor() : IGameObject, IRecyclable {

    enum class Style {
        SLASH,       // 전사 — 황금 곡선 베기
        STAB,        // 도적 — 빠른 십자 찌르기
        ARROW_HIT,   // 궁수 화살 충돌
        MAGIC_HIT,   // 마법사 마법 충돌
        HOLY_HIT,    // 성직자 신성 충돌
    }

    private var style  = Style.SLASH
    private var cx     = 0f
    private var cy     = 0f
    private var age    = 0f
    private var life   = 0f   // 스타일별 지속 시간

    private fun init(style: Style, x: Float, y: Float): HitEffect {
        this.style = style; cx = x; cy = y; age = 0f
        life = when (style) {
            Style.SLASH      -> 0.38f
            Style.STAB       -> 0.25f
            Style.ARROW_HIT  -> 0.30f
            Style.MAGIC_HIT  -> 0.42f
            Style.HOLY_HIT   -> 0.45f
        }
        return this
    }

    // ───── 업데이트 ──────────────────────────────────────────────────────────
    override fun update(gctx: GameContext) {
        age += gctx.frameTime
        if (age >= life) gctx.mainWorld().remove(this, MainScene.Layer.PROJECTILE)
    }

    // ───── 드로우 ────────────────────────────────────────────────────────────
    override fun draw(canvas: Canvas) {
        val t = (age / life).coerceIn(0f, 1f)
        val alpha = ((1f - t) * 255).toInt()
        when (style) {
            Style.SLASH     -> drawSlash(canvas, t, alpha)
            Style.STAB      -> drawStab(canvas, t, alpha)
            Style.ARROW_HIT -> drawArrowHit(canvas, t, alpha)
            Style.MAGIC_HIT -> drawMagicHit(canvas, t, alpha)
            Style.HOLY_HIT  -> drawHolyHit(canvas, t, alpha)
        }
    }

    // ── 전사 슬래시 : 3개의 황금 호가 밖으로 퍼짐 ────────────────────────────
    private fun drawSlash(canvas: Canvas, t: Float, alpha: Int) {
        slashPaint.alpha = alpha
        val r = 20f + t * 30f
        for (i in 0 until 3) {
            val startAngle = -60f + i * 60f - t * 20f
            canvas.drawArc(
                RectF(cx - r, cy - r, cx + r, cy + r),
                startAngle, 40f, false, slashPaint
            )
        }
    }

    // ── 도적 찌르기 : 빠른 4방향 선 ─────────────────────────────────────────
    private fun drawStab(canvas: Canvas, t: Float, alpha: Int) {
        stabPaint.alpha = alpha
        val len = 10f + t * 18f
        canvas.save(); canvas.translate(cx, cy)
        for (i in 0 until 4) {
            val rad = Math.toRadians((i * 90.0))
            canvas.drawLine(0f, 0f, (cos(rad) * len).toFloat(), (sin(rad) * len).toFloat(), stabPaint)
        }
        canvas.restore()
    }

    // ── 화살 충돌 : 방사형 파편 ──────────────────────────────────────────────
    private fun drawArrowHit(canvas: Canvas, t: Float, alpha: Int) {
        arrowHitPaint.alpha = alpha
        val len = 8f + t * 16f
        canvas.save(); canvas.translate(cx, cy)
        for (i in 0 until 6) {
            val rad = Math.toRadians((i * 60.0))
            canvas.drawLine(0f, 0f, (cos(rad) * len).toFloat(), (sin(rad) * len).toFloat(), arrowHitPaint)
        }
        canvas.restore()
    }

    // ── 마법 충돌 : 보라색 링 + 파티클 ──────────────────────────────────────
    private fun drawMagicHit(canvas: Canvas, t: Float, alpha: Int) {
        magicRingPaint.alpha = (alpha * 0.9f).toInt()
        val r = 14f + t * 28f
        canvas.drawCircle(cx, cy, r, magicRingPaint)
        // 파티클 점
        magicDotPaint.alpha = alpha
        for (i in 0 until 5) {
            val rad = Math.toRadians((i * 72.0 + t * 180))
            val pr = r * 0.6f
            canvas.drawCircle(
                cx + (cos(rad) * pr).toFloat(),
                cy + (sin(rad) * pr).toFloat(),
                4f - t * 3f, magicDotPaint
            )
        }
    }

    // ── 신성 충돌 : 황금 십자 + 빛 퍼짐 ─────────────────────────────────────
    private fun drawHolyHit(canvas: Canvas, t: Float, alpha: Int) {
        holyPaint.alpha = alpha
        val arm = 8f + t * 22f
        canvas.save(); canvas.translate(cx, cy); canvas.rotate(t * 45f)
        canvas.drawLine(-arm, 0f,  arm, 0f,  holyPaint)
        canvas.drawLine( 0f, -arm, 0f,  arm, holyPaint)
        canvas.restore()
        holyGlowPaint.alpha = (alpha * 0.4f).toInt()
        canvas.drawCircle(cx, cy, arm * 0.7f, holyGlowPaint)
    }

    override fun onRecycle() {}

    // ───── companion ─────────────────────────────────────────────────────────
    companion object {
        fun spawn(gctx: GameContext, style: Style, x: Float, y: Float) {
            val world = gctx.mainWorld()
            val e = world.obtain(HitEffect::class.java) ?: HitEffect()
            world.add(e.init(style, x, y), MainScene.Layer.PROJECTILE)
        }

        private val slashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 5f; color = "#FFFFCC00".toColorInt()
            strokeCap = Paint.Cap.ROUND
        }
        private val stabPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 4f; color = "#FFFF4444".toColorInt()
            strokeCap = Paint.Cap.ROUND
        }
        private val arrowHitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 3f; color = "#FFD4A017".toColorInt()
            strokeCap = Paint.Cap.ROUND
        }
        private val magicRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 4f; color = "#FFAA44FF".toColorInt()
        }
        private val magicDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FFDD88FF".toColorInt()
        }
        private val holyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 5f; color = "#FFFFFFCC".toColorInt()
            strokeCap = Paint.Cap.ROUND
        }
        private val holyGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
    }
}
