package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.physics.environments.NodeMovementTracker
import kotlin.math.pow
import kotlin.math.round

/**
 * Extract from an environment that implement [NodeMovementTracker] the total distance traveled by objects and cameras.
 */
class DistanceTraveled : AbstractDoubleExporter() {
    override val columnNames: List<String> = listOf("CamDist", "ObjDist")

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long,
    ): Map<String, Double> {
        require(environment is NodeMovementTracker)
        return mapOf(
            "CamDist" to coverageRound(environment.queryCameraMovementsSinceLastQuery()),
            "ObjDist" to coverageRound(environment.queryObjectMovementsSinceLastQuery()),
        )
    }

    private fun coverageRound(num: Double): Double {
        val factor = 10.0.pow(4)
        return round(num * factor) / factor
    }
}
