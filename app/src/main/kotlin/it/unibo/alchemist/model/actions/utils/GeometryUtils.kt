package it.unibo.alchemist.model.actions.utils

import it.unibo.alchemist.model.positions.Euclidean2DPosition
import kotlin.math.cos
import kotlin.math.sin

class GeometryUtils {
    companion object {
        fun rotateVector(vector: Euclidean2DPosition, angle: Double): Euclidean2DPosition {
            val newX = vector.x * cos(angle) - vector.y * sin(angle)
            val newY = vector.x * sin(angle) + vector.y * cos(angle)
            return Euclidean2DPosition(newX, newY)
        }
    }
}
