package it.unibo.experiment.herdexperiment

import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.VisibleNode
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import it.unibo.experiment.CameraAdapter
import it.unibo.experiment.CameraTargetAssignmentProblem
import org.protelis.lang.datatype.Field
import org.protelis.lang.datatype.Tuple
import smile.clustering.hclust
import java.util.stream.Collectors

class MyUtils {
    companion object {
        @JvmStatic
        fun getClusterSolver() = CameraClusterAssignmentProblemForProtelis()
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

@Suppress("UNCHECKED_CAST") // it is checked
fun Tuple.toAnyCluster(): List<VisibleNode<*, *>> {
    val list = toList()
    return list.apply {
        forEach {
            require(it is VisibleNode<*, *>) { "$it is expected to be VisibleNode but it is ${it::class}" }
        }
    } as List<VisibleNode<*, *>>
}

@Suppress("UNCHECKED_CAST") // it is checked
inline fun <reified P : Position<P>> Tuple.toCentroids(): List<Position<P>> {
    val anyCluster = toAnyCluster()
    if (anyCluster.isEmpty()) {
        return toAnyCluster().map { it.position } as List<Position<P>>
    }
    val data = anyCluster.map { it.position.coordinates }.toTypedArray()

    val c = hclust(data, "single")
    val labels = c.partition(5)

    val groupedData = data.zip(labels.toTypedArray()).groupBy { it.second }

    val result = mutableListOf<List<DoubleArray>>()

    groupedData.forEach { (label, pairs) ->
        result.add(pairs.map { it.first })
    }

    val centroids = mutableListOf<DoubleArray>()
    for (cluster in result) {
        val c = cluster.foldRight(doubleArrayOf(0.0, 0.0)) { v, acc ->
            doubleArrayOf(v[0] + acc[0], v[1] + acc[1])
        }
        val size = cluster.size
        centroids.add(doubleArrayOf(c[0] / size, c[1] / size))
    }
    return centroids.map { Euclidean2DPosition(it) } as List<Position<P>>
//    return anyCluster.apply {
//        forEach {
//            require(it.position is P) { "${it.position} is expected to be ${P::class} but it is ${it.position::class}" }
//        }
//    } as List<VisibleNode<*, P>>
}
