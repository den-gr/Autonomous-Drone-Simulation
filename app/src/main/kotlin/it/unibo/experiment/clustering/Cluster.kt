package it.unibo.experiment.clustering

import it.unibo.alchemist.model.VisibleNode
import it.unibo.alchemist.model.positions.Euclidean2DPosition

/**
 * A cluster representation.
 * @param id of the cluster.
 * @param centroid of the cluster.
 * @param nodes elements of the cluster.
 */
data class Cluster(
    val id: Int,
    val centroid: Euclidean2DPosition,
    val nodes: List<VisibleNode<*, Euclidean2DPosition>>,
)
