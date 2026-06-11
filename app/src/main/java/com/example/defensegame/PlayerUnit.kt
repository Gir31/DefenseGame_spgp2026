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
        val size:      Float,
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
            idleFps = 8f, attackFps = 9f, skillFps = 9f, dieFps = 9f,
            size = 138f,
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
            idleFps = 8f, attackFps = 9f, skillFps = 9f, dieFps = 9f,
            size = 138f,
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
            idleFps = 8f, attackFps = 9f, skillFps = 9f, dieFps = 9f,
            size = 174f,
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
            idleFps = 8f, attackFps = 9f, skillFps = 9f, dieFps = 9f,
            size = 102f,
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
            idleFps = 8f, attackFps = 9f, skillFps = 9f, dieFps = 9f,
            size = 180f,
        ),
    }

    // ───── 상태 ──────────────────────────────────────────────────────────────
    var hp: Float = unitType.maxHp
        protected set

    var sp: Float = 0f
        private set
    val skillReady get() = sp >= Balance.Unit.maxSp

    private var attackCooldown = 0f

    // DIE 애니메이션 재생 후 world 제거를 PlacementController 에 알리는 콜백.
    // PlacementController 가 배치 시 등록하고, 사망 처리가 끝나면 호출된다.
    var onDied: (() -> Unit)? = null

    // 사망 처리가 이미 시작됐는지 여부 (중복 호출 방지)
    private var dyingStarted = false

    init {
        setSize(unitType.size, unitType.size)
        registerAnim(State.ATTACK, unitType.attackResId ?: unitType.idleResId, unitType.attackFps)
        registerAnim(State.SKILL,  unitType.skillResId  ?: unitType.idleResId, unitType.skillFps)
        registerAnim(State.DIE,    unitType.dieResId    ?: unitType.idleResId, unitType.dieFps)
    }

    // ───── 업데이트 ──────────────────────────────────────────────────────────
    override fun update(gctx: GameContext) {
        // DIE 애니메이션이 끝나면 콜백을 호출해 world 에서 제거하게 한다.
        if (dyingStarted) {
            if (isDieAnimFinished()) onDied?.invoke()
            return
        }

        sp = (sp + Balance.Unit.spChargeRate * gctx.frameTime).coerceAtMost(Balance.Unit.maxSp)

        // 스킬 애니메이션 재생 중이면 공격 시도 안 함.
        // 공격 애니메이션 중에는 공격 가능 (attackCooldown 이 자연스럽게 막음).
        if (currentState == State.SKILL) return

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
        // 공격 중에는 스킬 사용 가능, 스킬 중에는 재발동 불가
        if (currentState == State.SKILL) return false
        sp = 0f
        onSkillActivated(gctx)
        playAnim(State.SKILL, durationMs = 1500L)
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
        if (dyingStarted) return
        val actual = Balance.DamageCalc.physical(amount, unitType.defense)
        hp = (hp - actual).coerceAtLeast(0f)
        if (hp <= 0f) startDying()
    }

    fun heal(amount: Float) {
        if (dyingStarted) return
        hp = (hp + amount).coerceAtMost(unitType.maxHp)
    }

    fun isDead() = hp <= 0f

    private fun startDying() {
        if (dyingStarted) return
        dyingStarted = true
        // dieFps 와 프레임 수로 애니메이션 총 재생 시간을 계산한다.
        // frameCount 는 playAnim 이후 setter 에서 자동 계산되므로,
        // 여기서는 일반적인 사망 애니메이션 길이(1초)를 durationMs 로 사용한다.
        // 에셋 프레임 수에 맞게 조절하려면 (frameCount / dieFps * 1000).toLong() 으로 바꾸면 된다.
        val dieDurMs = (1000f / unitType.dieFps * 8).toLong()  // 약 8프레임 분량
        playAnim(State.DIE, durationMs = dieDurMs)
    }

    fun placeAt(cx: Float, cy: Float) = setCenter(cx, cy)

    // ───── 드로우 ────────────────────────────────────────────────────────────
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        // 사망 중에는 체력바·SP바 숨김
        if (dyingStarted) return
        val barW    = Balance.Unit.hpBarWidth
        val barLeft = x - barW / 2f
        hpGauge.draw(canvas, barLeft, y + height / 2f + 2f, barW, hp / unitType.maxHp)
        val spY = y + height / 2f + 14f
        canvas.drawRoundRect(barLeft, spY, barLeft + barW, spY + 6f, 3f, 3f, spBgPaint)
        val spFill = barW * (sp / Balance.Unit.maxSp)
        if (spFill > 0f) {
            val paint = if (skillReady) spReadyPaint else spFillPaint
            canvas.drawRoundRect(barLeft, spY, barLeft + spFill, spY + 6f, 3f, 3f, paint)
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