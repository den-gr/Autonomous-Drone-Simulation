package it.unibo.experiment

import it.unibo.experiment.linpro.ApacheLinpro
import it.unibo.experiment.linpro.SCPLinpro

/**
 * Given a list of sources (cameras) and a list of destinations (targets), decides which camera gets which target.
 * @param <S> source type
 * @param <D> destination type
 */
interface CameraTargetAssignmentProblem<S, D> {

    /**
     * Given a list of sources and a list of destinations, decides which source gets which destination.
     * @param sources all possible sources
     * @param destinations all possible destinations
     * @param maxSourcesPerDestination maximum number of sources for each destination
     * @param cost a function calculating the cost for a source to reach the given destination.
     * @param fair if fair then it tries to assign destinations evenly among sources.
     * @return a map from sources to destinations
     */
    fun solve(
        sources: List<S>,
        destinations: List<D>,
        maxSourcesPerDestination: Int,
        fair: Boolean,
        cost: (source: S, destination: D) -> Double,
    ): Map<S, D>

    companion object {
        fun <S, D> getSolver(): CameraTargetAssignmentProblem<S, D> =
            if (SCPLinpro.isAvailable()) SCPLinpro() else ApacheLinpro()
    }
}
