package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.actions.utils.Movement
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
     * Search for nodes in node's zone
     * @return true if there are at least one node in zone
     */
    fun areNodesInZone(): Boolean

    /**
     * Search for nodes in zone with provided centroid.
     * @param position centroid of zone
     * @return true if there are at least one node in zone
     */
    fun areNodesInZone(position: Euclidean2DPosition): Boolean

    /**
     * In base of position of nodes in the zone define the next movement
     * @return the next movement of node
     */
    fun getNextMovement(): Movement

    /**
     * @param position centroid of the zone
     * @return all nodes that are in the zone, except zone owner
     */
    fun getNodesInZone(position: Euclidean2DPosition): List<Node<Any>>
}
