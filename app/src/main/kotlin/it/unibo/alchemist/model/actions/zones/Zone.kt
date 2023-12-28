package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.actions.utils.Movement
import it.unibo.alchemist.model.geometry.Euclidean2DShape

interface Zone {
    val zoneShape: ZoneShape<Euclidean2DShape>

    /**
     * Search for nodes in zone and if find nodes save them in cache
     * @return true if there are at least one node in zone
     */
    fun areNodesInZone(): Boolean

//    fun areNodesInZone(position: Euclidean2DPosition)

    /**
     * @return a list of nodes found by [areNodesInZone]
     */
    fun getNextMovement(): Movement
}
