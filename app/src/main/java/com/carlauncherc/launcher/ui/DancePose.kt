package com.carlauncherc.launcher.ui

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Joint coordinates in a 100-unit tall local stage (y=0 is the ground). */
internal data class Joint(val x: Float, val y: Float) {
    fun mixed(other: Joint, t: Float) =
        Joint(x + (other.x - x) * t, y + (other.y - y) * t)
    fun shifted(dx: Float, dy: Float) = Joint(x + dx, y + dy)
}

internal data class DanceSkeleton(
    val head: Joint,
    val shoulder: Joint,
    val hip: Joint,
    val leftElbow: Joint,
    val leftHand: Joint,
    val rightElbow: Joint,
    val rightHand: Joint,
    val leftKnee: Joint,
    val leftFoot: Joint,
    val rightKnee: Joint,
    val rightFoot: Joint
) {
    fun mixed(other: DanceSkeleton, t: Float): DanceSkeleton {
        val x = t.coerceIn(0f, 1f)
        return DanceSkeleton(
            head.mixed(other.head, x), shoulder.mixed(other.shoulder, x),
            hip.mixed(other.hip, x), leftElbow.mixed(other.leftElbow, x),
            leftHand.mixed(other.leftHand, x), rightElbow.mixed(other.rightElbow, x),
            rightHand.mixed(other.rightHand, x), leftKnee.mixed(other.leftKnee, x),
            leftFoot.mixed(other.leftFoot, x), rightKnee.mixed(other.rightKnee, x),
            rightFoot.mixed(other.rightFoot, x)
        )
    }

    fun shifted(dx: Float, dy: Float): DanceSkeleton = DanceSkeleton(
        head.shifted(dx, dy), shoulder.shifted(dx, dy), hip.shifted(dx, dy),
        leftElbow.shifted(dx, dy), leftHand.shifted(dx, dy),
        rightElbow.shifted(dx, dy), rightHand.shifted(dx, dy),
        leftKnee.shifted(dx, dy), leftFoot.shifted(dx, dy),
        rightKnee.shifted(dx, dy), rightFoot.shifted(dx, dy)
    )
}

internal object DancePose {
    private fun base() = DanceSkeleton(
        head = Joint(0f, -82f),
        shoulder = Joint(0f, -63f),
        hip = Joint(0f, -34f),
        leftElbow = Joint(-17f, -52f),
        leftHand = Joint(-24f, -39f),
        rightElbow = Joint(17f, -52f),
        rightHand = Joint(24f, -39f),
        leftKnee = Joint(-13f, -20f),
        leftFoot = Joint(-16f, -2f),
        rightKnee = Joint(13f, -20f),
        rightFoot = Joint(16f, -2f)
    )

    /** Procedural articulated poses with different rhythms, reach and leg patterns. */
    fun dance(move: DanceMove, phase: Double): DanceSkeleton {
        val s = sin(phase).toFloat()
        val c = cos(phase).toFloat()
        val s2 = sin(phase * 0.5).toFloat()
        val beat = abs(s)
        val b = base()
        return when (move) {
            DanceMove.BOUNCE -> b.copy(
                leftElbow = Joint(-18f, -54f - 6f * beat),
                leftHand = Joint(-32f, -53f - 13f * beat),
                rightElbow = Joint(18f, -54f - 6f * beat),
                rightHand = Joint(32f, -53f - 13f * beat),
                leftKnee = Joint(-14f, -18f + beat * 5f),
                rightKnee = Joint(14f, -18f + beat * 5f)
            ).shifted(0f, -beat * 5f)
            DanceMove.SIDE_STEP -> b.copy(
                hip = Joint(s * 8f, -34f),
                shoulder = Joint(-s * 4f, -63f),
                head = Joint(-s * 5f, -82f),
                leftFoot = Joint(-18f - s * 12f, -2f - beat * 4f),
                rightFoot = Joint(18f - s * 12f, -2f),
                leftHand = Joint(-34f, -48f + s * 8f),
                rightHand = Joint(34f, -48f - s * 8f)
            )
            DanceMove.ARM_WAVE -> b.copy(
                leftElbow = Joint(-20f, -72f + c * 7f),
                leftHand = Joint(-38f, -87f + s * 15f),
                rightElbow = Joint(18f, -68f - c * 6f),
                rightHand = Joint(40f, -78f - s * 15f),
                head = Joint(s * 3f, -82f)
            )
            DanceMove.KICK -> b.copy(
                leftElbow = Joint(-24f, -58f),
                leftHand = Joint(-35f, -50f + s * 10f),
                rightElbow = Joint(24f, -58f),
                rightHand = Joint(35f, -50f - s * 10f),
                leftKnee = Joint(-13f - s * 6f, -22f - (if (s > 0) s * 9f else 0f)),
                leftFoot = Joint(-18f - (if (s > 0) s * 32f else 0f), -2f - (if (s > 0) s * 28f else 0f)),
                rightKnee = Joint(13f + s * 6f, -22f - (if (s < 0) -s * 9f else 0f)),
                rightFoot = Joint(18f + (if (s < 0) -s * 32f else 0f), -2f - (if (s < 0) -s * 28f else 0f))
            )
            DanceMove.CLAP -> b.copy(
                leftElbow = Joint(-22f, -59f),
                leftHand = Joint(-3f - abs(s) * 11f, -57f),
                rightElbow = Joint(22f, -59f),
                rightHand = Joint(3f + abs(s) * 11f, -57f),
                head = Joint(0f, -82f - beat * 3f)
            )
            DanceMove.TWIST -> b.copy(
                head = Joint(s * 7f, -82f),
                shoulder = Joint(s * 12f, -64f),
                hip = Joint(-s * 9f, -34f),
                leftElbow = Joint(-20f - s * 12f, -49f),
                leftHand = Joint(-36f - s * 10f, -45f),
                rightElbow = Joint(20f + s * 12f, -49f),
                rightHand = Joint(36f + s * 10f, -45f)
            )
            DanceMove.DISCO -> b.copy(
                shoulder = Joint(s * 5f, -63f),
                head = Joint(s * 4f, -82f),
                leftElbow = Joint(-19f, -73f - s * 6f),
                leftHand = Joint(-30f - s * 10f, -99f),
                rightElbow = Joint(20f, -52f),
                rightHand = Joint(33f + s * 9f, -31f + beat * 14f),
                leftFoot = Joint(-18f - s * 8f, -2f)
            )
            DanceMove.ROBOT -> b.copy(
                head = Joint(4f * (if (s > 0) 1f else -1f), -82f),
                leftElbow = Joint(-27f, -63f),
                leftHand = Joint(-27f, -42f + if (s > 0) -16f else 0f),
                rightElbow = Joint(27f, -63f),
                rightHand = Joint(27f, -42f + if (s < 0) -16f else 0f),
                hip = Joint(s2 * 7f, -34f),
                rightFoot = Joint(16f + s2 * 8f, -2f)
            )
            DanceMove.HOP -> b.copy(
                leftElbow = Joint(-22f, -69f),
                leftHand = Joint(-29f, -85f),
                rightElbow = Joint(22f, -69f),
                rightHand = Joint(29f, -85f),
                leftKnee = Joint(-11f, -17f),
                rightKnee = Joint(11f, -17f)
            ).shifted(s * 2f, -beat * 13f)
            DanceMove.SLIDE -> b.copy(
                head = Joint(-s * 6f, -82f),
                shoulder = Joint(-s * 6f, -63f),
                hip = Joint(s * 7f, -34f),
                leftFoot = Joint(-22f - s * 14f, -2f),
                rightFoot = Joint(22f - s * 14f, -2f),
                leftHand = Joint(-42f, -46f + s * 5f),
                rightHand = Joint(42f, -46f - s * 5f)
            )
            DanceMove.POINT -> b.copy(
                leftElbow = Joint(-28f, -64f),
                leftHand = Joint(-52f, -71f - s * 9f),
                rightElbow = Joint(22f, -53f),
                rightHand = Joint(14f, -39f),
                head = Joint(-s * 4f, -82f)
            )
            DanceMove.GROOVE -> b.copy(
                head = Joint(s * 7f, -82f - beat * 2f),
                shoulder = Joint(s * 9f, -63f),
                hip = Joint(-s * 9f, -34f),
                leftElbow = Joint(-20f, -48f + c * 5f),
                leftHand = Joint(-30f, -36f + s * 8f),
                rightElbow = Joint(20f, -48f - c * 5f),
                rightHand = Joint(30f, -36f - s * 8f),
                leftKnee = Joint(-13f + s * 8f, -19f),
                rightKnee = Joint(13f - s * 8f, -19f)
            )
        }
    }

    fun rest(pose: StickFigurePose, phase: Double): DanceSkeleton {
        val sway = sin(phase * 0.25).toFloat() * 2f
        val b = base()
        return when (pose) {
            StickFigurePose.DANCE, StickFigurePose.STAND -> b.copy(
                head = Joint(sway, -82f),
                shoulder = Joint(sway, -63f),
                leftElbow = Joint(-17f, -52f + sway),
                rightElbow = Joint(17f, -52f - sway)
            )
            StickFigurePose.SIT -> b.copy(
                head = Joint(-9f, -74f),
                shoulder = Joint(-9f, -56f),
                hip = Joint(-12f, -29f),
                leftElbow = Joint(-26f, -44f),
                leftHand = Joint(-28f, -31f),
                rightElbow = Joint(11f, -44f),
                rightHand = Joint(18f, -31f),
                leftKnee = Joint(16f, -30f),
                leftFoot = Joint(16f, -2f),
                rightKnee = Joint(31f, -29f),
                rightFoot = Joint(31f, -2f)
            )
            StickFigurePose.LIE -> b.copy(
                head = Joint(-51f, -20f),
                shoulder = Joint(-34f, -18f),
                hip = Joint(15f, -15f),
                leftElbow = Joint(-17f, -8f),
                leftHand = Joint(-5f, -3f),
                rightElbow = Joint(-18f, -30f),
                rightHand = Joint(-5f, -31f),
                leftKnee = Joint(37f, -13f),
                leftFoot = Joint(53f, -3f),
                rightKnee = Joint(39f, -17f),
                rightFoot = Joint(55f, -17f)
            )
            StickFigurePose.STRETCH -> b.copy(
                leftElbow = Joint(-16f, -80f),
                leftHand = Joint(-25f, -98f),
                rightElbow = Joint(16f, -80f),
                rightHand = Joint(25f, -98f),
                head = Joint(0f, -84f)
            )
            StickFigurePose.WAVE -> b.copy(
                rightElbow = Joint(24f, -69f),
                rightHand = Joint(34f + sin(phase * 2.0).toFloat() * 9f, -88f),
                head = Joint(sway, -82f)
            )
        }
    }
}
