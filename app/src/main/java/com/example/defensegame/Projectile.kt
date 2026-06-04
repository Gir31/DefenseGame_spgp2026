package com.example.defensegame

import a2dg.objects.IGameObject
import a2dg.objects.IRecyclable
import a2dg.view.GameContext
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.toColorInt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// 원거리 유닛이 발사하는 투사체.
// 목표 Enemy 를 추적하며 날아가, 충돌 시 데미지를 주고 HitEffect 를 생성한다.
// IRecyclable 로 객체 풀에서 재사용한다.
class Projectile private constructor() : IGameObject, IRecyclable {

    // 투사체 종류별 비행 속도와 충돌 이펙트
    enum class Visual(val speed: Float, val impact: HitEffect.Style) {
        ARROW     (900f,  HitEffect.Style.ARROW_HIT),
        MAGIC_BOLT(480f,  HitEffect.Style.MAGIC_HIT),
        HOLY      (680f,  HitEffect.Style.HOLY_HIT),
    }

    private var visual     = Visual.ARROW
    private var target:    Enemy? = null
    private var power      = 0f
    private var isPhysical = true
    private var curX       = 0f
    private var curY       = 0f
    private var angle      = 0f   // 현재 비행 방향 (degrees)
    private var age        = 0f   // 회전/맥동 효과용

    private fun init(
        visual: Visual, srcX: Float, srcY: Float,
        target: Enemy, power: Float, isPhysical: Boolean,
    ): Projectile {
        this.visual = visual; this.target = target
        this.power  = power;  this.isPhysical = isPhysical
        curX = srcX; curY = srcY; angle = 0f; age = 0f
        return this
    }

    // ───── 업데이트 ──────────────────────────────────────────────────────────
    override fun update(gctx: GameContext) {
        age += gctx.frameTime

        val t = target
        if (t == null || t.isDead()) {
            gctx.mainWorld().remove(this, MainScene.Layer.PROJECTILE)
            return
        }

        val dx = t.x - curX
        val dy = t.y - curY
        val dist = sqrt(dx * dx + dy * dy)
        angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        val step = visual.speed * gctx.frameTime
        if (dist <= step + t.width / 2f) {
            onHit(gctx, t)
        } else {
            curX += dx / dist * step
            curY += dy / dist * step
        }
    }

    private fun onHit(gctx: GameContext, enemy: Enemy) {
        val damage = if (isPhysical) {
            Balance.DamageCalc.physical(power, enemy.def)
        } else {
            Balance.DamageCalc.magic(power, enemy.resPct)
        }
        enemy.takeDamage(damage)
        if (enemy.isDead()) {
            gctx.mainWorld().remove(enemy, MainScene.Layer.ENEMY)
            gctx.gameManager().onEnemyKilled(enemy.enemyType.energyReward)
        }

        HitEffect.spawn(gctx, visual.impact, enemy.x, enemy.y)
        gctx.mainWorld().remove(this, MainScene.Layer.PROJECTILE)
    }

    // ───── 드로우 ────────────────────────────────────────────────────────────
    override fun draw(canvas: Canvas) {
        when (visual) {
            Visual.ARROW      -> drawArrow(canvas)
            Visual.MAGIC_BOLT -> drawMagicBolt(canvas)
            Visual.HOLY       -> drawHoly(canvas)
        }
    }

    // ── 화살 : 회전한 선 + 화살촉 ────────────────────────────────────────────
    private fun drawArrow(canvas: Canvas) {
        canvas.save()
        canvas.translate(curX, curY)
        canvas.rotate(angle)
        canvas.drawLine(-18f, 0f, 8f, 0f, arrowShaftPaint)
        // 화살촉
        canvas.drawLine(8f, 0f, -2f, -5f, arrowHeadPaint)
        canvas.drawLine(8f, 0f, -2f,  5f, arrowHeadPaint)
        canvas.restore()
    }

    // ── 마법볼트 : 중심 오브 + 회전 파티클 ───────────────────────────────────
    private fun drawMagicBolt(canvas: Canvas) {
        // 외곽 글로우
        boltGlowPaint.alpha = 100
        canvas.drawCircle(curX, curY, 14f, boltGlowPaint)
        // 중심 오브
        canvas.drawCircle(curX, curY, 7f, boltCorePaint)
        // 궤도 파티클 (3개, 회전)
        canvas.save(); canvas.translate(curX, curY)
        for (i in 0 until 3) {
            val rad = Math.toRadians((i * 120.0 + age * 360))
            val px = (cos(rad) * 11f).toFloat()
            val py = (sin(rad) * 11f).toFloat()
            canvas.drawCircle(px, py, 3f, boltParticlePaint)
        }
        canvas.restore()
    }

    // ── 신성 : 회전 십자 + 빛 링 ─────────────────────────────────────────────
    private fun drawHoly(canvas: Canvas) {
        // 배경 글로우
        holyGlowPaint.alpha = 80
        canvas.drawCircle(curX, curY, 16f, holyGlowPaint)
        // 회전 십자
        canvas.save()
        canvas.translate(curX, curY)
        canvas.rotate(angle + age * 180f)
        canvas.drawLine(-10f, 0f, 10f, 0f, holyCrossPaint)
        canvas.drawLine(0f, -10f, 0f, 10f, holyCrossPaint)
        // 대각선 (두 번째 십자, 45도)
        canvas.rotate(45f)
        canvas.drawLine(-7f, 0f, 7f, 0f, holyCrossPaint)
        canvas.drawLine(0f, -7f, 0f, 7f, holyCrossPaint)
        canvas.restore()
    }

    override fun onRecycle() { target = null }

    // ───── companion (팩토리 + Paint) ─────────────────────────────────────────
    companion object {
        fun spawn(
            gctx: GameContext,
            visual: Visual,
            srcX: Float,
            srcY: Float,
            target: Enemy,
            power: Float,
            isPhysical: Boolean,
        ) {
            val world = gctx.mainWorld()
            val p = world.obtain(Projectile::class.java) ?: Projectile()
            world.add(p.init(visual, srcX, srcY, target, power, isPhysical), MainScene.Layer.PROJECTILE)
        }

        // Arrow
        private val arrowShaftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 3f
            color = "#FFD4A017".toColorInt(); strokeCap = Paint.Cap.ROUND
        }
        private val arrowHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 3f
            color = "#FFC0C0C0".toColorInt(); strokeCap = Paint.Cap.ROUND
        }
        // Magic bolt
        private val boltGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FFAA44FF".toColorInt()
        }
        private val boltCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FFEE99FF".toColorInt()
        }
        private val boltParticlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FFCC66FF".toColorInt()
        }
        // Holy
        private val holyGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FFFFFFCC".toColorInt()
        }
        private val holyCrossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 4f
            color = "#FFFFFFEE".toColorInt(); strokeCap = Paint.Cap.ROUND
        }
    }
}
