package com.example.defensegame

// 맵 타일의 배치 가능 여부와 점유 상태를 관리한다.
// Ground   (R타일) → Warrior·Rogue 배치, 적을 막아 섬
// Elevated (B타일) → Archer·Mage·Cleric 배치, 원거리 공격
class TileGrid(val rows: Int, val cols: Int) {

    private val elevated = Array(rows) { BooleanArray(cols) }  // 고지대 (B)
    private val ground   = Array(rows) { BooleanArray(cols) }  // 길 위  (R)
    private val occupied = Array(rows) { BooleanArray(cols) }

    fun markElevated(col: Int, row: Int) { if (inBounds(col, row)) elevated[row][col] = true }
    fun markGround  (col: Int, row: Int) { if (inBounds(col, row)) ground  [row][col] = true }

    fun isElevatedFree(col: Int, row: Int) =
        inBounds(col, row) && elevated[row][col] && !occupied[row][col]

    fun isGroundFree(col: Int, row: Int) =
        inBounds(col, row) && ground[row][col] && !occupied[row][col]

    /** 선택한 유닛 타입에 맞는 타일인지 한 번에 확인 */
    fun isPlaceableFor(col: Int, row: Int, type: PlayerUnit.PlacementType) = when (type) {
        PlayerUnit.PlacementType.GROUND   -> isGroundFree(col, row)
        PlayerUnit.PlacementType.ELEVATED -> isElevatedFree(col, row)
    }

    fun occupy(col: Int, row: Int) { if (inBounds(col, row)) occupied[row][col] = true }
    fun vacate(col: Int, row: Int) { if (inBounds(col, row)) occupied[row][col] = false }

    private fun inBounds(col: Int, row: Int) = row in 0 until rows && col in 0 until cols
}
