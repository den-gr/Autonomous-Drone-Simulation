package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.ContinuousPhysics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition

data class NodeSnapshotEuclidean(
    val id: Int,
    val position: Euclidean2DPosition,
)

class VelocityExtractorEuclidean(private val stepInSeconds: Int) : AbstractVelocityExtractor() {
    var prev: List<NodeSnapshotEuclidean> = emptyList()

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long,
    ): Map<String, Double> {
        val ris = columnNames.associateWith { 0.0 }.toMutableMap()
        require(environment is ContinuousPhysics2DEnvironment)
        val m = prev.associate { it.id to it.position }
        val lst = environment.nodes.filter {
            it.contains(SimpleMolecule("onn"))
        }.map { NodeSnapshotEuclidean(it.id, environment.getPosition(it)) }
        lst.forEach {
            if (m.containsKey(it.id)) {
                val distance: Double = it.position.distanceTo(m[it.id]!!)
                val velocity = (distance / stepInSeconds) * 3600
                val range = getRange(velocity)
                ris[range] = (ris[range] ?: 0.0) + 1.0
            }
        }
        prev = lst
        return ris
    }
}
