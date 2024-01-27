package it.unibo.experiment.herdexperiment

import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.Position2D
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import it.unibo.alchemist.model.protelis.AlchemistExecutionContext
import it.unibo.alchemist.protelis.properties.ProtelisDevice
import it.unibo.experiment.CameraAdapter
import it.unibo.experiment.CameraTargetAssignmentProblem
import it.unibo.experiment.toTargets
import org.protelis.lang.datatype.Field
import org.protelis.lang.datatype.Tuple
import org.protelis.lang.datatype.impl.ArrayTupleImpl
import smile.clustering.hclust
import java.util.stream.Collectors
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

class MyUtils {
    companion object {
        @JvmStatic
        fun getClusterSolver() = CameraClusterAssignmentProblemForProtelis()

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun findNonCollidingPosition(
            context: AlchemistExecutionContext<Euclidean2DPosition>,
            field: Field<*>,
            default: Tuple,
            targetPosition: Euclidean2DPosition,
            distance: Double,
        ): Tuple {
            val env = context.environmentAccess
            require(env is Physics2DEnvironment<Any>)
            val node = (context.deviceUID as ProtelisDevice<Euclidean2DPosition>).node
            val allNodes = field.stream()
                .filter { it.value == targetPosition }
                .map { (it.key as ProtelisDevice<Euclidean2DPosition>).node }
                .sorted()
                .collect(Collectors.toList())
            return if (allNodes.size <= 1) {
                default
            } else {
                val myPos = allNodes.indexOf(node)
                val plusAngle = (env.getPosition(allNodes.first()!!) - targetPosition).asAngle

                val offs = 2 * Math.PI / allNodes.size
                val targetAngle = offs * myPos + plusAngle
                (targetPosition + env.makePosition(cos(targetAngle) * distance, sin(targetAngle) * distance)).toTuple()
            }
        }
    }
}
class CameraClusterAssignmentProblemForProtelis {
    private val problem = CameraTargetAssignmentProblem.getSolver<CameraAdapter, Position<Euclidean2DPosition>>()

    /**
     * Just an adapter for protelis which works for Euclidean2DPosition only.
     * See [CameraTargetAssignmentProblem.solve]
     */
    fun solve(cameras: Field<*>, clusters: List<List<Euclidean2DPosition>>, maxCamerasPerDestination: Int, fair: Boolean): Map<String, Position<Euclidean2DPosition>> {
        return problem.solve(
            cameras.toCameras(),
            getCentroids(clusters),
            maxCamerasPerDestination,
            fair,
        ) { camera, target ->
            camera.position.distanceTo(target as Euclidean2DPosition)
        }.mapKeys { it.key.uid }
    }

    fun getClustersOfVisibleNodes(targets: Tuple, clusteringLimit: Double): List<List<Euclidean2DPosition>> {
        val nodes = targets.toTargets<Euclidean2DPosition>()
        if (nodes.isEmpty()) {
            return emptyList()
        } else if (nodes.size == 1) {
            return listOf(nodes.map { Euclidean2DPosition(it.position.x, it.position.y) })
        }
        val data = nodes.map { it.position.coordinates }.toTypedArray()
        val c = hclust(data, "ward")
        val labels = if (c.height().last() < clusteringLimit) {
            IntArray(data.size) { 0 }
        } else {
            c.partition(clusteringLimit)
        }

        val groupedData = data.zip(labels.toTypedArray()).groupBy { it.second }

        val result = mutableListOf<List<Euclidean2DPosition>>()

        groupedData.forEach { (_, pairs) ->
            result.add(pairs.map { Euclidean2DPosition(it.first[0], it.first[1]) })
        }
        return result
    }

    private fun getCentroids(clusters: List<List<Euclidean2DPosition>>): List<Position<Euclidean2DPosition>> {
        val centroids = mutableListOf<Euclidean2DPosition>()
        val MPL = 4.0
        for (cluster in clusters) {
            val c = cluster.foldRight(Euclidean2DPosition(0.0, 0.0)) { v, acc -> v + acc }
            val size = cluster.size
            centroids.add(Euclidean2DPosition(round((c[0] / size) / MPL) * MPL, round((c[1] / size) / MPL) * MPL))
        }
        return centroids
    }
}

private fun Field<*>.toCameras() = stream().map {
    CameraAdapter(
        it.key,
        it.value,
    )
}.collect(Collectors.toList())

private fun <P : Position2D<P>> Position2D<P>.toTuple(): Tuple = ArrayTupleImpl(x, y)
