package com.example.defensegame

import a2dg.objects.IRecyclable
import a2dg.util.Gauge
import a2dg.view.GameContext
import android.graphics.Canvas
import android.graphics.PointF
import androidx.core.graphics.toColorInt
import kotlin.math.sqrt

class Enemy private constructor(gctx: GameContext, resId: Int) :
    UnitAnimSprite(gctx, resId, idleFps = 4f), IRecyclable {

    // ───── 타입 정의 ─────────────────────────────────────────────────────────
    // attackResId / dieResId 가 null 이면 idle 스프라이트를 그대로 사용한다.
    // 스프라이트 에셋이 준비되면 각 필드에 R.mipmap.enemy_XXX_attack 등을 채워넣으면 된다.
    enum class Type(
        val maxHp:          Float,
        val def:            Float,
        val resPct:         Float,
        val attackDmg:      Float,
        val attackInterval: Float,
        val speed:          Float,
        val size:           Float,
        val energyReward:   Int,
        val idleResId:      Int,
        val attackResId:    Int?,   // null → idle 스프라이트 대용
        val dieResId:       Int?,   // null → idle 스프라이트 대용
        val idleFps:        Float,
        val attackFps:      Float,
        val dieFps:         Float,
    ) {
        NORMAL(
            Balance.Enemy.normalHp,  Balance.Enemy.normalDef,  Balance.Enemy.normalRes,
            Balance.Enemy.normalAtk, Balance.Enemy.normalInterval, Balance.Enemy.normalSpeed, Balance.Enemy.normalSize,
            Balance.Enemy.normalEnergyReward,
            idleResId   = R.mipmap.enemy_normal,
            attackResId = R.mipmap.enemy_normal_attack,
            dieResId    = R.mipmap.enemy_normal_die,
            idleFps = 4f, attackFps = 8f, dieFps = 8f,
        ),
        TANKER(
            Balance.Enemy.tankerHp,  Balance.Enemy.tankerDef,  Balance.Enemy.tankerRes,
            Balance.Enemy.tankerAtk, Balance.Enemy.tankerInterval, Balance.Enemy.tankerSpeed, Balance.Enemy.tankerSize,
            Balance.Enemy.tankerEnergyReward,
            idleResId   = R.mipmap.enemy_tanker,
            attackResId = R.mipmap.enemy_tanker_attack,
            dieResId    = R.mipmap.enemy_tanker_die,
            idleFps = 3f, attackFps = 6f, dieFps = 6f,
        ),
        SWARM(
            Balance.Enemy.swarmHp,   Balance.Enemy.swarmDef,   Balance.Enemy.swarmRes,
            Balance.Enemy.swarmAtk, Balance.Enemy.swarmInterval, Balance.Enemy.swarmSpeed, Balance.Enemy.swarmSize,
            Balance.Enemy.swarmEnergyReward,
            idleResId   = R.mipmap.enemy_swarm,
            attackResId = R.mipmap.enemy_swarm_attack,
            dieResId    = R.mipmap.enemy_swarm_die,
            idleFps = 6f, attackFps = 12f, dieFps = 10f,
        ),
        SPECIAL(
            Balance.Enemy.specialHp, Balance.Enemy.specialDef, Balance.Enemy.specialRes,
            Balance.Enemy.specialAtk, Balance.Enemy.specialInterval, Balance.Enemy.specialSpeed, Balance.Enemy.specialSize,
            Balance.Enemy.specialEnergyReward,
            idleResId   = R.mipmap.enemy_special,
            attackResId = R.mipmap.enemy_special_attack,
            dieResId    = R.mipmap.enemy_special_die,
            idleFps = 4f, attackFps = 8f, dieFps = 8f,
        ),
        RANGED(
            Balance.Enemy.rangedHp,  Balance.Enemy.rangedDef,  Balance.Enemy.rangedRes,
            Balance.Enemy.rangedAtk, Balance.Enemy.rangedInterval, Balance.Enemy.rangedSpeed, Balance.Enemy.rangedSize,
            Balance.Enemy.rangedEnergyReward,
            idleResId   = R.mipmap.enemy_ranged,
            attackResId = R.mipmap.enemy_ranged_attack,
            dieResId    = R.mipmap.enemy_ranged_die,
            idleFps = 4f, attackFps = 8f, dieFps = 8f,
        ),
    }

    // ───── 상태 ──────────────────────────────────────────────────────────────
    private lateinit var type: Type
    private lateinit var path: List<PointF>

    val enemyType:  Type  get() = type
    val def:   Float get() = type.def
    val resPct:Float get() = type.resPct

    var hp = 0f;    private set
    var maxHp = 0f; private set

    private var waypointIndex = 0
    private var pendingRemoval = false

    // ── 블로킹 ────────────────────────────────────────────────────────────────
    private var blocker: PlayerUnit? = null
    private var attackCooldown = 0f

    // ───── 초기화 ─────────────────────────────────────────────────────────────
    private fun init(type: Type, path: List<PointF>): Enemy {
        this.type = type
        this.path = path
        hp = type.maxHp; maxHp = type.maxHp
        waypointIndex = 0
        blocker = null; attackCooldown = 0f
        pendingRemoval = false
        setSize(type.size, type.size)
        if (path.isNotEmpty()) setCenter(path[0].x, path[0].y)

        // IDLE 비트맵 교체 (오브젝트 풀 재활용 시 타입이 바뀔 수 있음)
        refreshIdleBitmap(type.idleResId)

        // ATTACK / DIE 애니메이션 등록
        registerAnim(State.ATTACK, type.attackResId ?: type.idleResId, type.attackFps)
        registerAnim(State.DIE,    type.dieResId    ?: type.idleResId, type.dieFps)

        // 오브젝트 풀 재활용 시 이전 DIE 상태 잠금 등을 초기화한다
        resetToIdle()
        return this
    }

    // ───── 업데이트 ──────────────────────────────────────────────────────────
    override fun update(gctx: GameContext) {
        // DIE 애니메이션 완료 → World 에서 제거
        if (pendingRemoval) {
            if (isDieAnimFinished()) {
                gctx.mainWorld().remove(this, MainScene.Layer.ENEMY)
            }
            return
        }

        // 블로커가 죽었으면 해제
        if (blocker != null && blocker!!.isDead()) blocker = null

        // 블로커 탐색
        if (blocker == null) blocker = findBlocker(gctx)

        // 블로커가 있으면 멈추고 공격
        if (blocker != null) {
            attackCooldown -= gctx.frameTime
            if (attackCooldown <= 0f) {
                attackBlocker(gctx, blocker!!)
                attackCooldown = type.attackInterval
            }
            return
        }

        // 정상 이동
        if (waypointIndex >= path.size) return
        val target = path[waypointIndex]
        val dx = target.x - x; val dy = target.y - y
        val dist = sqrt(dx * dx + dy * dy)
        val step = type.speed * gctx.frameTime

        if (dist <= step) {
            setCenter(target.x, target.y)
            waypointIndex++
            if (waypointIndex >= path.size) {
                gctx.mainWorld().remove(this, MainScene.Layer.ENEMY)
                gctx.gameManager().onEnemyReachedEnd()
            }
        } else {
            setCenter(x + dx / dist * step, y + dy / dist * step)
        }
    }

    private fun findBlocker(gctx: GameContext): PlayerUnit? {
        val units = gctx.mainWorld().objectsAt(MainScene.Layer.UNIT)
        var i = 0
        while (i < units.size) {
            val u = units[i] as? PlayerUnit
            if (u != null && u.unitType.placementType == PlayerUnit.PlacementType.GROUND) {
                val dx = u.x - x; val dy = u.y - y
                if (dx * dx + dy * dy <= BLOCK_RANGE_SQ) return u
            }
            i++
        }
        return null
    }

    private fun attackBlocker(gctx: GameContext, unit: PlayerUnit) {
        val durationMs = (type.attackInterval * 1000f).toLong()
        playAnim(State.ATTACK, durationMs)

        val damage = Balance.DamageCalc.physical(type.attackDmg, unit.unitType.defense)
        unit.takeDamage(damage)
        if (unit.isDead()) blocker = null
    }

    // ───── 드로우 ────────────────────────────────────────────────────────────
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (pendingRemoval) return
        val barW = width * 0.9f
        lifeGauge.draw(canvas, x - barW / 2f, y + height / 2f + 2f, barW, hp / maxHp)
    }

    // ───── 피격 ──────────────────────────────────────────────────────────────
    fun takeDamage(amount: Float) {
        if (pendingRemoval) return
        hp = (hp - amount).coerceAtLeast(0f)
        if (hp <= 0f) startDying()
    }

    fun isDead() = hp <= 0f

    private fun startDying() {
        if (pendingRemoval) return
        pendingRemoval = true
        // dieFps 와 프레임 수(약 8프레임)로 재생 시간 계산
        val dieDurMs = (1000f / type.dieFps * 8).toLong()
        playAnim(State.DIE, durationMs = dieDurMs)
    }

    // ───── IRecyclable ───────────────────────────────────────────────────────
    override fun onRecycle() {
        blocker = null
        pendingRemoval = false
    }

    // ───── companion ─────────────────────────────────────────────────────────
    companion object {
        private const val BLOCK_RANGE_SQ = 55f * 55f

        fun get(gctx: GameContext, type: Type, path: List<PointF>): Enemy {
            val world = gctx.mainWorld()
            val enemy = world.obtain(Enemy::class.java) ?: Enemy(gctx, type.idleResId)
            return enemy.init(type, path)
        }

        private val lifeGauge = Gauge(
            thickness = 0.25f,
            fgColor = "#C0228822".toColorInt(),
            bgColor = "#A0FF4444".toColorInt(),
        )
    }
}