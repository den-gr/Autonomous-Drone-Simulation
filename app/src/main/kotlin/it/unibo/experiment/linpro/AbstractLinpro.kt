package it.unibo.experiment.linpro

import it.unibo.experiment.CameraTargetAssignmentProblem
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

/**
 * Base class for linpro implementations.
 * It uses [LinproCache] to optimize performances.
 */
abstract class AbstractLinpro<S, D> :
    CameraTargetAssignmentProblem<S, D> {
    private val cache = LinproCache<S, D>()
    override fun solve(
        sources: List<S>,
        destinations: List<D>,
        maxSourcesPerDestination: Int,
        fair: Boolean,
        cost: (source: S, destination: D) -> Double,
    ): Map<S, D> {
        return cache.get(sources, destinations, maxSourcesPerDestination, fair, cost, this::solveProblem)
    }

    private fun solveProblem(
        sources: List<S>,
        destinations: List<D>,
        maxSourcesPerDestination: Int,
        fair: Boolean,
        cost: (source: S, destination: D) -> Double,
    ): Map<S, D> {
        if (sources.isEmpty() || destinations.isEmpty() || maxSourcesPerDestination <= 0) {
            return emptyMap()
        }
        val hasFakeDestination = sources.size > maxSourcesPerDestination * destinations.size
        // Create a fake destination for the surplus sources to adhere to the maxSourcesPerDestination rule.
        val totalDestinations = if (hasFakeDestination) destinations.size + 1 else destinations.size
        /*
         * MIN z = Summation of Cij * Xij where i = source number and j = destination number
         */
        var maxCost = 0.0
        val objectiveFunctionCoefficients = DoubleArray(sources.size * totalDestinations).also {
            var i = 0
            repeat(sources.size) { src ->
                repeat(destinations.size) { dest ->
                    val thisCost = cost.invoke(sources[src], destinations[dest])
                    if (thisCost > maxCost) {
                        maxCost = thisCost
                    }
                    it[i++] = thisCost
                }
                if (hasFakeDestination) {
                    i++ // empty spot for the fake destination. It will be filled later when the maxCost is calculated
                }
            }
            if (hasFakeDestination) { // add the costs to the fake destinations for all the sources
                repeat(sources.size) { src ->
                    // the fake destination is the most expensive in order to be the last choice
                    it[src * totalDestinations + destinations.size] = maxCost * 2
                }
            }
        }
        val solution = solveLPProblem {
            setObjectiveFunction(
                objectiveFunctionCoefficients,
                LPProblemBuilder.GOAL.MINIMIZE,
            )
            repeat(sources.size) { srcIdx ->
                val coefficients = DoubleArray(sources.size * totalDestinations)
                repeat(totalDestinations) { dstIdx ->
                    coefficients[srcIdx * totalDestinations + dstIdx] = 1.0
                }
                addEqualsConstraint(coefficients, 1.0)
            }
            /*
             * Maximum and minimum amount of sources per destination. The fake destination has no such constraints.
             */
            repeat(destinations.size) { dstIdx ->
                val coefficients = DoubleArray(sources.size * totalDestinations)
                repeat(sources.size) { srcIdx ->
                    coefficients[srcIdx * totalDestinations + dstIdx] = 1.0
                }

                if (!fair) {
                    // "STANDARD" constraints
                    addSmallerThanEqualsConstraint(coefficients, maxSourcesPerDestination.toDouble())
                    val minAm = min(1.0, floor(sources.size.toDouble() / destinations.size))
                    addBiggerThanEqualsConstraint(coefficients, minAm)
                } else {
                    // "FAIR" constraints, worse k-cov performance
                    val sourcesPerDestination =
                        min(maxSourcesPerDestination.toDouble(), sources.size.toDouble() / destinations.size)
                    if (sources.size.toDouble() % destinations.size == 0.0) {
                        // if we can save constraints we do so
                        addEqualsConstraint(coefficients, sourcesPerDestination)
                    } else {
                        addBiggerThanEqualsConstraint(coefficients, floor(sourcesPerDestination))
                        addSmallerThanEqualsConstraint(coefficients, ceil(sourcesPerDestination))
                    }
                }
            }
            addNonNegativityConstraint()
        }
        val sourceToDestination = mutableMapOf<S, D>()
        solution.forEachIndexed { idx, value ->
            if (value > 0.0 && idx % totalDestinations < destinations.size) { // exclude the fake destination which counts as a zero
                val source = sources[idx / totalDestinations]
                sourceToDestination[source] = destinations[idx % totalDestinations]
            }
        }
        return sourceToDestination
    }

    protected abstract fun solveLPProblem(builder: LPProblemBuilder.() -> Unit): DoubleArray

    protected interface LPProblemBuilder {
        enum class GOAL {
            MINIMIZE, MAXIMIZE
        }
        fun setObjectiveFunction(coefficients: DoubleArray, goal: GOAL)
        fun addSmallerThanEqualsConstraint(coefficients: DoubleArray, value: Double)
        fun addBiggerThanEqualsConstraint(coefficients: DoubleArray, value: Double)
        fun addEqualsConstraint(coefficients: DoubleArray, value: Double)
        fun addNonNegativityConstraint()
    }
}
