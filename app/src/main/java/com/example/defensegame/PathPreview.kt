package com.example.defensegame

import a2dg.objects.IGameObject
import a2dg.view.GameContext
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import kotlin.math.atan2
import kotlin.math.sqrt

// 명일방주 스타일 경로 미리보기.
// DRAWING → HOLDING → FADING 순서로 상태를 전환하며 자동 제거된다.
class PathPreview(
    private val waypoints: List<PointF>,
) : IGameObject {

    private enum class State { DRAWING, HOLDING, FADING, DONE }

    // ───── 경로 길이 사전 계산 ───────────────────────────────────────────────
    private val segLengths = FloatArray(maxOf(0, waypoints.size - 1))
    private val totalLength: Float

    init {
        var total = 0f
        for (i in segLengths.indices) {
            val dx = waypoints[i + 1].x - waypoints[i].x
            val dy = waypoints[i + 1].y - waypoints[i].y
            segLengths[i] = sqrt(dx * dx + dy * dy)
            total += segLengths[i]
        }
        totalLength = total
    }

    // ───── 상태 ─────────────────────────────────────────────────────────────
    private var state      = State.DRAWING
    private var drawnLen   = 0f    // 현재까지 그려진 길이 (픽셀)
    private var stateTimer = 0f    // 현재 상태에서 경과 시간
    private var alpha      = 255   // 전체 알파 (FADING 에서 감소)
    private val frontPos   = PointF()  // 선 앞 끝 위치

    // ───── 페인트 (인스턴스별 → 알파 독립 조작) ─────────────────────────────
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 30f
        color = Color.parseColor("#5500DDFF")
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 7f
        color = Color.parseColor("#FF00DDFF")
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val dotRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#FF00DDFF")
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#CC00DDFF")
    }

    // 화살표 모양 (원점 기준, 오른쪽 방향)
    private val arrowShape = Path().apply {
        moveTo(14f,  0f)   // 앞 끝
        lineTo(-8f, -7f)   // 왼쪽 아래
        lineTo(-4f,  0f)   // 뒤쪽 오목
        lineTo(-8f,  7f)   // 왼쪽 위
        close()
    }

    // ───── 업데이트 ──────────────────────────────────────────────────────────
    override fun update(gctx: GameContext) {
        stateTimer += gctx.frameTime
        when (state) {
            State.DRAWING -> {
                drawnLen = (drawnLen + DRAW_SPEED * gctx.frameTime).coerceAtMost(totalLength)
                if (drawnLen >= totalLength) {
                    state = State.HOLDING
                    stateTimer = 0f
                }
            }
            State.HOLDING -> {
                if (stateTimer >= HOLD_DURATION) {
                    state = State.FADING
                    stateTimer = 0f
                }
            }
            State.FADING -> {
                alpha = (255 * (1f - stateTimer / FADE_DURATION)).toInt().coerceAtLeast(0)
                if (stateTimer >= FADE_DURATION) {
                    state = State.DONE
                    gctx.mainWorld().remove(this, MainScene.Layer.PATH_PREVIEW)
                }
            }
            State.DONE -> {}
        }
        calcFrontPos()
    }

    // ───── 드로우 ────────────────────────────────────────────────────────────
    override fun draw(canvas: Canvas) {
        if (state == State.DONE || waypoints.size < 2) return

        // 모든 페인트에 현재 알파 적용
        glowPaint.alpha  = (alpha * 0.4f).toInt()
        linePaint.alpha  = alpha
        dotPaint.alpha   = alpha
        dotRingPaint.alpha = alpha
        arrowPaint.alpha = (alpha * 0.85f).toInt()

        drawPartialPath(canvas, glowPaint)   // 외곽 글로우
        drawPartialPath(canvas, linePaint)   // 메인 선
        drawArrows(canvas)                   // 진행 방향 화살표

        // 선 앞 끝 도트 (DRAWING 중에만)
        if (state == State.DRAWING) {
            canvas.drawCircle(frontPos.x, frontPos.y, DOT_RADIUS, dotPaint)
            canvas.drawCircle(frontPos.x, frontPos.y, DOT_RADIUS + 4f, dotRingPaint)
        }
    }

    // ───── 그리기 헬퍼 ───────────────────────────────────────────────────────
    private fun drawPartialPath(canvas: Canvas, paint: Paint) {
        var remaining = drawnLen
        for (i in segLengths.indices) {
            if (remaining <= 0f) break
            val draw = remaining.coerceAtMost(segLengths[i])
            val t = if (segLengths[i] > 0f) draw / segLengths[i] else 1f
            val x1 = waypoints[i].x;     val y1 = waypoints[i].y
            val x2 = waypoints[i + 1].x; val y2 = waypoints[i + 1].y
            canvas.drawLine(x1, y1, x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, paint)
            remaining -= draw
        }
    }

    // 각 세그먼트 중간에 방향 화살표를 그린다 (이미 그려진 구간만)
    private fun drawArrows(canvas: Canvas) {
        var cumLen = 0f
        for (i in segLengths.indices) {
            cumLen += segLengths[i]
            // 세그먼트 중간이 drawnLen 안쪽일 때만 그린다
            val midLen = cumLen - segLengths[i] / 2f
            if (midLen > drawnLen) break

            val midX = (waypoints[i].x + waypoints[i + 1].x) / 2f
            val midY = (waypoints[i].y + waypoints[i + 1].y) / 2f
            val deg  = Math.toDegrees(
                atan2((waypoints[i + 1].y - waypoints[i].y).toDouble(),
                      (waypoints[i + 1].x - waypoints[i].x).toDouble())
            ).toFloat()

            canvas.save()
            canvas.translate(midX, midY)
            canvas.rotate(deg)
            canvas.drawPath(arrowShape, arrowPaint)
            canvas.restore()
        }
    }

    // drawnLen 위치에 해당하는 월드 좌표 계산
    private fun calcFrontPos() {
        var remaining = drawnLen
        for (i in segLengths.indices) {
            if (segLengths[i] <= 0f) continue
            if (remaining <= segLengths[i]) {
                val t = remaining / segLengths[i]
                frontPos.set(
                    waypoints[i].x + (waypoints[i + 1].x - waypoints[i].x) * t,
                    waypoints[i].y + (waypoints[i + 1].y - waypoints[i].y) * t,
                )
                return
            }
            remaining -= segLengths[i]
        }
        if (waypoints.isNotEmpty()) frontPos.set(waypoints.last().x, waypoints.last().y)
    }

    // ───── 상수 ─────────────────────────────────────────────────────────────
    companion object {
        private const val DRAW_SPEED    = 900f   // px/sec
        private const val HOLD_DURATION = 1.2f   // 완전히 그려진 뒤 유지 (초)
        private const val FADE_DURATION = 0.8f   // 페이드아웃 (초)
        private const val DOT_RADIUS    = 14f
    }
}
