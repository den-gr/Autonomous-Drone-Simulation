package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.GeoPosition
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.maps.environments.OSMEnvironment
import it.unibo.alchemist.model.molecules.SimpleMolecule

data class NodeSnapshot(
    val id: Int,
    val position: GeoPosition,
)

class VelocityExtractorOSME(private val stepInSeconds: Int) : AbstractVelocityExtractor() {
    var previousSnapshot: List<NodeSnapshot> = emptyList()

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long,
    ): Map<String, Double> {
        val ris = columnNames.associateWith { 0.0 }.toMutableMap()
        require(environment is OSMEnvironment)
        val m = previousSnapshot.associate { it.id to it.position }
        val list = environment.nodes.filter {
            it.contains(SimpleMolecule("on"))
        }.map { NodeSnapshot(it.id, environment.getPosition(it)) }
        list.forEach {
            if (m.containsKey(it.id)) {
                val distance: Double = it.position.distanceTo(m[it.id])
                val velocity = (distance / stepInSeconds) * 3600
                val range = getRange(velocity)
                ris[range] = (ris[range] ?: 0.0) + 1.0
            }
        }
        previousSnapshot = list
        return ris
    }
}
