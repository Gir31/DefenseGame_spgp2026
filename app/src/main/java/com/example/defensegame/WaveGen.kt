package com.example.defensegame

import a2dg.objects.IGameObject
import a2dg.scene.World
import a2dg.view.GameContext
import android.graphics.Canvas
import android.graphics.PointF
import kotlin.random.Random

// WaveGen 은 일정 간격으로 적을 생성하고, 시간이 지날수록 생성 속도를 높인다.
// 웨이브 시작 직전에 PathPreview 를 모든 경로에 대해 띄워 경로를 미리 보여준다.
class WaveGen(
    private val gctx: GameContext,
    private val world: World<MainScene.Layer>,
    private val paths: List<List<PointF>>,   // 스타트 지점마다 하나씩
) : IGameObject {

    private var spawnTimer    = 0f
    private var spawnInterval = Balance.Wave.spawnIntervalInit
    private var waveTimer     = 0f
    private var wave          = 0
    private var previewShown  = false

    /** 현재 웨이브 번호 (1-based) */
    val currentWave: Int get() = wave + 1

    /** 모든 웨이브가 끝나 더 이상 적을 생성하지 않는 상태 */
    val isDone: Boolean get() = wave >= Balance.Game.totalWaves

    override fun update(gctx: GameContext) {
        if (paths.isEmpty() || isDone) return

        waveTimer += gctx.frameTime

        // 웨이브 시작 시 모든 경로에 PathPreview 표시
        if (!previewShown) {
            showPathPreviews()
            previewShown = true
        }

        // 경로 애니메이션이 끝날 때까지 적 생성 대기
        if (waveTimer < Balance.Wave.spawnDelay) return

        spawnTimer += gctx.frameTime
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f
            spawnEnemy()
            spawnInterval = (spawnInterval * Balance.Wave.spawnIntervalDecay)
                .coerceAtLeast(Balance.Wave.spawnIntervalMin)
        }

        if (waveTimer >= Balance.Wave.waveInterval) {
            waveTimer    = 0f
            previewShown = false   // 다음 웨이브 시작에 preview 다시 표시
            wave++
        }
    }

    // 모든 경로에 대해 PathPreview 를 동시에 생성한다
    private fun showPathPreviews() {
        for (path in paths) {
            world.add(PathPreview(path), MainScene.Layer.PATH_PREVIEW)
        }
    }

    private fun spawnEnemy() {
        // 스타트 지점을 랜덤하게 골라 적을 생성한다
        val path  = paths.random()
        val type  = randomType()
        val enemy = Enemy.get(gctx, type, path)
        world.add(enemy, MainScene.Layer.ENEMY)
    }

    // 웨이브가 높아질수록 강한 적이 더 자주 등장한다
    private fun randomType(): Enemy.Type {
        val roll = Random.nextInt(100)
        return when {
            wave < 1 -> if (roll < 80) Enemy.Type.NORMAL else Enemy.Type.SWARM
            wave < 2 -> when {
                roll < 50 -> Enemy.Type.NORMAL
                roll < 75 -> Enemy.Type.SWARM
                roll < 90 -> Enemy.Type.RANGED
                else      -> Enemy.Type.TANKER
            }
            else -> when {
                roll < 30 -> Enemy.Type.NORMAL
                roll < 50 -> Enemy.Type.SWARM
                roll < 65 -> Enemy.Type.RANGED
                roll < 80 -> Enemy.Type.SPECIAL
                else      -> Enemy.Type.TANKER
            }
        }
    }

    override fun draw(canvas: Canvas) {}
}
