package com.example.defensegame

import a2dg.objects.AnimSprite
import a2dg.res.GameResources
import a2dg.view.GameContext
import android.graphics.Bitmap
import android.graphics.Canvas

// AnimSprite 를 확장해 IDLE / ATTACK / SKILL / DIE 네 가지 애니메이션 상태를 전환할 수 있게 한다.
//
// AnimSprite 는 createdOn 을 기준으로 프레임을 계산하기 때문에
// 상태 전환 후 새 애니메이션을 처음부터 재생하려면 별도의 시작 시각을 추적해야 한다.
// UnitAnimSprite 는 stateStartMs 를 관리하고 draw() 에서 이를 기준으로 프레임을 산출한다.
open class UnitAnimSprite(
    ctx: GameContext,
    idleResId: Int,
    idleFps: Float,
) : AnimSprite(ctx, idleResId, idleFps) {

    enum class State { IDLE, ATTACK, SKILL, DIE }

    private data class AnimData(val bitmap: Bitmap, val fps: Float)

    private val res: GameResources = ctx.res
    private val animMap = HashMap<State, AnimData>(4)

    // PlayerUnit 에서 currentState == State.SKILL 비교를 위해 protected 로 노출한다.
    protected var currentState = State.IDLE
        private set

    private var stateStartMs = System.currentTimeMillis()
    private var stateDurMs   = 0L   // 0 = 무한 반복

    init {
        animMap[State.IDLE] = AnimData(bitmap, idleFps)
    }

    // ── 애니메이션 등록 ────────────────────────────────────────────────────────
    fun registerAnim(state: State, resId: Int, fps: Float) {
        animMap[state] = AnimData(res.getBitmap(resId), fps)
    }

    // ── 상태 전환 ─────────────────────────────────────────────────────────────
    // durationMs: 재생 후 자동으로 IDLE 로 복귀할 시간(ms). 0 이면 수동 복귀 전까지 유지.
    fun playAnim(state: State, durationMs: Long = 0L) {
        if (currentState == state) return
        // DIE 상태는 다른 애니메이션으로 덮어쓸 수 없다.
        if (currentState == State.DIE) return
        val data = animMap[state] ?: return
        currentState = state
        stateStartMs = System.currentTimeMillis()
        stateDurMs   = durationMs
        bitmap       = data.bitmap
        fps          = data.fps
        frameCount   = 0   // setter 가 새 bitmap 크기로 frameWidth/Height 재계산
    }

    // ATTACK 또는 SKILL 애니메이션이 아직 재생 중인지 여부.
    // duration 이 지났거나 IDLE/DIE 상태이면 false 를 반환한다.
    fun isAnimating(): Boolean {
        if (currentState != State.ATTACK && currentState != State.SKILL) return false
        if (stateDurMs <= 0L) return false
        return System.currentTimeMillis() - stateStartMs < stateDurMs
    }

    // DIE 애니메이션이 끝났는지 여부. PlayerUnit 이 사망 제거 타이밍을 판단하는 데 사용한다.
    fun isDieAnimFinished(): Boolean {
        if (currentState != State.DIE) return false
        if (stateDurMs <= 0L) return false
        return System.currentTimeMillis() - stateStartMs >= stateDurMs
    }

    // 오브젝트 풀 재활용 시 상태를 IDLE 로 강제 초기화한다.
    // DIE 잠금을 포함한 모든 상태를 리셋하므로 playAnim() 우회 없이 직접 currentState 를 바꾼다.
    protected fun resetToIdle() {
        val data = animMap[State.IDLE] ?: return
        currentState = State.IDLE
        stateStartMs = System.currentTimeMillis()
        stateDurMs   = 0L
        bitmap       = data.bitmap
        fps          = data.fps
        frameCount   = 0
    }

    // 다른 타입으로 재활용될 때 IDLE 비트맵을 새로 설정한다
    protected fun refreshIdleBitmap(resId: Int) {
        val bmp = res.getBitmap(resId)
        animMap[State.IDLE] = AnimData(bmp, animMap[State.IDLE]?.fps ?: fps)
        if (currentState == State.IDLE) {
            bitmap     = bmp
            frameCount = 0
        }
    }

    // ── 드로우 오버라이드 ──────────────────────────────────────────────────────
    override fun draw(canvas: Canvas) {
        // duration 이 지나면 IDLE 로 복귀 (DIE 는 복귀하지 않음)
        if (stateDurMs > 0L
            && currentState != State.DIE
            && System.currentTimeMillis() - stateStartMs >= stateDurMs
        ) {
            playAnim(State.IDLE)
        }

        syncDstRect()
        val time = (System.currentTimeMillis() - stateStartMs) / 1000f
        val fi   = ((time * fps).toInt()) % maxOf(frameCount, 1)
        srcRect?.set(fi * frameWidth, 0, (fi + 1) * frameWidth, frameHeight)
        canvas.drawBitmap(bitmap, srcRect, dstRect, null)
    }
}