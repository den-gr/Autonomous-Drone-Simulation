package it.unibo.experiment.clustering

import it.unibo.experiment.CameraAdapter
import it.unibo.experiment.CameraTargetAssignmentProblem
import org.protelis.lang.datatype.Field
import java.util.stream.Collectors

class CameraClusterAssignmentProblemForProtelis {
    private val problem = CameraTargetAssignmentProblem.getSolver<CameraAdapter, Cluster>()

    /**
     * Just an adapter for protelis which works for Euclidean2DPosition only.
     * See [CameraTargetAssignmentProblem.solve]
     */
    fun solve(
        cameras: Field<*>,
        clusters: List<Cluster>,
        maxCamerasPerDestination: Int,
        fair: Boolean,
    ): Map<String, Cluster> {
        return problem.solve(
            cameras.toCameras(),
            clusters,
            maxCamerasPerDestination,
            fair,
        ) { camera, target ->
            camera.position.distanceTo(target.centroid)
        }.mapKeys { it.key.uid }
    }

    private fun Field<*>.toCameras() = stream().map {
        CameraAdapter(
            it.key,
            it.value,
        )
    }.collect(Collectors.toList())
}
