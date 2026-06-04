package com.example.defensegame

import a2dg.objects.IGameObject
import a2dg.scene.World
import a2dg.view.GameContext
import android.graphics.Canvas
import android.graphics.PointF

class MapLoader(
    gctx: GameContext,
    val world: World<MainScene.Layer>,
    private val stage: Int,
) : IGameObject {

    private val rows = 10
    private val cols = 30
    private val lines = mutableListOf<String>()

    // 스타트 지점이 여러 개일 수 있으므로, 각각의 경로를 리스트로 관리한다.
    val paths: List<List<PointF>>
    // 배치 가능 타일과 점유 상태를 추적하는 그리드
    val tileGrid = TileGrid(rows, cols)

    init {
        // 타일 레지스트리를 먼저 등록한다.
        // MainScene 의 init 순서와 무관하게 MapLoader 가 생성되는 시점에 항상 준비된다.
        MapObjectCatalog.registerAll()

        loadStage(gctx, stage)
        val cellWidth  = gctx.metrics.width  / cols
        val cellHeight = gctx.metrics.height / rows
        createAllTiles(gctx, cellWidth, cellHeight)
        paths = computeAllPaths(cellWidth, cellHeight)
    }

    // ───── 맵 파일 로드 ──────────────────────────────────────────────────────
    private fun loadStage(gctx: GameContext, stage: Int) {
        val filename = "stage_%02d.txt".format(stage)
        try {
            gctx.view.context.assets.open(filename).bufferedReader().use { reader ->
                lines.clear()
                for (i in 0 until rows) {
                    val line = reader.readLine() ?: break
                    lines.add(line)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ───── 타일 오브젝트 생성 ────────────────────────────────────────────────
    private fun createAllTiles(gctx: GameContext, cellWidth: Float, cellHeight: Float) {
        for (r in 0 until rows) {
            val line = lines.getOrNull(r) ?: continue
            for (c in 0 until cols) {
                val ch   = if (c < line.length) line[c] else ' '
                val info = MapObjectRegistry.getInfo(ch) ?: continue
                world.add(TileObject(gctx, c, r, cellWidth, cellHeight, info.resId), MainScene.Layer.GRID)
                if (info.isElevatedPlaceable) tileGrid.markElevated(c, r)
                if (info.isGroundPlaceable)   tileGrid.markGround(c, r)
            }
        }
    }

    // ───── 모든 S 위치를 찾아 각각의 경로를 계산한다 ─────────────────────────
    private fun computeAllPaths(cellWidth: Float, cellHeight: Float): List<List<PointF>> {
        val starts = mutableListOf<Pair<Int, Int>>()
        var endCol = -1; var endRow = -1

        for (r in lines.indices) {
            for (c in lines[r].indices) {
                when (lines[r][c]) {
                    'S' -> starts.add(c to r)
                    'E' -> { endCol = c; endRow = r }
                }
            }
        }
        if (endCol < 0) return emptyList()

        return starts.mapNotNull { (sc, sr) ->
            bfsPath(sc, sr, endCol, endRow, cellWidth, cellHeight)
        }
    }

    // ───── BFS 로 startCol/Row → endCol/Row 경로 계산 ───────────────────────
    private fun bfsPath(
        startCol: Int, startRow: Int,
        endCol: Int,   endRow: Int,
        cellWidth: Float, cellHeight: Float,
    ): List<PointF>? {

        val walkable = setOf('R', 'S', 'E')
        val visited  = Array(rows) { BooleanArray(cols) }
        val parentC  = Array(rows) { IntArray(cols) { -1 } }
        val parentR  = Array(rows) { IntArray(cols) { -1 } }
        val qC = ArrayDeque<Int>(); val qR = ArrayDeque<Int>()

        visited[startRow][startCol] = true
        qC.add(startCol); qR.add(startRow)

        val dc = intArrayOf(1, -1, 0, 0)
        val dr = intArrayOf(0, 0, 1, -1)

        var found = false
        while (qC.isNotEmpty() && !found) {
            val c = qC.removeFirst(); val r = qR.removeFirst()
            for (d in 0..3) {
                val nc = c + dc[d]; val nr = r + dr[d]
                if (nr !in 0 until rows || nc !in 0 until cols) continue
                if (visited[nr][nc]) continue
                val ch = lines.getOrNull(nr)?.getOrNull(nc) ?: continue
                if (ch !in walkable) continue
                visited[nr][nc] = true
                parentC[nr][nc] = c; parentR[nr][nc] = r
                qC.add(nc); qR.add(nr)
                if (nc == endCol && nr == endRow) { found = true; break }
            }
        }
        if (!found) return null

        // 역추적
        val raw = mutableListOf<Pair<Int, Int>>()
        var c = endCol; var r = endRow
        while (c >= 0 && r >= 0) {
            raw.add(c to r)
            val pc = parentC[r][c]; val pr = parentR[r][c]
            c = pc; r = pr
        }
        raw.reverse()

        // 꺾이는 지점만 남기는 간소화
        val simplified = mutableListOf(raw[0])
        for (i in 1 until raw.size - 1) {
            val (pc, pr) = raw[i - 1]; val (cc, cr) = raw[i]; val (nc, nr) = raw[i + 1]
            if ((cc - pc != nc - cc) || (cr - pr != nr - cr)) simplified.add(raw[i])
        }
        simplified.add(raw.last())

        return simplified.map { (col, row) ->
            PointF(col * cellWidth + cellWidth / 2f, row * cellHeight + cellHeight / 2f)
        }
    }

    override fun update(gctx: GameContext) {}
    override fun draw(canvas: Canvas) {}
}
