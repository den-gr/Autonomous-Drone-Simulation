package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.actions.utils.Movement

interface Zone {
    /**
     * Search for nodes in zone and if find nodes save them in cache
     * @return true if there are at least one node in zone
     */
    fun areNodesInZone(): Boolean

    /**
     * @return a list of nodes found by [areNodesInZone]
     */
    fun getNextMovement(): Movement
}