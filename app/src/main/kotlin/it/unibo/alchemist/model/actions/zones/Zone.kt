package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.actions.utils.Movement
import it.unibo.alchemist.model.actions.zones.shapes.ZoneShape
import it.unibo.alchemist.model.geometry.Euclidean2DShape
import it.unibo.alchemist.model.positions.Euclidean2DPosition

interface Zone {
    val zoneShape: ZoneShape<Euclidean2DShape>

    /**
     * Search for nodes in zone and if find nodes save them in cache
     * @return true if there are at least one node in zone
     */
    fun areNodesInZone(): Boolean

    fun areNodesInZone(position: Euclidean2DPosition): Boolean

    /**
     * @return a list of nodes found by [areNodesInZone]
     */
    fun getNextMovement(): Movement
}
