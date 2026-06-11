package com.example.defensegame

// 게임 전체의 수치 균형을 한 곳에서 관리한다.
// 튜닝이 필요할 때 이 파일만 수정하면 된다.
//
// ── 유닛 생존 시간 설계 기준 (적 1마리 기준) ─────────────────────────────────
// Warrior(HP 600, DEF 50) vs Normal:  16초 → 3마리 동시 5초 → 여러 유닛 필수
// Warrior                 vs Tanker:  10초 → 탱커 = 즉각 위협
// Rogue  (HP 160, DEF 10) vs Normal:   2초 → 회수 안 하면 즉사 수준
// Archer (HP 260, DEF 15) vs Normal:   4초 → 후방도 위험
object Balance {

    object Enemy {
        // ── 체력 ──────────────────────────────────────────────────────────────
        const val normalHp   = 350f
        const val tankerHp   = 1800f
        const val swarmHp    = 60f
        const val specialHp  = 500f
        const val rangedHp   = 280f

        // ── 물리 방어 (DEF) ───────────────────────────────────────────────────
        const val normalDef   = 30f
        const val tankerDef   = 120f
        const val swarmDef    = 0f
        const val specialDef  = 15f
        const val rangedDef   = 25f

        // ── 마법 저항 (RES%) ──────────────────────────────────────────────────
        const val normalRes   = 0f
        const val tankerRes   = 0f
        const val swarmRes    = 0f
        const val specialRes  = 90f
        const val rangedRes   = 25f

        // ── 공격력 ────────────────────────────────────────────────────────────
        // Warrior DEF 50 기준 실질 피해:
        //   Normal  95 - 50 = 45/hit  → 1.2s 간격 → DPS 37.5 → Warrior 16초 생존
        //   Tanker 160 - 50 = 110/hit → 1.8s 간격 → DPS 61.1 → Warrior  9.8초 생존
        //   Swarm   50 - 50 = 2.5/hit(5%최솟값) → 0.7s 간격 → DPS 3.6 → 혼자선 위협 없음, 누적이 문제
        //   Special 110 - 50 = 60/hit → 1.4s 간격 → DPS 42.9 → Warrior 14초 생존
        const val normalAtk   = 95f
        const val tankerAtk   = 160f
        const val swarmAtk    = 50f
        const val specialAtk  = 110f
        const val rangedAtk   = 70f

        // ── 공격 간격 (초) ────────────────────────────────────────────────────
        const val normalInterval   = 1.2f
        const val tankerInterval   = 1.8f
        const val swarmInterval    = 0.7f
        const val specialInterval  = 1.4f
        const val rangedInterval   = 1.5f

        // ── 이동 속도 (pixels/sec) ────────────────────────────────────────────
        const val normalSpeed  = 85f
        const val tankerSpeed  = 40f
        const val swarmSpeed   = 160f
        const val specialSpeed = 95f
        const val rangedSpeed  = 65f

        // ── 처치 시 에너지 보상 ───────────────────────────────────────────────
        const val normalEnergyReward  = 2
        const val tankerEnergyReward  = 10
        const val swarmEnergyReward   = 1
        const val specialEnergyReward = 5
        const val rangedEnergyReward  = 3

        // ── 렌더 크기 ─────────────────────────────────────────────────────────
        const val normalSize  = 58f
        const val tankerSize  = 90f
        const val swarmSize   = 38f
        const val specialSize = 62f
        const val rangedSize  = 54f
    }

    // ── 데미지 공식 ───────────────────────────────────────────────────────────
    object DamageCalc {
        fun physical(atk: Float, def: Float): Float =
            maxOf(atk - def, atk * MIN_PHYSICAL_RATIO)

        fun magic(atk: Float, resPct: Float): Float =
            atk * (1f - (resPct / 100f).coerceIn(0f, 1f))

        private const val MIN_PHYSICAL_RATIO = 0.05f
    }

    object Unit {
        // ── 체력 ──────────────────────────────────────────────────────────────
        const val warriorHp  = 600f
        const val rogueHp    = 160f
        const val archerHp   = 260f
        const val mageHp     = 200f
        const val clericHp   = 220f

        // ── 물리 방어 / 마법 저항 ─────────────────────────────────────────────
        const val warriorDef = 50f;  const val warriorMr = 15f
        const val rogueDef   = 10f;  const val rogueMr   =  0f
        const val archerDef  = 15f;  const val archerMr  = 10f
        const val mageDef    =  0f;  const val mageMr    = 15f
        const val clericDef  = 15f;  const val clericMr  = 25f

        // ── 공격력 ────────────────────────────────────────────────────────────
        const val warriorAtk = 90f
        const val rogueAtk   = 55f
        const val archerAtk  = 70f
        const val mageAtk    = 130f
        const val clericAtk  = 35f

        // ── 공격 간격 (초) ────────────────────────────────────────────────────
        const val warriorInterval = 1.5f
        const val rogueInterval   = 0.65f
        const val archerInterval  = 1.1f
        const val mageInterval    = 2.0f
        const val clericInterval  = 2.5f

        // ── 사거리 ────────────────────────────────────────────────────────────
        const val warriorRange = 90f
        const val rogueRange   = 80f
        const val archerRange  = 320f
        const val mageRange    = 290f
        const val clericRange  = 240f

        // ── 렌더 크기 & UI ────────────────────────────────────────────────────
        const val unitSize   = 138f
        const val hpBarWidth = 60f

        // ── SP ────────────────────────────────────────────────────────────────
        const val maxSp        = 20f
        const val spChargeRate = 1f
        const val spPerAttack  = 3f

        // ── 재배치 쿨다운 (초) ────────────────────────────────────────────────
        const val warriorCooldown = 25f
        const val rogueCooldown   = 12f
        const val archerCooldown  = 20f
        const val mageCooldown    = 30f
        const val clericCooldown  = 22f

        // ── 회수 시 에너지 환불 비율 ────────────────────────────────────────────
        const val retreatRefundRatio = 0.5f

        // ── 배치 에너지 비용 ──────────────────────────────────────────────────
        const val warriorCost =  8
        const val rogueCost   =  5
        const val archerCost  = 10
        const val mageCost    = 14
        const val clericCost  = 12
    }

    object Game {
        const val baseHp     = 3
        const val totalWaves = 3
    }

    object Wave {
        const val spawnDelay         = 2.0f
        const val spawnIntervalInit  = 2.5f
        const val spawnIntervalMin   = 0.6f
        const val spawnIntervalDecay = 0.988f
        const val waveInterval       = 30.0f
    }
}