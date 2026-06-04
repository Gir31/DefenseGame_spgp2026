package com.example.defensegame

import a2dg.util.Gauge
import a2dg.view.GameContext
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.toColorInt

// 플레이어가 배치하는 유닛의 기반 클래스.
// UnitAnimSprite 를 상속해 IDLE / ATTACK / SKILL / DIE 애니메이션을 자동 전환한다.
open class PlayerUnit protected constructor(
    gctx: GameContext,
    val unitType: Type,
) : UnitAnimSprite(gctx, unitType.idleResId, idleFps = unitType.idleFps) {

    // ───── 배치 구분 ─────────────────────────────────────────────────────────
    enum class PlacementType {
        GROUND,
        ELEVATED,
    }

    // ───── 타입 정의 ─────────────────────────────────────────────────────────
    // attackResId / skillResId / dieResId 가 null 이면 idle 스프라이트를 대용한다.
    // 에셋이 준비되면 null 을 R.mipmap.unit_XXX_attack 등으로 교체하면 된다.
    enum class Type(
        val maxHp: Float,
        val defense: Float,
        val magicResist: Float,
        val attackPower: Float,
        val attackInterval: Float,
        val range: Float,
        val energyCost: Int,
        val isPhysical: Boolean,
        val placementType: PlacementType,
        val projectile: Projectile.Visual?,
        val meleeEffect: HitEffect.Style,
        val retreatCooldown: Float,
        val idleResId:   Int,
        val attackResId: Int?,   // null → idle 대용
        val skillResId:  Int?,   // null → idle 대용
        val dieResId:    Int?,   // null → idle 대용
        val idleFps:   Float,
        val attackFps: Float,
        val skillFps:  Float,
        val dieFps:    Float,
    ) {
        WARRIOR(
            Balance.Unit.warriorHp, Balance.Unit.warriorDef, Balance.Unit.warriorMr,
            Balance.Unit.warriorAtk, Balance.Unit.warriorInterval, Balance.Unit.warriorRange,
            Balance.Unit.warriorCost, isPhysical = true, PlacementType.GROUND,
            projectile = null, meleeEffect = HitEffect.Style.SLASH,
            retreatCooldown = Balance.Unit.warriorCooldown,
            idleResId   = R.mipmap.unit_warrior,
            attackResId = R.mipmap.unit_warrior_attack,
            skillResId  = R.mipmap.unit_warrior_skill,
            dieResId    = R.mipmap.unit_warrior_die,
            idleFps = 6f, attackFps = 10f, skillFps = 8f, dieFps = 8f,
        ),
        ROGUE(
            Balance.Unit.rogueHp, Balance.Unit.rogueDef, Balance.Unit.rogueMr,
            Balance.Unit.rogueAtk, Balance.Unit.rogueInterval, Balance.Unit.rogueRange,
            Balance.Unit.rogueCost, isPhysical = true, PlacementType.GROUND,
            projectile = null, meleeEffect = HitEffect.Style.STAB,
            retreatCooldown = Balance.Unit.rogueCooldown,
            idleResId   = R.mipmap.unit_rogue,
            attackResId = R.mipmap.unit_rogue_attack,
            skillResId  = R.mipmap.unit_rogue_skill,
            dieResId    = R.mipmap.unit_rogue_die,
            idleFps = 8f, attackFps = 14f, skillFps = 12f, dieFps = 8f,
        ),
        ARCHER(
            Balance.Unit.archerHp, Balance.Unit.archerDef, Balance.Unit.archerMr,
            Balance.Unit.archerAtk, Balance.Unit.archerInterval, Balance.Unit.archerRange,
            Balance.Unit.archerCost, isPhysical = true, PlacementType.ELEVATED,
            projectile = Projectile.Visual.ARROW, meleeEffect = HitEffect.Style.ARROW_HIT,
            retreatCooldown = Balance.Unit.archerCooldown,
            idleResId   = R.mipmap.unit_archer,
            attackResId = R.mipmap.unit_archer_attack,
            skillResId  = R.mipmap.unit_archer_skill,
            dieResId    = R.mipmap.unit_archer_die,
            idleFps = 6f, attackFps = 10f, skillFps = 10f, dieFps = 8f,
        ),
        MAGE(
            Balance.Unit.mageHp, Balance.Unit.mageDef, Balance.Unit.mageMr,
            Balance.Unit.mageAtk, Balance.Unit.mageInterval, Balance.Unit.mageRange,
            Balance.Unit.mageCost, isPhysical = false, PlacementType.ELEVATED,
            projectile = Projectile.Visual.MAGIC_BOLT, meleeEffect = HitEffect.Style.MAGIC_HIT,
            retreatCooldown = Balance.Unit.mageCooldown,
            idleResId   = R.mipmap.unit_mage,
            attackResId = R.mipmap.unit_mage_attack,
            skillResId  = R.mipmap.unit_mage_skill,
            dieResId    = R.mipmap.unit_mage_die,
            idleFps = 5f, attackFps = 8f, skillFps = 6f, dieFps = 7f,
        ),
        CLERIC(
            Balance.Unit.clericHp, Balance.Unit.clericDef, Balance.Unit.clericMr,
            Balance.Unit.clericAtk, Balance.Unit.clericInterval, Balance.Unit.clericRange,
            Balance.Unit.clericCost, isPhysical = false, PlacementType.ELEVATED,
            projectile = Projectile.Visual.HOLY, meleeEffect = HitEffect.Style.HOLY_HIT,
            retreatCooldown = Balance.Unit.clericCooldown,
            idleResId   = R.mipmap.unit_cleric,
            attackResId = R.mipmap.unit_cleric_attack,
            skillResId  = R.mipmap.unit_cleric_skill,
            dieResId    = R.mipmap.unit_cleric_die,
            idleFps = 5f, attackFps = 8f, skillFps = 6f, dieFps = 7f,
        ),
    }

    // ───── 상태 ──────────────────────────────────────────────────────────────
    var hp: Float = unitType.maxHp
        protected set

    var sp: Float = 0f
        private set
    val skillReady get() = sp >= Balance.Unit.maxSp

    private var attackCooldown = 0f
    private var pendingRemoval = false

    init {
        setSize(Balance.Unit.unitSize, Balance.Unit.unitSize)
        registerAnim(State.ATTACK, unitType.attackResId ?: unitType.idleResId, unitType.attackFps)
        registerAnim(State.SKILL,  unitType.skillResId  ?: unitType.idleResId, unitType.skillFps)
        registerAnim(State.DIE,    unitType.dieResId    ?: unitType.idleResId, unitType.dieFps)
    }

    // ───── 업데이트 ──────────────────────────────────────────────────────────
    override fun update(gctx: GameContext) {
        if (pendingRemoval) {
            if (deathAnimDone) {
                gctx.mainWorld().remove(this, MainScene.Layer.UNIT)
            }
            return
        }

        sp = (sp + Balance.Unit.spChargeRate * gctx.frameTime).coerceAtMost(Balance.Unit.maxSp)

        attackCooldown -= gctx.frameTime
        if (attackCooldown > 0f) return

        val target = findTarget(gctx) ?: return
        attackTarget(gctx, target)
        attackCooldown = unitType.attackInterval
        sp = (sp + Balance.Unit.spPerAttack).coerceAtMost(Balance.Unit.maxSp)
    }

    // ── 스킬 발동 ─────────────────────────────────────────────────────────────
    fun activateSkill(gctx: GameContext): Boolean {
        if (!skillReady) return false
        sp = 0f
        playAnim(State.SKILL, 1200L)
        onSkillActivated(gctx)
        return true
    }

    protected open fun onSkillActivated(gctx: GameContext) {
        SkillEffect.spawn(gctx, SkillEffect.styleFor(unitType), x, y)

        when (unitType) {
            Type.WARRIOR -> heal(300f)

            Type.ROGUE -> {
                val enemies = gctx.mainWorld().objectsAt(MainScene.Layer.ENEMY).toList()
                val rangeSq = unitType.range * unitType.range
                for (obj in enemies) {
                    val e = obj as? Enemy ?: continue
                    val dx = e.x - x; val dy = e.y - y
                    if (dx*dx + dy*dy > rangeSq) continue
                    val dmg = Balance.DamageCalc.physical(unitType.attackPower * 4f, e.def)
                    e.takeDamage(dmg)
                    if (e.isDead()) {
                        gctx.mainWorld().remove(e, MainScene.Layer.ENEMY)
                        gctx.gameManager().onEnemyKilled(e.enemyType.energyReward)
                    }
                    HitEffect.spawn(gctx, HitEffect.Style.STAB, e.x, e.y)
                }
            }

            Type.ARCHER -> {
                val enemies = gctx.mainWorld().objectsAt(MainScene.Layer.ENEMY)
                val rangeSq = unitType.range * unitType.range
                var count = 0; var i = 0
                while (i < enemies.size && count < 3) {
                    val e = enemies[i] as? Enemy
                    if (e != null) {
                        val dx = e.x - x; val dy = e.y - y
                        if (dx*dx + dy*dy <= rangeSq) {
                            Projectile.spawn(gctx, Projectile.Visual.ARROW, x, y, e,
                                unitType.attackPower * 2f, isPhysical = true)
                            count++
                        }
                    }
                    i++
                }
            }

            Type.MAGE -> {
                val enemies = gctx.mainWorld().objectsAt(MainScene.Layer.ENEMY).toList()
                val rangeSq = unitType.range * unitType.range
                for (obj in enemies) {
                    val e = obj as? Enemy ?: continue
                    val dx = e.x - x; val dy = e.y - y
                    if (dx*dx + dy*dy > rangeSq) continue
                    val dmg = Balance.DamageCalc.magic(unitType.attackPower * 3f, e.resPct)
                    e.takeDamage(dmg)
                    if (e.isDead()) {
                        gctx.mainWorld().remove(e, MainScene.Layer.ENEMY)
                        gctx.gameManager().onEnemyKilled(e.enemyType.energyReward)
                    }
                    HitEffect.spawn(gctx, HitEffect.Style.MAGIC_HIT, e.x, e.y)
                }
            }

            Type.CLERIC -> {
                heal(150f)
                val allies = gctx.mainWorld().objectsAt(MainScene.Layer.UNIT)
                val rangeSq = unitType.range * unitType.range
                for (obj in allies) {
                    val ally = obj as? PlayerUnit ?: continue
                    if (ally === this) continue
                    val dx = ally.x - x; val dy = ally.y - y
                    if (dx*dx + dy*dy <= rangeSq) ally.heal(200f)
                }
                HitEffect.spawn(gctx, HitEffect.Style.HOLY_HIT, x, y)
            }
        }
    }

    // ── 타겟 탐색 ─────────────────────────────────────────────────────────────
    protected open fun findTarget(gctx: GameContext): Enemy? {
        val enemies = gctx.mainWorld().objectsAt(MainScene.Layer.ENEMY)
        var nearest: Enemy? = null
        var nearestDistSq = unitType.range * unitType.range
        var i = 0
        while (i < enemies.size) {
            val e = enemies[i] as? Enemy
            if (e != null) {
                val dx = e.x - x; val dy = e.y - y
                val dSq = dx * dx + dy * dy
                if (dSq < nearestDistSq) { nearestDistSq = dSq; nearest = e }
            }
            i++
        }
        return nearest
    }

    // ── 공격 ──────────────────────────────────────────────────────────────────
    protected open fun attackTarget(gctx: GameContext, target: Enemy) {
        val durationMs = (unitType.attackInterval * 1000f).toLong()
        playAnim(State.ATTACK, durationMs)

        val proj = unitType.projectile
        if (proj != null) {
            Projectile.spawn(gctx, proj, x, y, target, unitType.attackPower, unitType.isPhysical)
        } else {
            val damage = if (unitType.isPhysical)
                Balance.DamageCalc.physical(unitType.attackPower, target.def)
            else
                Balance.DamageCalc.magic(unitType.attackPower, target.resPct)
            target.takeDamage(damage)
            if (target.isDead()) {
                gctx.mainWorld().remove(target, MainScene.Layer.ENEMY)
                gctx.gameManager().onEnemyKilled(target.enemyType.energyReward)
            }
            HitEffect.spawn(gctx, unitType.meleeEffect, target.x, target.y)
        }
    }

    // ───── HP 관리 ───────────────────────────────────────────────────────────
    fun takeDamage(amount: Float) {
        if (pendingRemoval) return
        val actual = Balance.DamageCalc.physical(amount, unitType.defense)
        hp = (hp - actual).coerceAtLeast(0f)
        if (hp <= 0f) startDying()
    }

    fun heal(amount: Float) {
        hp = (hp + amount).coerceAtMost(unitType.maxHp)
    }

    fun isDead() = hp <= 0f

    private fun startDying() {
        if (pendingRemoval) return
        pendingRemoval = true
        playAnim(State.DIE)
    }

    fun placeAt(cx: Float, cy: Float) = setCenter(cx, cy)

    // ───── 드로우 ────────────────────────────────────────────────────────────
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (pendingRemoval) return
        val barW = width * 0.9f
        val barLeft = x - barW / 2f
        hpGauge.draw(canvas, barLeft, y + height / 2f + 2f, barW, hp / unitType.maxHp)
        val spY = y + height / 2f + 14f
        canvas.drawRoundRect(barLeft, spY, barLeft + barW, spY + 6f, 3f, 3f, spBgPaint)
        val spFill = barW * (sp / Balance.Unit.maxSp)
        if (spFill > 0f) {
            canvas.drawRoundRect(barLeft, spY, barLeft + spFill, spY + 6f, 3f, 3f,
                if (skillReady) spReadyPaint else spFillPaint)
        }
    }

    // ───── 팩토리 ─────────────────────────────────────────────────────────────
    companion object {
        fun create(gctx: GameContext, type: Type): PlayerUnit = when (type) {
            Type.ARCHER -> ArcherUnit(gctx)
            Type.CLERIC -> ClericUnit(gctx)
            else        -> PlayerUnit(gctx, type)
        }

        private val spBgPaint    = Paint().apply { color = "#55FFFFFF".toColorInt() }
        private val spFillPaint  = Paint().apply { color = "#FFFFCC00".toColorInt() }
        private val spReadyPaint = Paint().apply { color = "#FFFFFFEE".toColorInt() }

        private val hpGauge = Gauge(
            thickness = 0.22f,
            fgColor = "#CC22AA22".toColorInt(),
            bgColor = "#99FF3333".toColorInt(),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 궁수: RANGED 적 우선 타겟팅
// ─────────────────────────────────────────────────────────────────────────────
class ArcherUnit(gctx: GameContext) : PlayerUnit(gctx, Type.ARCHER) {
    override fun findTarget(gctx: GameContext): Enemy? {
        val enemies = gctx.mainWorld().objectsAt(MainScene.Layer.ENEMY)
        val rangeSq = Type.ARCHER.range * Type.ARCHER.range
        var rangedTarget: Enemy? = null
        var nearest: Enemy? = null
        var nearestDSq = rangeSq
        var i = 0
        while (i < enemies.size) {
            val e = enemies[i] as? Enemy
            if (e != null) {
                val dx = e.x - x; val dy = e.y - y
                val dSq = dx * dx + dy * dy
                if (dSq <= rangeSq) {
                    if (e.enemyType == Enemy.Type.RANGED && rangedTarget == null) rangedTarget = e
                    if (dSq < nearestDSq) { nearestDSq = dSq; nearest = e }
                }
            }
            i++
        }
        return rangedTarget ?: nearest
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 성직자: 공격 + 주기적 힐링
// ─────────────────────────────────────────────────────────────────────────────
class ClericUnit(gctx: GameContext) : PlayerUnit(gctx, Type.CLERIC) {
    private var healCooldown = 0f

    override fun update(gctx: GameContext) {
        super.update(gctx)
        healCooldown -= gctx.frameTime
        if (healCooldown <= 0f) {
            healNearbyAllies(gctx)
            healCooldown = HEAL_INTERVAL
        }
    }

    private fun healNearbyAllies(gctx: GameContext) {
        val allies = gctx.mainWorld().objectsAt(MainScene.Layer.UNIT)
        var i = 0
        while (i < allies.size) {
            val ally = allies[i] as? PlayerUnit
            if (ally != null && ally !== this) {
                val dx = ally.x - x; val dy = ally.y - y
                if (dx * dx + dy * dy <= HEAL_RANGE_SQ) ally.heal(HEAL_AMOUNT)
            }
            i++
        }
    }

    companion object {
        private const val HEAL_INTERVAL = 3.0f
        private const val HEAL_AMOUNT   = 20f
        private const val HEAL_RANGE_SQ = 200f * 200f
    }
}