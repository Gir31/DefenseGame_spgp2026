package com.example.defensegame

// 게임 전체의 수치 균형을 한 곳에서 관리한다.
// 튜닝이 필요할 때 이 파일만 수정하면 된다.
object Balance {

    object Enemy {
        // ── 체력 ──────────────────────────────────────────────────────────────
        const val normalHp   = 80f
        const val tankerHp   = 320f
        const val swarmHp    = 25f
        const val specialHp  = 120f
        const val rangedHp   = 60f

        // ── 물리 방어 (DEF) — 피해 = max(ATK - DEF, ATK × 0.05) ──────────────
        // 일반: 평균 방어
        const val normalDef   = 30f
        // 탱커: 높은 방어력 → 물리 유닛이 거의 못 뚫음
        const val tankerDef   = 500f
        // 물량: 방어 없음 → 어떤 공격에도 쉽게 처치
        const val swarmDef    = 0f
        // 특수: 낮은 물리 방어 → 물리 공격에는 취약
        const val specialDef  = 15f
        // 원거리: 평균 방어
        const val rangedDef   = 25f

        // ── 마법 저항 (RES%) — 피해 = ATK × (1 - RES/100) ───────────────────
        // RES 는 0~100 퍼센트. 값이 클수록 마법 데미지 감소.
        const val normalRes   = 0f    // 마법 저항 없음
        const val tankerRes   = 50f   // 탱커: 높은 마법 저항 (50% 감소)
        const val swarmRes    = 0f    // 물량: 저항 없음
        const val specialRes  = 80f   // 특수: 마법에 매우 강함 → 물리 유닛 필요
        const val rangedRes   = 30f   // 원거리: 적당한 마법 저항

        // ── 이동 속도 (pixels/sec) ────────────────────────────────────────────
        const val normalSpeed  = 90f
        const val tankerSpeed  = 45f
        const val swarmSpeed   = 150f
        const val specialSpeed = 100f
        const val rangedSpeed  = 70f

        // ── 처치 시 에너지 보상 ───────────────────────────────────────────────
        const val normalEnergyReward  = 2
        const val tankerEnergyReward  = 8
        const val swarmEnergyReward   = 1
        const val specialEnergyReward = 5
        const val rangedEnergyReward  = 3

        // ── 렌더 크기 ─────────────────────────────────────────────────────────
        const val normalSize  = 58f
        const val tankerSize  = 85f
        const val swarmSize   = 38f
        const val specialSize = 62f
        const val rangedSize  = 54f
    }

    // ── 데미지 공식 (명일방주 방식) ───────────────────────────────────────────
    // 물리: max(ATK - DEF, ATK × 0.05)  → 높은 방어엔 최소 5%만 통과
    // 마법: ATK × (1 - RES / 100)       → 저항이 클수록 비율로 감소
    object DamageCalc {
        fun physical(atk: Float, def: Float): Float =
            maxOf(atk - def, atk * MIN_PHYSICAL_RATIO)

        fun magic(atk: Float, resPct: Float): Float =
            atk * (1f - (resPct / 100f).coerceIn(0f, 1f))

        private const val MIN_PHYSICAL_RATIO = 0.05f
    }

    object Unit {
        // 체력
        const val warriorHp  = 500f
        const val rogueHp    = 180f
        const val archerHp   = 280f
        const val mageHp     = 220f
        const val clericHp   = 200f

        // 물리 방어 DEF (flat) / 마법 저항 RES% (퍼센트)
        // 전사: 높은 방어력·마법 저항 (README 명시)
        const val warriorDef = 120f; const val warriorMr = 20f
        const val rogueDef   =  10f; const val rogueMr   =  0f
        const val archerDef  =  20f; const val archerMr  = 10f
        const val mageDef    =   0f; const val mageMr    = 10f
        const val clericDef  =  10f; const val clericMr  = 20f

        // 공격력
        const val warriorAtk = 80f
        const val rogueAtk   = 55f
        const val archerAtk  = 65f
        const val mageAtk    = 120f
        const val clericAtk  = 40f

        // 공격 간격 (초)
        const val warriorInterval = 1.5f
        const val rogueInterval   = 0.65f
        const val archerInterval  = 1.1f
        const val mageInterval    = 2.0f
        const val clericInterval  = 2.5f

        // 사거리 (게임 좌표)
        const val warriorRange = 90f
        const val rogueRange   = 80f
        const val archerRange  = 320f
        const val mageRange    = 290f
        const val clericRange  = 220f

        // 렌더 크기
        const val unitSize = 68f

        // ── SP (스킬 포인트) ───────────────────────────────────────────────────
        const val maxSp          = 20f  // 충전 완료 기준
        const val spChargeRate   = 1f   // 초당 자동 충전
        const val spPerAttack    = 3f   // 공격 시 추가 충전

        // ── 재배치 쿨다운 (회수 후 재배치까지 걸리는 시간, 초) ─────────────────
        const val warriorCooldown = 30f
        const val rogueCooldown   = 15f
        const val archerCooldown  = 25f
        const val mageCooldown    = 35f
        const val clericCooldown  = 28f

        // ── 회수 시 에너지 환불 비율 ────────────────────────────────────────────
        const val retreatRefundRatio = 0.5f

        // ── 배치 에너지 비용 ─────────────────────────────────────────────────────
        const val warriorCost = 10
        const val rogueCost   = 6
        const val archerCost  = 12
        const val mageCost    = 16
        const val clericCost  = 14
    }

    object Game {
        const val baseHp     = 3   // 기지 목숨 수
        const val totalWaves = 3   // 총 웨이브 수 (이후 적 생성 중단 → 클리어 판정)
    }

    object Wave {
        // 경로 미리보기가 끝난 뒤 적 생성 시작까지 대기 시간 (초)
        // PathPreview DRAW_SPEED=900, 맵 경로 약 1400px → 약 1.6초 소요
        const val spawnDelay         = 2.0f
        // 적 생성 간격 (초)
        const val spawnIntervalInit  = 2.0f
        const val spawnIntervalMin   = 0.4f
        const val spawnIntervalDecay = 0.985f   // 매 생성마다 간격 감소
        // 웨이브 주기 (초)
        const val waveInterval       = 30.0f
    }
}
