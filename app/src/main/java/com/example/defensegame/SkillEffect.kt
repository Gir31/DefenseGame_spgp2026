package com.example.defensegame

import a2dg.objects.IGameObject
import a2dg.objects.IRecyclable
import a2dg.view.GameContext
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.toColorInt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// 스킬 발동 시 표시되는 대형 시각 이펙트.
// HitEffect(충돌 이펙트)보다 크고 오래 지속된다.
// IRecyclable 로 객체 풀에서 재사용.
class SkillEffect private constructor() : IGameObject, IRecyclable {

    enum class Style {
        WARRIOR_HEAL,      // 황금 리플 링 + 힐 십자 + 녹색 파티클
        ROGUE_BURST,       // 8방향 빠른 슬래시 폭발
        ARCHER_MULTISHOT,  // 3방향 화살 궤적 부채꼴
        MAGE_NOVA,         // 마법진 + 충격파 링
        CLERIC_BLESSING,   // 황금 빛 기둥 + 부유 오브
    }

    private var style = Style.WARRIOR_HEAL
    private var cx = 0f;  private var cy = 0f
    private var age = 0f; private var life = 0f

    // 궁수 멀티샷: 최대 3개의 목표 방향 각도 (degrees, 절대값)
    private var archerAngles = floatArrayOf(-20f, 0f, 20f)

    private fun init(style: Style, x: Float, y: Float, gctx: GameContext): SkillEffect {
        this.style = style; cx = x; cy = y; age = 0f
        life = when (style) {
            Style.WARRIOR_HEAL     -> 1.0f
            Style.ROGUE_BURST      -> 0.55f
            Style.ARCHER_MULTISHOT -> 0.75f
            Style.MAGE_NOVA        -> 1.2f
            Style.CLERIC_BLESSING  -> 1.4f
        }
        if (style == Style.ARCHER_MULTISHOT) {
            archerAngles = computeArcherAngles(gctx, x, y)
        }
        return this
    }

    // 사거리 내 적 최대 3개 방향을 각도로 반환.
    // 적이 부족하면 중앙 방향 기준 ±20° 스프레드로 채운다.
    private fun computeArcherAngles(gctx: GameContext, srcX: Float, srcY: Float): FloatArray {
        val rangeSq = Balance.Unit.archerRange * Balance.Unit.archerRange
        val enemies = gctx.mainWorld().objectsAt(MainScene.Layer.ENEMY)
        val found = mutableListOf<Float>()

        var i = 0
        while (i < enemies.size && found.size < 3) {
            val e = enemies[i] as? Enemy
            if (e != null) {
                val dx = e.x - srcX; val dy = e.y - srcY
                if (dx * dx + dy * dy <= rangeSq) {
                    found.add(Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat())
                }
            }
            i++
        }

        return when (found.size) {
            0    -> floatArrayOf(-20f, 0f, 20f)            // 기본 오른쪽 부채꼴
            1    -> floatArrayOf(found[0] - 20f, found[0], found[0] + 20f)
            2    -> floatArrayOf(found[0], (found[0] + found[1]) / 2f, found[1])
            else -> found.take(3).toFloatArray()
        }
    }

    // ── 업데이트 ───────────────────────────────────────────────────────────────
    override fun update(gctx: GameContext) {
        age += gctx.frameTime
        if (age >= life) gctx.mainWorld().remove(this, MainScene.Layer.PROJECTILE)
    }

    // ── 드로우 ────────────────────────────────────────────────────────────────
    override fun draw(canvas: Canvas) {
        val t = (age / life).coerceIn(0f, 1f)
        val a = ((1f - t) * 255).toInt()
        when (style) {
            Style.WARRIOR_HEAL     -> drawWarriorHeal(canvas, t, a)
            Style.ROGUE_BURST      -> drawRogueBurst(canvas, t, a)
            Style.ARCHER_MULTISHOT -> drawArcherMultishot(canvas, t, a)
            Style.MAGE_NOVA        -> drawMageNova(canvas, t, a)
            Style.CLERIC_BLESSING  -> drawClericBlessing(canvas, t, a)
        }
    }

    // ─── 전사: 황금 리플 링 + 십자 + 녹색 파티클 ─────────────────────────────
    private fun drawWarriorHeal(canvas: Canvas, t: Float, alpha: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // 확장 황금 링 (2개, 시차를 두고)
        for (wave in 0..1) {
            val wt = (t - wave * 0.25f).coerceIn(0f, 1f)
            if (wt <= 0f) continue
            val r = 30f + wt * 110f
            val wa = ((1f - wt) * alpha).toInt()
            p.style = Paint.Style.STROKE
            p.strokeWidth = 8f - wt * 5f
            p.color = "#FFFFCC00".toColorInt(); p.alpha = wa
            canvas.drawCircle(cx, cy, r, p)
            p.color = "#FF88FF44".toColorInt(); p.alpha = (wa * 0.5f).toInt()
            canvas.drawCircle(cx, cy, r * 0.75f, p)
        }

        // 힐 십자 (중앙에 고정, 나타났다 사라짐)
        val crossAlpha = when {
            t < 0.3f -> (t / 0.3f * alpha).toInt()
            t > 0.7f -> ((1f - (t - 0.7f) / 0.3f) * alpha).toInt()
            else     -> alpha
        }
        val arm = 22f + t * 8f
        p.style = Paint.Style.STROKE; p.strokeWidth = 9f; p.strokeCap = Paint.Cap.ROUND
        p.color = "#FF88FF44".toColorInt(); p.alpha = crossAlpha
        canvas.drawLine(cx - arm, cy, cx + arm, cy, p)
        canvas.drawLine(cx, cy - arm * 1.4f, cx, cy + arm * 1.4f, p)

        // 녹색 파티클 (위로 떠오름)
        p.style = Paint.Style.FILL; p.strokeCap = Paint.Cap.ROUND
        for (i in 0..5) {
            val px = cx + (i - 2.5f) * 18f
            val py = cy - t * 80f - i * 6f
            val pa = ((1f - t) * alpha).toInt()
            p.color = "#FF66EE44".toColorInt(); p.alpha = pa
            canvas.drawCircle(px, py, 5f - t * 2f, p)
        }
    }

    // ─── 도적: 8방향 슬래시 폭발 ───────────────────────────────────────────────
    private fun drawRogueBurst(canvas: Canvas, t: Float, alpha: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        }

        // 중앙 섬광 원
        val flashAlpha = (alpha * maxOf(0f, 1f - t * 4f)).toInt()
        p.style = Paint.Style.FILL; p.color = "#FFFF6666".toColorInt(); p.alpha = flashAlpha
        canvas.drawCircle(cx, cy, 24f * (1f - t), p)
        p.style = Paint.Style.STROKE

        // 8방향 슬래시 (2겹: 굵은 + 얇은)
        canvas.save(); canvas.translate(cx, cy)
        val len = 20f + t * 90f
        val sa = ((1f - t * 0.8f) * alpha).toInt()
        for (i in 0 until 8) {
            canvas.rotate(45f)
            p.strokeWidth = 6f; p.color = "#FFFF4444".toColorInt(); p.alpha = sa
            canvas.drawLine(12f, 0f, len, 0f, p)
            p.strokeWidth = 2f; p.color = "#FFFFAAAA".toColorInt(); p.alpha = (sa * 0.7f).toInt()
            canvas.drawLine(12f, -5f, len * 0.8f, -5f, p)
            canvas.drawLine(12f,  5f, len * 0.8f,  5f, p)
        }
        canvas.restore()

        // 팽창 링
        val rAlpha = (alpha * maxOf(0f, 1f - t * 2.5f)).toInt()
        p.strokeWidth = 4f; p.color = "#FFCC2222".toColorInt(); p.alpha = rAlpha
        canvas.drawCircle(cx, cy, len, p)
    }

    // ─── 궁수: 3방향 화살 궤적 (적 방향) ──────────────────────────────────────
    private fun drawArcherMultishot(canvas: Canvas, t: Float, alpha: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }

        // init 시 계산된 적 방향 각도 사용
        val arrowLen = 40f + t * 180f

        canvas.save(); canvas.translate(cx, cy)
        for (deg in archerAngles) {
            canvas.save(); canvas.rotate(deg)

            // 빛 궤적 (후광)
            val trailA = ((1f - t) * alpha).toInt()
            p.style = Paint.Style.STROKE; p.strokeWidth = 10f
            p.color = "#FF44DDFF".toColorInt(); p.alpha = (trailA * 0.35f).toInt()
            canvas.drawLine(0f, 0f, arrowLen, 0f, p)

            // 메인 화살 궤적
            p.strokeWidth = 4f; p.color = "#FF88EEFF".toColorInt(); p.alpha = trailA
            canvas.drawLine(0f, 0f, arrowLen, 0f, p)

            // 화살촉
            val tipA = ((1f - t * 0.6f) * alpha).toInt()
            p.style = Paint.Style.FILL; p.color = "#FFCCF8FF".toColorInt(); p.alpha = tipA
            val tx = arrowLen; val hs = 8f - t * 4f
            canvas.drawLine(tx, 0f, tx - 14f, -hs, p)
            canvas.drawLine(tx, 0f, tx - 14f,  hs, p)

            canvas.restore()
        }
        canvas.restore()

        // 릴리즈 섬광
        val flashA = (alpha * maxOf(0f, 1f - t * 5f)).toInt()
        p.style = Paint.Style.FILL; p.color = "#FF44DDFF".toColorInt(); p.alpha = flashA
        canvas.drawCircle(cx, cy, 18f * (1f - t * 2f).coerceAtLeast(0f), p)
    }

    // ─── 마법사: 마법진 + 충격파 ────────────────────────────────────────────────
    private fun drawMageNova(canvas: Canvas, t: Float, alpha: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val range = 290f  // mage range

        // 충격파 링 (빠르게 팽창)
        val shockR = range * minOf(t * 1.3f, 1f)
        val shockA = (alpha * maxOf(0f, 1f - t * 1.8f)).toInt()
        p.style = Paint.Style.STROKE; p.strokeWidth = 6f
        p.color = "#FFAA44FF".toColorInt(); p.alpha = shockA
        canvas.drawCircle(cx, cy, shockR, p)
        p.strokeWidth = 2f; p.color = "#FFDD99FF".toColorInt(); p.alpha = (shockA * 0.6f).toInt()
        canvas.drawCircle(cx, cy, shockR * 0.85f, p)

        // 회전 마법진 (육각형 기반)
        canvas.save(); canvas.translate(cx, cy)
        canvas.rotate(t * 180f)
        val magicR = 70f + t * 30f
        val magicA = when {
            t < 0.2f -> (t / 0.2f * alpha).toInt()
            t > 0.6f -> ((1f - (t - 0.6f) / 0.4f) * alpha).toInt()
            else     -> alpha
        }
        p.style = Paint.Style.STROKE; p.strokeWidth = 2.5f
        p.color = "#FFCC66FF".toColorInt(); p.alpha = magicA
        for (i in 0 until 6) {
            val a1 = Math.toRadians((i * 60).toDouble())
            val a2 = Math.toRadians(((i + 1) * 60).toDouble())
            canvas.drawLine(
                (cos(a1) * magicR).toFloat(), (sin(a1) * magicR).toFloat(),
                (cos(a2) * magicR).toFloat(), (sin(a2) * magicR).toFloat(), p
            )
            // 중심 → 꼭짓점 (3개 건너 하나씩)
            if (i % 2 == 0) {
                p.alpha = (magicA * 0.5f).toInt()
                canvas.drawLine(0f, 0f, (cos(a1) * magicR).toFloat(), (sin(a1) * magicR).toFloat(), p)
                p.alpha = magicA
            }
        }

        // 역방향 외곽 원
        canvas.rotate(-t * 300f)
        p.strokeWidth = 1.5f; p.color = "#FF9944EE".toColorInt(); p.alpha = (magicA * 0.7f).toInt()
        canvas.drawCircle(0f, 0f, magicR * 1.35f, p)
        // 작은 룬 점 (8개)
        p.style = Paint.Style.FILL; p.color = "#FFDD88FF".toColorInt()
        for (i in 0 until 8) {
            val a = Math.toRadians((i * 45).toDouble())
            canvas.drawCircle(
                (cos(a) * magicR * 1.35f).toFloat(),
                (sin(a) * magicR * 1.35f).toFloat(),
                4f, p
            )
        }
        canvas.restore()

        // 중앙 폭발 플래시
        val explodeT = (t * 3f).coerceAtMost(1f)
        val explodeR = 36f * sin(explodeT * PI.toFloat())
        val explodeA = (alpha * maxOf(0f, 1f - t * 2.5f)).toInt()
        p.style = Paint.Style.FILL; p.color = "#FFEE88FF".toColorInt(); p.alpha = explodeA
        canvas.drawCircle(cx, cy, explodeR, p)
    }

    // ─── 성직자: 황금 빛 기둥 + 십자 + 부유 오브 ──────────────────────────────
    private fun drawClericBlessing(canvas: Canvas, t: Float, alpha: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // 빛 기둥 (위로 솟구침)
        val pillarH = 240f * minOf(t * 2.5f, 1f)
        val pillarFade = maxOf(0f, 1f - (t - 0.4f) / 0.6f).coerceAtLeast(0f)
        val pillarA = (alpha * pillarFade * 0.65f).toInt()
        p.style = Paint.Style.FILL; p.color = "#FFFFFFCC".toColorInt(); p.alpha = pillarA
        canvas.drawRect(cx - 18f, cy - pillarH, cx + 18f, cy + 8f, p)
        // 기둥 외곽 글로우
        p.color = "#FFFFEEAA".toColorInt(); p.alpha = (pillarA * 0.4f).toInt()
        canvas.drawRect(cx - 36f, cy - pillarH * 0.9f, cx + 36f, cy, p)

        // 팽창 황금 십자
        val crossProgress = minOf(t * 2f, 1f)
        val arm  = crossProgress * 120f
        val armV = crossProgress * 160f
        val crossA = when {
            t < 0.15f -> (t / 0.15f * alpha).toInt()
            t > 0.65f -> ((1f - (t - 0.65f) / 0.35f) * alpha).toInt()
            else      -> alpha
        }
        p.style = Paint.Style.STROKE; p.strokeWidth = 10f; p.strokeCap = Paint.Cap.ROUND
        p.color = "#FFFFD700".toColorInt(); p.alpha = crossA
        canvas.drawLine(cx - arm, cy, cx + arm, cy, p)
        canvas.drawLine(cx, cy - armV, cx, cy + arm * 0.6f, p)
        // 얇은 보조 십자 (45도 회전)
        p.strokeWidth = 4f; p.color = "#FFFFFFCC".toColorInt(); p.alpha = (crossA * 0.55f).toInt()
        val diag = arm * 0.65f
        canvas.drawLine(cx - diag, cy - diag, cx + diag, cy + diag, p)
        canvas.drawLine(cx + diag, cy - diag, cx - diag, cy + diag, p)

        // 부유 황금 오브 (5개, 아치형으로 위로)
        p.style = Paint.Style.FILL
        for (i in 0..4) {
            val phase = (i / 4f) * 0.3f  // 시차
            val orbt = (t - phase).coerceAtLeast(0f)
            if (orbt <= 0f) continue
            val orbX = cx + (i - 2f) * 32f + sin(orbt * PI.toFloat() * 2) * 10f
            val orbY = cy - orbt * 100f - i * 5f
            val orbA = ((1f - orbt.coerceAtMost(1f)) * alpha).toInt()
            // 오브 글로우
            p.color = "#FFFFD700".toColorInt(); p.alpha = (orbA * 0.35f).toInt()
            canvas.drawCircle(orbX, orbY, 10f, p)
            // 오브 코어
            p.color = "#FFFFFFEE".toColorInt(); p.alpha = orbA
            canvas.drawCircle(orbX, orbY, 5f, p)
        }

        // 바닥 황금 링 (퍼져나감)
        val baseR = t * 80f
        val baseA = ((1f - t * 0.7f) * alpha).toInt()
        p.style = Paint.Style.STROKE; p.strokeWidth = 4f
        p.color = "#FFFFD700".toColorInt(); p.alpha = baseA
        canvas.drawCircle(cx, cy, baseR, p)
    }

    override fun onRecycle() {}

    companion object {
        fun spawn(gctx: GameContext, style: Style, x: Float, y: Float) {
            val world = gctx.mainWorld()
            val e = world.obtain(SkillEffect::class.java) ?: SkillEffect()
            world.add(e.init(style, x, y, gctx), MainScene.Layer.PROJECTILE)
        }

        // PlayerUnit.Type → SkillEffect.Style 변환
        fun styleFor(type: PlayerUnit.Type) = when (type) {
            PlayerUnit.Type.WARRIOR -> Style.WARRIOR_HEAL
            PlayerUnit.Type.ROGUE   -> Style.ROGUE_BURST
            PlayerUnit.Type.ARCHER  -> Style.ARCHER_MULTISHOT
            PlayerUnit.Type.MAGE    -> Style.MAGE_NOVA
            PlayerUnit.Type.CLERIC  -> Style.CLERIC_BLESSING
        }
    }
}
