package com.example.defensegame

import a2dg.objects.IGameObject
import a2dg.scene.World
import a2dg.view.GameContext
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.toColorInt

// 게임 전체 흐름을 관리한다.
// - 기지 HP: 적이 목적지에 도달하면 감소, 0이 되면 패배
// - 처치 수: 적 처치 시 증가 + 에너지 보상
// - 클리어 판정: 모든 웨이브 종료 + 남은 적 없음 → 승리
// - HUD: 기지 HP(하트), 웨이브, 처치 수, 경과 시간
class GameManager(
    private val gctx: GameContext,
    private val world: World<MainScene.Layer>,
    private val energySystem: EnergySystem,
    private val waveGen: WaveGen,
) : IGameObject {

    var baseHp = Balance.Game.baseHp
        private set
    var killCount = 0
        private set
    private var elapsedTime = 0f
    private var ended = false

    // ───── 외부 이벤트 ───────────────────────────────────────────────────────
    fun onEnemyReachedEnd() {
        if (ended) return
        baseHp--
        if (baseHp <= 0) {
            baseHp = 0
            endGame(victory = false)
        }
    }

    fun onEnemyKilled(energyReward: Int) {
        killCount++
        energySystem.add(energyReward.toFloat())
    }

    // ───── 업데이트 ──────────────────────────────────────────────────────────
    override fun update(gctx: GameContext) {
        if (ended) return
        elapsedTime += gctx.frameTime

        // 승리 조건: 모든 웨이브 종료 + 화면에 남은 적 없음
        if (waveGen.isDone && world.objectsAt(MainScene.Layer.ENEMY).isEmpty()) {
            endGame(victory = true)
        }
    }

    private fun endGame(victory: Boolean) {
        if (ended) return
        ended = true
        gctx.sceneStack.change(ResultScene(gctx, victory, elapsedTime, killCount, baseHp))
    }

    // ───── HUD 드로우 ────────────────────────────────────────────────────────
    override fun draw(canvas: Canvas) {
        drawBaseHp(canvas)
        drawWaveInfo(canvas)
    }

    // 상단 우측: 기지 HP 하트
    private fun drawBaseHp(canvas: Canvas) {
        canvas.drawRoundRect(HP_PANEL, 14f, 14f, panelPaint)
        val total = Balance.Game.baseHp
        for (i in 0 until total) {
            val filled = i < baseHp
            val hx = 1420f + i * 52f
            canvas.drawText(if (filled) "♥" else "♡", hx, 44f, if (filled) heartFillPaint else heartEmptyPaint)
        }
    }

    // 상단 중앙: 웨이브 · 처치 수 · 경과 시간
    private fun drawWaveInfo(canvas: Canvas) {
        canvas.drawRoundRect(WAVE_PANEL, 14f, 14f, panelPaint)
        val wave = "WAVE ${waveGen.currentWave} / ${Balance.Game.totalWaves}"
        val kills = "처치  $killCount"
        val time  = formatTime(elapsedTime)
        canvas.drawText(wave,  800f, 28f, wavePaint)
        canvas.drawText(kills, 680f, 52f, infoSmallPaint)
        canvas.drawText(time,  920f, 52f, infoSmallPaint)
    }

    private fun formatTime(sec: Float): String {
        val m = (sec / 60).toInt()
        val s = (sec % 60).toInt()
        return "%02d:%02d".format(m, s)
    }

    companion object {
        private val HP_PANEL   = RectF(1406f, 8f, 1592f, 62f)
        private val WAVE_PANEL = RectF(580f,  8f, 1020f, 66f)

        private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#CC000000".toColorInt()
        }
        private val heartFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FFFF4466".toColorInt(); textSize = 38f; textAlign = Paint.Align.CENTER
        }
        private val heartEmptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#66FFFFFF".toColorInt(); textSize = 38f; textAlign = Paint.Align.CENTER
        }
        private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 24f; textAlign = Paint.Align.CENTER
        }
        private val infoSmallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FFAAAAAA".toColorInt(); textSize = 20f; textAlign = Paint.Align.CENTER
        }
    }
}
