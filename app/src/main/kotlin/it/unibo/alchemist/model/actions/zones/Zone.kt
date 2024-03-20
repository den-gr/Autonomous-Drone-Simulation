package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.actions.zones.shapes.ZoneShape
import it.unibo.alchemist.model.geometry.Euclidean2DShape
import it.unibo.alchemist.model.positions.Euclidean2DPosition

/**
 * An area that is visible to a node.
 * It can define the next node's movement in base of the positions of neighbor nodes inside of zone.
 */
interface Zone {
    /**
     * Shape of zone.
     */
    val zoneShape: ZoneShape<Euclidean2DShape>

    /**
     * Search for neighbors in the zone.
     * @return true if there are at least one node in zone.
     */
    fun areNodesInZone(): Boolean

    /**
     * Search for neighbors in the zone.
     * @return all nodes that are in the zone, except zone owner and irrelevant nodes (as nodes from other groups).
     */
    fun getNodesInZone(): List<Node<Any>>

    /**
     * In base of neighbours relative positions define the next movement.
     * @return vector of next movement.
     */
    fun getNextMovement(): Euclidean2DPosition
}
