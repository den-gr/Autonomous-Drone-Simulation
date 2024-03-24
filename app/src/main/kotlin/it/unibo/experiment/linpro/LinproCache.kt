package it.unibo.experiment.linpro

import it.unibo.experiment.CameraTargetAssignmentProblem
import org.danilopianini.util.LinkedListSet

/**
 * Basic cache which takes into account only the last input.
 * Note: it does not take into consideration the cost function!
 * See [CameraTargetAssignmentProblem] for a description of the parameters.
 */
class LinproCache<S, D> {
    private var lastInput = Input<S, D>(emptySet(), emptySet(), 0, false)
    private var lastResult = emptyMap<S, D>()
    private var tid = -1L

    /**
     * Get the cached result, or compute a new one if it is not present.
     * The first parameters are those of [CameraTargetAssignmentProblem].
     * @param calculate is the function which is called to calculate the correct result, in case the cache misses
     */
    fun get(
        sources: List<S>,
        destinations: List<D>,
        maxSourcesPerDestination: Int,
        fair: Boolean,
        cost: (source: S, destination: D) -> Double,
        calculate: (
            sources: List<S>,
            destinations: List<D>,
            maxSourcesPerDestination: Int,
            fair: Boolean,
            cost: (source: S, destination: D) -> Double,
        ) -> Map<S, D>,
    ): Map<S, D> {
        if (tid == -1L) {
            tid = Thread.currentThread().threadId()
        } else if (tid != Thread.currentThread().threadId()) {
            throw IllegalAccessException("This is not thread-safe!")
        }
        val destsSet = LinkedListSet(destinations)
        val newInput = Input(sources.toSet(), destsSet, maxSourcesPerDestination, fair)
        return if (lastInput != newInput) {
            lastInput = newInput
            lastResult = calculate(sources, destinations, maxSourcesPerDestination, fair, cost)
            lastResult
        } else {
            // return updated positions
            lastResult.mapValues {
                destsSet[destsSet.indexOf(it.value)]
            }
        }
    }

    private data class Input<S, D>(
        val sources: Set<S>,
        val destinations: Set<D>,
        val maxSourcesPerDestination: Int,
        val fair: Boolean,
    )
}
