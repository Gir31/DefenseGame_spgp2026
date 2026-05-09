package com.example.defensegame

import a2dg.objects.IBoxCollidable
import a2dg.objects.IRecyclable
import a2dg.objects.Sprite
import a2dg.view.GameContext
import android.graphics.RectF
import kotlin.collections.remove
import kotlin.compareTo

abstract class MapObject(
    gctx: GameContext,
    resId: Int,
) : Sprite(gctx, resId), IRecyclable, IBoxCollidable {
    abstract val layer: MainScene.Layer

    override val collisionRect: RectF
        get() = dstRect

    // 생성 후 위치를 초기화하도록 한다. 이 함수는 재활용 된 뒤에도 불릴 예정이다
    fun setLeftTop(left: Float, top: Float) {
        // left/top 을 기준으로 dstRect 를 바로 옮기므로,
        // 타일 배치 시 중심점과 폭/높이를 따로 계산하지 않아도 된다.
        dstRect.offsetTo(left, top)
    }

    override fun update(gctx: GameContext) {

    }

    override fun onRecycle() {
    }
}