package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.physics.environments.NodeMovementTracker
import kotlin.math.pow
import kotlin.math.round

class DistanceTraveled : AbstractDoubleExporter() {
    override val columnNames: List<String> = listOf("CamDist", "ObjDist")

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long,
    ): Map<String, Double> {
        return if (environment is NodeMovementTracker) {
            mapOf(
                "CamDist" to coverageRound(environment.queryCameraMovementsSinceLastQuery()),
                "ObjDist" to coverageRound(environment.queryObjectMovementsSinceLastQuery()),
            )
        } else {
            throw IllegalArgumentException("DistanceTraveled only works with environments implementing NodeMovementTracker")
        }
    }

    private fun coverageRound(num: Double): Double {
        val factor = 10.0.pow(4)
        return round(num * factor) / factor
    }
}
