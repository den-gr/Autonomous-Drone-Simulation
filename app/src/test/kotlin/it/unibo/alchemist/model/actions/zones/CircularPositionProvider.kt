package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.positions.Euclidean2DPosition
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class CircularPositionProvider(
    private val radius: Double,
    private val bodyLen: Double,
) {
    private val epsilon = 0.01
    private val offset = 9999.0
    private var count = 0

    fun getNextExternalPosition(): Euclidean2DPosition {
        count++
        return Euclidean2DPosition(-offset - (count * 10) - epsilon, offset + (count * bodyLen) + epsilon)
    }

    fun getNorthInZonePosition(): Euclidean2DPosition {
        return Euclidean2DPosition(0.0, radius)
    }

    fun getSouthEastInZonePosition(): Euclidean2DPosition {
        return Euclidean2DPosition(radius / 2, -radius / 2)
    }

    fun getSouthWestInZonePosition(): Euclidean2DPosition {
        return Euclidean2DPosition(-radius / 2, -radius / 2)
    }

    fun getNorthEastInZonePosition(): Euclidean2DPosition {
        return Euclidean2DPosition(radius / 2, radius / 2)
    }

    fun getNorthWestInZonePosition(): Euclidean2DPosition {
        return Euclidean2DPosition(-radius / 2, radius / 2)
    }

    fun getPointsInRadius(radius: Double, numPoints: Int = 10): List<Euclidean2DPosition> {
        val points = mutableListOf<Euclidean2DPosition>()

        for (i in 0 until numPoints) {
            val theta = 2 * PI * i / numPoints
            val x = radius * cos(theta)
            val y = radius * sin(theta)

            points.add(Euclidean2DPosition(x, y))
        }

        return points.toList()
    }

    fun generateEquidistantPointsInHalfCircle(
        radius: Double,
        numberOfPoints: Int,
        headingAngle: Double,
    ): List<Euclidean2DPosition> {
        val points = mutableListOf<Euclidean2DPosition>()

        val normalizedHeadingAngle = (headingAngle - PI / 2 + PI) % (2 * PI) - PI

        for (i in 0 until numberOfPoints) {
            val theta = normalizedHeadingAngle + PI * i / numberOfPoints
            val x = radius * cos(theta)
            val y = radius * sin(theta)

            points.add(Euclidean2DPosition(x, y))
        }

        return points
    }
}
