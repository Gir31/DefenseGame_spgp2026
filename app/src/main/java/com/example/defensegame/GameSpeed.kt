package com.example.defensegame

// 게임 배속을 전역으로 관리한다. (1× / 2×)
// MainScene.update() 에서 gctx.frameTime 을 스케일링해 모든 게임 오브젝트에 적용된다.
object GameSpeed {
    var scale = 1f
        private set

    fun toggle() {
        scale = if (scale == 1f) 2f else 1f
    }

    fun reset() { scale = 1f }
}
