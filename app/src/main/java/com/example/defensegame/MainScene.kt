package com.example.defensegame

import a2dg.scene.Scene
import a2dg.scene.World
import a2dg.view.GameContext
import android.view.MotionEvent

class MainScene(gctx: GameContext, private val stage: Int) : Scene(gctx) {

    enum class Layer {
        CONTROLLER,    // WaveGen, MapLoader 등 논리 객체
        GRID,          // 타일 배경
        PATH_PREVIEW,  // 경로 미리보기
        UNIT,          // 플레이어 유닛
        ENEMY,         // 적 유닛
        PROJECTILE,    // 투사체 & 이펙트
        HUD,           // 에너지, 기지HP, 웨이브 정보, 유닛 배치 바
    }

    override val clipsRect = true
    override val world = World(Layer.entries.toTypedArray())

    private val mapLoader     = MapLoader(gctx, world, stage)
    private val energySystem  = EnergySystem()

    // GameManager 는 외부(GameManagerExt)에서 접근한다
    val gameManager: GameManager
    private val placementController: PlacementController

    init {
        val cellW = gctx.metrics.width  / 30f
        val cellH = gctx.metrics.height / 10f

        world.add(mapLoader, Layer.CONTROLLER)

        val waveGen = WaveGen(gctx, world, mapLoader.paths)
        world.add(waveGen, Layer.CONTROLLER)

        world.add(energySystem, Layer.HUD)

        gameManager = GameManager(gctx, world, energySystem, waveGen)
        world.add(gameManager, Layer.HUD)

        placementController = PlacementController(gctx, world, mapLoader.tileGrid, energySystem, cellW, cellH)
        world.add(placementController, Layer.HUD)
    }

    // ── 배속 스케일링: frameTime 을 임시로 배율 적용해 world 를 업데이트한다 ──
    override fun update(gctx: GameContext) {
        val orig = gctx.frameTime
        gctx.frameTime = orig * GameSpeed.scale
        world.update(gctx)
        gctx.frameTime = orig
    }

    override fun onTouchEvent(event: MotionEvent): Boolean =
        placementController.onTouchEvent(event)

    // 뒤로가기 → 일시정지
    override fun onBackPressed(): Boolean {
        gctx.sceneStack.push(PauseScene(gctx))
        return true
    }

    override fun onExit() {
        GameSpeed.reset()   // 씬 전환 시 배속 초기화
    }
}
