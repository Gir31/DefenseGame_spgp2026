package com.example.defensegame

import a2dg.objects.IGameObject
import a2dg.scene.World
import a2dg.view.GameContext
import android.graphics.Canvas

class MapLoader(gctx: GameContext, val world: World<MainScene.Layer>, private val stage: Int): IGameObject {
    private val rows = 10
    private val cols = 30
    private val lines = mutableListOf<String>()

    init {
        // 1. 맵 파일 읽기
        loadStage(gctx, stage)
        // 2. 읽어온 데이터를 기반으로 모든 타일 객체(TileObject) 생성
        createAllTiles(gctx)
    }

    private fun loadStage(gctx: GameContext, stage: Int) {
        val filename = "stage_%02d.txt".format(stage)
        try {
            gctx.view.context.assets.open(filename).bufferedReader().use { reader ->
                lines.clear()
                // 10줄을 읽어서 리스트에 저장
                for (i in 0 until rows) {
                    val line = reader.readLine() ?: break
                    lines.add(line)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createAllTiles(gctx: GameContext) {
        // 화면 크기를 기준으로 한 칸의 가로/세로 크기 계산
        val cellWidth = gctx.metrics.width / cols
        val cellHeight = gctx.metrics.height / rows

        for (r in 0 until rows) {
            val line = lines.getOrNull(r) ?: continue
            for (c in 0 until cols) {
                // 한 글자씩 읽기
                val tileChar = if (c < line.length) line[c] else ' '

                // 등록된 정보가 있는지 확인
                val info = MapObjectRegistry.getInfo(tileChar) ?: continue

                // TileObject 생성 (사각형 객체)
                val tileObj = TileObject(
                    c, r,
                    cellWidth, cellHeight,
                    info.color
                )

                // World에 추가 (가장 아래 레이어인 GRID 레이어에 배치)
                world.add(tileObj, MainScene.Layer.GRID)
            }
        }
    }

    // 모든 타일을 World에 이미 추가했으므로 Loader 자체는 별도로 할 일이 없습니다.
    override fun update(gctx: GameContext) {}
    override fun draw(canvas: Canvas) {}
}