package it.unibo.alchemist.boundary.swingui.effect.impl

import it.unibo.alchemist.boundary.ui.api.Wormhole2D
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import smile.clustering.hclust
import java.awt.Graphics2D
import java.awt.Point

/**
 * Draw clusters. Perform it own clustering independently of cameras.
 * This GUI effect can be used to see how different clustering algorithm partition object in the simulation.
 */
class DrawAllClusters : DrawVisibleClusters() {

    override fun <T> draw(
        graphics: Graphics2D,
        environment: Physics2DEnvironment<T>,
        wormhole: Wormhole2D<Euclidean2DPosition>,
    ) {
        graphics.color = colorSummary

        val allVisibleNodesPositions = environment.nodes
            .filter { it.contains(SimpleMolecule("zebra")) }
            .toSet()
            .map { it as Node<T> }
            .map { environment.getPosition(it) }

        val data = allVisibleNodesPositions.map { it.coordinates }.toTypedArray()
        if (data.size < 2) {
            allVisibleNodesPositions.forEach {
                fillVisibleNodeWithColor(graphics, wormhole.getViewPoint(it), listOfColors[0])
            }
        } else {
            val doubleArrays = allVisibleNodesPositions.map { it.coordinates }.toTypedArray()
            val c = hclust(doubleArrays, "ward")
            val molecule = SimpleMolecule("ClusteringLimit")
            val clusteringLimit = environment.nodes.first { it.contains(molecule) }.getConcentration(molecule) as Double
            val labels = if (c.height().last() < clusteringLimit) {
                IntArray(doubleArrays.size) { 0 }
            } else {
                c.partition(clusteringLimit)
            }

            val groupedData = doubleArrays.zip(labels.toTypedArray()).groupBy { it.second }

            val result = mutableListOf<List<Euclidean2DPosition>>()

            groupedData.forEach { (_, pairs) ->
                result.add(pairs.map { Euclidean2DPosition(it.first[0], it.first[1]) })
            }

            result.forEachIndexed { idx, cluster ->
                cluster.forEach {
                    val viewPoint: Point = wormhole.getViewPoint(it)
                    fillVisibleNodeWithColor(graphics, viewPoint, listOfColors[idx % listOfColors.size])
                }
            }
        }
    }
}
