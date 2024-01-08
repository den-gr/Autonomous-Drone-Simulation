package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.positions.Euclidean2DPosition
import org.junit.jupiter.api.Test
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
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

    private fun rotateVector(vector: Euclidean2DPosition, angle: Double): Euclidean2DPosition {
        val newX = vector.x * cos(angle) - vector.y * sin(angle)
        val newY = vector.x * sin(angle) + vector.y * cos(angle)
        return Euclidean2DPosition(newX, newY)
    }
}
