package it.unibo.experiment.herdexperiment

import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.Position2D
import it.unibo.alchemist.model.VisibleNode
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import it.unibo.alchemist.model.protelis.AlchemistExecutionContext
import it.unibo.alchemist.protelis.properties.ProtelisDevice
import it.unibo.experiment.CameraAdapter
import it.unibo.experiment.CameraTargetAssignmentProblem
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
    fun solve(cameras: Field<*>, targets: Tuple, maxCamerasPerDestination: Int, fair: Boolean): Map<String, Position<Euclidean2DPosition>> =
        problem.solve(
            cameras.toCameras(),
            targets.toCentroids(),
            maxCamerasPerDestination,
            fair,
        ) { camera, target ->
            camera.position.distanceTo(target as Euclidean2DPosition)
        }.mapKeys { it.key.uid }
}

private fun Field<*>.toCameras() = stream().map {
    CameraAdapter(
        it.key,
        it.value,
    )
}.collect(Collectors.toList())

@Suppress("UNCHECKED_CAST")
fun Tuple.toAnyCluster(): List<VisibleNode<*, *>> {
    val list = toList()
    return list.apply {
        forEach {
            require(it is VisibleNode<*, *>) { "$it is expected to be VisibleNode but it is ${it::class}" }
        }
    } as List<VisibleNode<*, *>>
}

@Suppress("UNCHECKED_CAST")
inline fun <reified P : Position<P>> Tuple.toCentroids(): List<Position<P>> {
    val anyCluster = toAnyCluster()
    if (anyCluster.size < 2) {
        return toAnyCluster().map { it.position } as List<Position<P>>
    }
    val data = anyCluster.map { it.position.coordinates }.toTypedArray()
    val c = hclust(data, "ward")
//    val labels = c.partition(if(anyCluster.size > 5) 5 else anyCluster.size)
    val LIMIT = 350.0
    val labels = if (c.height().last() < LIMIT) {
        IntArray(data.size) { 0 }
    } else {
        c.partition(LIMIT)
    }

    val groupedData = data.zip(labels.toTypedArray()).groupBy { it.second }

    val result = mutableListOf<List<DoubleArray>>()

    groupedData.forEach { (_, pairs) ->
        result.add(pairs.map { it.first })
    }

    val centroids = mutableListOf<DoubleArray>()
    val MPL = 4.0
    for (cluster in result) {
        val c = cluster.foldRight(doubleArrayOf(0.0, 0.0)) { v, acc ->
            doubleArrayOf(v[0] + acc[0], v[1] + acc[1])
        }
        val size = cluster.size
        centroids.add(doubleArrayOf(round((c[0] / size) / MPL) * MPL, round((c[1] / size) / MPL) * MPL))
    }
    return centroids.map { Euclidean2DPosition(it) } as List<Position<P>>
//    return anyCluster.apply {
//        forEach {
//            require(it.position is P) { "${it.position} is expected to be ${P::class} but it is ${it.position::class}" }
//        }
//    } as List<VisibleNode<*, P>>
}

private fun <P : Position2D<P>> Position2D<P>.toTuple(): Tuple = ArrayTupleImpl(x, y)
