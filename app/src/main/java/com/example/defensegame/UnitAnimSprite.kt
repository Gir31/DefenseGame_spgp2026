package com.example.defensegame

import a2dg.objects.AnimSprite
import a2dg.res.GameResources
import a2dg.view.GameContext
import android.graphics.Bitmap
import android.graphics.Canvas

// AnimSprite 를 확장해 IDLE / ATTACK / SKILL / DIE 네 가지 애니메이션 상태를 전환한다.
//
// ── 재생 규칙 ────────────────────────────────────────────────────────────────
// • IDLE  : 무한 루프
// • ATTACK: durationMs 동안 재생 후 자동으로 IDLE 복귀
// • SKILL : durationMs 동안 재생 후 자동으로 IDLE 복귀
// • DIE   : 1회 재생 후 마지막 프레임에서 정지 (루프 없음)
//           playAnim(State.DIE) 를 호출하면 isDead 플래그가 true 가 된다.
//           외부에서 isDead 를 확인해 World 에서 제거하면 된다.
open class UnitAnimSprite(
    ctx: GameContext,
    idleResId: Int,
    idleFps: Float,
) : AnimSprite(ctx, idleResId, idleFps) {

    enum class State { IDLE, ATTACK, SKILL, DIE }

    private data class AnimData(val bitmap: Bitmap, val fps: Float, val frameCount: Int)

    private val res: GameResources = ctx.res
    private val animMap = HashMap<State, AnimData>(4)

    private var currentState = State.IDLE
    private var stateStartMs  = System.currentTimeMillis()
    private var stateDurMs    = 0L   // 0 = 무한 반복

    /** DIE 애니메이션 재생이 완료됐는지 여부 */
    var deathAnimDone = false
        private set

    init {
        // 기본 IDLE 등록 (생성자에서 이미 bitmap 이 로드된 상태)
        animMap[State.IDLE] = AnimData(bitmap, idleFps, frameCount)
    }

    // ── 애니메이션 등록 ────────────────────────────────────────────────────────
    // 호출 시점에 이미 frameCount 가 계산된 상태여야 한다.
    // 등록 직후 frameCount 를 읽어 AnimData 에 저장하기 때문에
    // registerAnim 은 반드시 init 블록 이후에 호출해야 한다.
    fun registerAnim(state: State, resId: Int, fps: Float) {
        val bmp = res.getBitmap(resId)
        // 프레임 수 계산: 스프라이트 시트에서 정사각형 프레임이 가로로 배열된 구조
        val fc = bmp.width / bmp.height
        animMap[state] = AnimData(bmp, fps, fc)
    }

    // ── 상태 전환 ─────────────────────────────────────────────────────────────
    // durationMs : ATTACK/SKILL 에서 IDLE 로 자동 복귀할 시간(ms). 0 이면 수동 복귀.
    // DIE 는 durationMs 를 무시하고 항상 1회 재생 후 정지한다.
    fun playAnim(state: State, durationMs: Long = 0L) {
        if (currentState == state) return
        val data = animMap[state] ?: return
        currentState  = state
        stateStartMs  = System.currentTimeMillis()
        stateDurMs    = durationMs
        bitmap        = data.bitmap
        fps           = data.fps
        frameCount    = 0   // setter 가 새 bitmap 크기로 frameWidth/Height 재계산
        if (state == State.DIE) deathAnimDone = false
    }

    // 다른 타입으로 재활용될 때 IDLE 비트맵을 새로 설정한다
    protected fun refreshIdleBitmap(resId: Int) {
        val bmp = res.getBitmap(resId)
        val fc  = bmp.width / bmp.height
        animMap[State.IDLE] = AnimData(bmp, animMap[State.IDLE]?.fps ?: fps, fc)
        if (currentState == State.IDLE) {
            bitmap     = bmp
            frameCount = 0
        }
    }

    /** 상태를 IDLE 로 강제 복귀 (재활용 시 호출) */
    protected fun resetToIdle() {
        val data = animMap[State.IDLE] ?: return
        currentState  = State.IDLE
        stateStartMs  = System.currentTimeMillis()
        stateDurMs    = 0L
        deathAnimDone = false
        bitmap        = data.bitmap
        fps           = data.fps
        frameCount    = 0
    }

    // ── 드로우 오버라이드 ──────────────────────────────────────────────────────
    override fun draw(canvas: Canvas) {
        val elapsed = System.currentTimeMillis() - stateStartMs

        when (currentState) {
            // ── IDLE : 무한 루프 ──────────────────────────────────────────────
            State.IDLE -> {
                val time = elapsed / 1000f
                val fi = ((time * fps).toInt()) % maxOf(frameCount, 1)
                renderFrame(canvas, fi)
            }

            // ── ATTACK / SKILL : durationMs 후 IDLE 복귀 ─────────────────────
            State.ATTACK, State.SKILL -> {
                if (stateDurMs > 0L && elapsed >= stateDurMs) {
                    playAnim(State.IDLE)
                    // IDLE 로 복귀한 직후 프레임 다시 계산
                    val time = (System.currentTimeMillis() - stateStartMs) / 1000f
                    val fi = ((time * fps).toInt()) % maxOf(frameCount, 1)
                    renderFrame(canvas, fi)
                } else {
                    val time = elapsed / 1000f
                    val fi = ((time * fps).toInt()) % maxOf(frameCount, 1)
                    renderFrame(canvas, fi)
                }
            }

            // ── DIE : 1회 재생 후 마지막 프레임 고정 ─────────────────────────
            State.DIE -> {
                val totalFrames = maxOf(frameCount, 1)
                val time = elapsed / 1000f
                val rawFi = (time * fps).toInt()
                if (rawFi >= totalFrames) {
                    // 마지막 프레임 고정
                    renderFrame(canvas, totalFrames - 1)
                    deathAnimDone = true
                } else {
                    renderFrame(canvas, rawFi)
                }
            }
        }
    }

    private fun renderFrame(canvas: Canvas, frameIndex: Int) {
        syncDstRect()
        srcRect?.set(frameIndex * frameWidth, 0, (frameIndex + 1) * frameWidth, frameHeight)
        canvas.drawBitmap(bitmap, srcRect, dstRect, null)
    }
}