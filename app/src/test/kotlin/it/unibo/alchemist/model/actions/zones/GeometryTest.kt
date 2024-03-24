package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.actions.utils.GeometryUtils.rotateVector
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import org.junit.jupiter.api.Test
import kotlin.math.* // ktlint-disable no-wildcard-imports
import kotlin.test.assertEquals

class GeometryTest {

    @Test
    fun testAngles() {
        val position = Euclidean2DPosition(0.0, -1.0)
        val heading = atan2(position.y, position.x)
        val headingPosition = Euclidean2DPosition(cos(heading), sin(heading))

        val west = Euclidean2DPosition(-0.0, 1.0)
        val updatedAngle = (west.asAngle - heading)

        val rotatedVector = rotateVector(headingPosition, updatedAngle)
        assertEquals(west.x, rotatedVector.x, StressZoneTest.EPSILON)
        assertEquals(west.y, rotatedVector.y, StressZoneTest.EPSILON)
    }

    @Test
    fun testAngleDifference() {
        val p1 = Euclidean2DPosition(0.0, 1.0)
        val p2 = Euclidean2DPosition(1.0, 1.0)
        val p3 = Euclidean2DPosition(0.0, -1.0)
        val p4 = Euclidean2DPosition(-1.0, -0.0)
        val halfPi = PI * (1.0 / 2.0)
        val minusHalfPi = -PI * (1.0 / 2.0)
        val quarterPi = PI * (1.0 / 4.0)
        assertEquals(halfPi, p1.asAngle)
        assertEquals(quarterPi, p2.asAngle)
        assertEquals(minusHalfPi, p3.asAngle)
        assertEquals(-PI, p4.asAngle)

        val diffFun = this::getAngleDifference

        assertEquals(PI, diffFun(p1.asAngle, p3.asAngle))
        assertEquals(PI, p1.angleBetween(p3))
        assertEquals(PI, p3.angleBetween(p1))
        assertEquals(PI, diffFun(p3.asAngle, p1.asAngle))
        assertEquals(quarterPi, diffFun(p1.asAngle, p2.asAngle))
        assertEquals(quarterPi, diffFun(p2.asAngle, p1.asAngle))

        assertEquals(quarterPi, p2.angleBetween(p1), 0.0001)
        assertEquals(quarterPi, p1.angleBetween(p2), 0.0001)

        assertEquals(PI - quarterPi, diffFun(p2.asAngle, p3.asAngle))
        assertEquals(PI - quarterPi, diffFun(p3.asAngle, p2.asAngle))

        assertEquals(PI - quarterPi, p2.angleBetween(p3))
        assertEquals(PI - quarterPi, p3.angleBetween(p2))

        assertEquals(halfPi, p3.angleBetween(p4))
        assertEquals(halfPi, p4.angleBetween(p3))

        assertEquals(halfPi, diffFun(p3.asAngle, p4.asAngle))
        assertEquals(halfPi, diffFun(p4.asAngle, p3.asAngle))
    }

//    private fun angleDifference(angle1: Double, angle2: Double): Double {
//        val rawDiff = angle2 - angle1
//        return (rawDiff + PI) % (2 * PI) - PI
//    }

    /**
     * Return minor angle difference in radians. The value is always > 0
     */
    private fun getAngleDifference(angle1: Double, angle2: Double): Double {
        val rawDiff = abs(angle2 - angle1)
        return if (rawDiff > PI) 2 * PI - rawDiff else rawDiff
    }
}
