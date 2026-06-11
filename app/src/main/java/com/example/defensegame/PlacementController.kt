package com.example.defensegame

import a2dg.objects.IGameObject
import a2dg.scene.World
import a2dg.view.GameContext
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import androidx.core.graphics.toColorInt

// 명일방주 스타일 배치·회수·스킬·배속 컨트롤러.
// 모든 게임 내 터치 이벤트를 MainScene 으로부터 위임받아 처리한다.
class PlacementController(
    private val gctx: GameContext,
    private val world: World<MainScene.Layer>,
    private val tileGrid: TileGrid,
    private val energy: EnergySystem,
    private val cellW: Float,
    private val cellH: Float,
) : IGameObject {

    // ── 배치 드래그 ───────────────────────────────────────────────────────────
    private var selected: PlayerUnit.Type? = null
    private var isDragging = false
    private var dragX = 0f; private var dragY = 0f
    private var hoverCol = -1; private var hoverRow = -1

    // ── 배치된 유닛 추적 ──────────────────────────────────────────────────────
    private val deployedUnits = mutableMapOf<Pair<Int, Int>, PlayerUnit>()

    // ── 재배치 쿨다운 (타입 → 남은 초) ──────────────────────────────────────
    private val cooldowns = mutableMapOf<PlayerUnit.Type, Float>()

    // ── 회수/스킬 팝업 ────────────────────────────────────────────────────────
    private var popupUnit: PlayerUnit? = null
    private var popupKey: Pair<Int, Int>? = null
    private val popupRect   = RectF()
    private val skillBtnR   = RectF()
    private val retreatBtnR = RectF()

    // ── 아이콘 비트맵 ─────────────────────────────────────────────────────────
    private val icons: List<Bitmap> by lazy {
        PlayerUnit.Type.entries.map { t ->
            val s = gctx.res.getBitmap(t.idleResId)
            Bitmap.createBitmap(s, 0, 0, s.height, s.height)
        }
    }

    // ───── 터치 이벤트 ────────────────────────────────────────────────────────
    fun onTouchEvent(event: MotionEvent): Boolean {
        val pt = gctx.metrics.fromScreen(event.x, event.y)
        val gx = pt.x; val gy = pt.y

        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN   -> handleDown(gx, gy)
            MotionEvent.ACTION_MOVE   -> { if (isDragging) { updateDrag(gx, gy); true } else false }
            MotionEvent.ACTION_UP     -> handleUp(gx, gy)
            MotionEvent.ACTION_CANCEL -> { cancelDrag(); false }
            else -> false
        }
    }

    private fun handleDown(gx: Float, gy: Float): Boolean {
        // 1. 배속 버튼
        if (SPEED_BTN.contains(gx, gy)) {
            GameSpeed.toggle(); return true
        }
        // 2. 팝업 버튼 (이미 팝업이 열린 경우)
        if (popupUnit != null) {
            when {
                skillBtnR.contains(gx, gy)   -> { popupUnit!!.activateSkill(gctx); dismissPopup(); return true }
                retreatBtnR.contains(gx, gy) -> { doRetreat(); return true }
                !popupRect.contains(gx, gy)  -> { dismissPopup() }
            }
        }
        // 3. 배치된 유닛 탭 → 팝업
        if (gy < BAR_TOP) {
            val col = (gx / cellW).toInt(); val row = (gy / cellH).toInt()
            val key = col to row
            val unit = deployedUnits[key]
            if (unit != null) { showPopup(unit, key, gx, gy); return true }
        }
        // 4. 유닛 바에서 드래그 시작
        val type = hitTestBar(gx, gy)
        if (type != null && energy.canAfford(type.energyCost) && !isCoolingDown(type)) {
            selected = type; isDragging = true; updateDrag(gx, gy); return true
        }
        return false
    }

    private fun handleUp(gx: Float, gy: Float): Boolean {
        if (!isDragging) return false
        updateDrag(gx, gy)
        tryPlace()
        cancelDrag()
        return true
    }

    // ── 팝업 ─────────────────────────────────────────────────────────────────
    private fun showPopup(unit: PlayerUnit, key: Pair<Int, Int>, gx: Float, gy: Float) {
        popupUnit = unit; popupKey = key
        val px = unit.x; val py = unit.y - unit.unitType.size / 2f - 90f
        popupRect.set(px - 110f, py - 10f, px + 110f, py + 80f)
        skillBtnR.set(px - 100f, py, px,         py + 66f)
        retreatBtnR.set(px,      py, px + 100f,  py + 66f)
    }

    private fun dismissPopup() { popupUnit = null; popupKey = null }

    private fun doRetreat() {
        val unit = popupUnit ?: return
        val key  = popupKey  ?: return
        removeUnit(unit, key)
        energy.add(unit.unitType.energyCost * Balance.Unit.retreatRefundRatio)
        cooldowns[unit.unitType] = unit.unitType.retreatCooldown
        dismissPopup()
    }

    // ── 유닛 제거 공통 처리 ───────────────────────────────────────────────────
    // 회수(doRetreat)와 사망(onDied 콜백) 양쪽에서 호출한다.
    private fun removeUnit(unit: PlayerUnit, key: Pair<Int, Int>) {
        world.remove(unit, MainScene.Layer.UNIT)
        tileGrid.vacate(key.first, key.second)
        deployedUnits.remove(key)
        // 팝업이 이 유닛을 가리키고 있었다면 닫는다.
        if (popupUnit === unit) dismissPopup()
    }

    // ── 배치 ─────────────────────────────────────────────────────────────────
    private fun hitTestBar(gx: Float, gy: Float): PlayerUnit.Type? {
        if (gy < BAR_TOP) return null
        return PlayerUnit.Type.entries.getOrNull((gx / SLOT_W).toInt())
    }

    private fun isCoolingDown(type: PlayerUnit.Type) =
        (cooldowns[type] ?: 0f) > 0f

    private fun updateDrag(gx: Float, gy: Float) {
        dragX = gx; dragY = gy
        if (gy >= BAR_TOP) { hoverCol = -1; hoverRow = -1; return }
        hoverCol = (gx / cellW).toInt().coerceIn(0, tileGrid.cols - 1)
        hoverRow = (gy / cellH).toInt().coerceIn(0, tileGrid.rows - 1)
    }

    private fun tryPlace() {
        val type = selected ?: return
        if (hoverCol < 0 || !tileGrid.isPlaceableFor(hoverCol, hoverRow, type.placementType)) return
        if (!energy.spend(type.energyCost)) return
        val cx = hoverCol * cellW + cellW / 2f
        val cy = hoverRow * cellH + cellH / 2f
        val key = hoverCol to hoverRow
        val unit = PlayerUnit.create(gctx, type).apply {
            placeAt(cx, cy)
            // 사망 시 자동으로 world 제거 + 타일 반환 + 쿨다운 시작
            onDied = {
                removeUnit(this, key)
                cooldowns[unitType] = unitType.retreatCooldown
            }
        }
        world.add(unit, MainScene.Layer.UNIT)
        tileGrid.occupy(hoverCol, hoverRow)
        deployedUnits[key] = unit
    }

    private fun cancelDrag() {
        isDragging = false; selected = null
        hoverCol = -1; hoverRow = -1
    }

    // ───── 업데이트 ──────────────────────────────────────────────────────────
    override fun update(gctx: GameContext) {
        for (type in cooldowns.keys.toList()) {
            val remaining = (cooldowns[type] ?: 0f) - gctx.frameTime
            if (remaining <= 0f) cooldowns.remove(type)
            else cooldowns[type] = remaining
        }
    }

    // ───── 드로우 ────────────────────────────────────────────────────────────
    override fun draw(canvas: Canvas) {
        drawUnitBar(canvas)
        drawSpeedButton(canvas)
        if (isDragging) { drawTileHighlights(canvas); drawDraggingIcon(canvas) }
        drawPopup(canvas)
    }

    // ── 하단 유닛 바 ─────────────────────────────────────────────────────────
    private fun drawUnitBar(canvas: Canvas) {
        canvas.drawRect(0f, BAR_TOP, 1600f, 900f, barBgPaint)
        canvas.drawLine(0f, BAR_TOP, 1600f, BAR_TOP, dividerPaint)

        PlayerUnit.Type.entries.forEachIndexed { i, type ->
            val cx = SLOT_W * i + SLOT_W / 2f
            val cd = cooldowns[type] ?: 0f
            val canAfford = energy.canAfford(type.energyCost)
            val available = canAfford && cd <= 0f

            val card = RectF(cx - CARD_W / 2f, BAR_TOP + 6f, cx + CARD_W / 2f, 894f)
            canvas.drawRoundRect(card, 10f, 10f, if (available) cardPaint else cardDimPaint)

            val iSize = 58f
            val iconRect = RectF(cx - iSize/2f, BAR_TOP + 10f, cx + iSize/2f, BAR_TOP + 10f + iSize)
            canvas.drawBitmap(icons[i], null, iconRect, if (available) null else dimIconPaint)

            if (cd > 0f) {
                canvas.drawRoundRect(card, 10f, 10f, cooldownOverlayPaint)
                canvas.drawText("%.0fs".format(cd), cx, BAR_TOP + 48f, cooldownTextPaint)
            }

            canvas.drawText("${type.energyCost}", cx, 888f,
                if (available) costPaint else costDimPaint)

            if (selected == type && isDragging)
                canvas.drawRoundRect(card, 10f, 10f, selectedBorderPaint)
        }
    }

    // ── 타일 하이라이트 ──────────────────────────────────────────────────────
    private fun drawTileHighlights(canvas: Canvas) {
        val type = selected ?: return
        for (r in 0 until tileGrid.rows) {
            for (c in 0 until tileGrid.cols) {
                val canPlace = tileGrid.isPlaceableFor(c, r, type.placementType)
                val isHover  = c == hoverCol && r == hoverRow
                if (!canPlace && !isHover) continue
                val rect = RectF(c*cellW, r*cellH, (c+1)*cellW, (r+1)*cellH)
                canvas.drawRect(rect, when {
                    isHover && canPlace  -> hoverValidPaint
                    isHover && !canPlace -> hoverInvalidPaint
                    else                 -> validTilePaint
                })
            }
        }
    }

    // ── 드래그 중 아이콘 ─────────────────────────────────────────────────────
    private fun drawDraggingIcon(canvas: Canvas) {
        val type = selected ?: return
        val s = type.size
        canvas.drawBitmap(icons[type.ordinal], null,
            RectF(dragX - s/2f, dragY - s - 10f, dragX + s/2f, dragY - 10f), null)
        canvas.drawText(type.name, dragX, dragY - s - 16f, dragLabelPaint)
    }

    // ── 회수·스킬 팝업 ────────────────────────────────────────────────────────
    private fun drawPopup(canvas: Canvas) {
        val unit = popupUnit ?: return
        canvas.drawRoundRect(popupRect, 12f, 12f, popupBgPaint)

        val skillPaint = if (unit.skillReady) skillReadyBtnPaint else skillDimBtnPaint
        canvas.drawRoundRect(skillBtnR, 8f, 8f, skillPaint)
        canvas.drawText("스킬", skillBtnR.centerX(), skillBtnR.centerY() + 12f, popupBtnTextPaint)
        if (unit.skillReady)
            canvas.drawRoundRect(skillBtnR, 8f, 8f, skillReadyBorderPaint)

        canvas.drawRoundRect(retreatBtnR, 8f, 8f, retreatBtnPaint)
        canvas.drawText("회수", retreatBtnR.centerX(), retreatBtnR.centerY() + 12f, popupBtnTextPaint)

        val spW    = popupRect.width() - 10f
        val spLeft = popupRect.left + 5f
        val spY    = popupRect.top - 18f
        canvas.drawRoundRect(spLeft, spY, spLeft + spW, spY + 10f, 5f, 5f, spBgPaint)
        val fill = spW * (unit.sp / Balance.Unit.maxSp)
        if (fill > 0f)
            canvas.drawRoundRect(spLeft, spY, spLeft + fill, spY + 10f, 5f, 5f,
                if (unit.skillReady) spReadyPaint else spFillPaint)
    }

    // ── 배속 버튼 ────────────────────────────────────────────────────────────
    private fun drawSpeedButton(canvas: Canvas) {
        val label = if (GameSpeed.scale == 1f) "1×" else "2×"
        canvas.drawRoundRect(SPEED_BTN, 10f, 10f,
            if (GameSpeed.scale == 2f) speedActivePaint else speedNormalPaint)
        canvas.drawText(label, SPEED_BTN.centerX(), SPEED_BTN.centerY() + 12f, speedTextPaint)
    }

    companion object {
        const val BAR_TOP = 812f
        const val SLOT_W  = 1600f / 5f
        const val CARD_W  = 290f

        private val SPEED_BTN = RectF(1530f, 10f, 1590f, 60f)

        private val barBgPaint          = Paint().apply { color = "#EE1A1A2E".toColorInt() }
        private val dividerPaint         = Paint().apply { color = "#FF44BBFF".toColorInt(); strokeWidth = 2f }
        private val cardPaint            = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#CC2A2A3E".toColorInt() }
        private val cardDimPaint         = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#CC151520".toColorInt() }
        private val dimIconPaint         = Paint().apply { alpha = 70 }
        private val cooldownOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#AA000000".toColorInt() }
        private val cooldownTextPaint    = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 28f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
        }
        private val selectedBorderPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = "#FF44BBFF".toColorInt(); strokeWidth = 3f
        }
        private val costPaint            = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#FFFFD700".toColorInt(); textSize = 26f; textAlign = Paint.Align.CENTER
        }
        private val costDimPaint         = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#88888888".toColorInt(); textSize = 26f; textAlign = Paint.Align.CENTER
        }
        private val validTilePaint       = Paint().apply { color = "#5500FF88".toColorInt() }
        private val hoverValidPaint      = Paint().apply { color = "#AA00FF88".toColorInt() }
        private val hoverInvalidPaint    = Paint().apply { color = "#AAFF2200".toColorInt() }
        private val dragLabelPaint       = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 24f; textAlign = Paint.Align.CENTER
        }
        private val popupBgPaint         = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#EE1A1A2E".toColorInt() }
        private val skillReadyBtnPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#FF1A3A6C".toColorInt() }
        private val skillDimBtnPaint     = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#FF111118".toColorInt() }
        private val skillReadyBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 2.5f; color = "#FFFFFFEE".toColorInt()
        }
        private val retreatBtnPaint      = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#FF4A1A1A".toColorInt() }
        private val popupBtnTextPaint    = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 28f; textAlign = Paint.Align.CENTER
        }
        private val spBgPaint            = Paint().apply { color = "#55FFFFFF".toColorInt() }
        private val spFillPaint          = Paint().apply { color = "#FFFFCC00".toColorInt() }
        private val spReadyPaint         = Paint().apply { color = "#FFFFFFEE".toColorInt() }
        private val speedNormalPaint     = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#CC1A2A3A".toColorInt() }
        private val speedActivePaint     = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#CC2A4A1A".toColorInt() }
        private val speedTextPaint       = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 26f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
        }
    }
}