package it.unibo.alchemist.boundary.extractors

import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.VisibleNode
import it.unibo.experiment.toBoolean
import java.util.*
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round

/**
 * Exports the percentage of targets covered by at least 1 camera, up to [maxCamerasPerTarget].
 * For instance if [maxCamerasPerTarget] is 2 then it exports 1-coverage and 2-coverage percentages.
 * It exports NaN if there are no targets
 * A target is any [Node] containing [targetMolecule].
 * A camera is any [Node] containing [visionMolecule].
 * [visionMolecule] is expected to contain a collection of [VisibleNode].
 */
@Suppress("unused")
class CamerasKCoverage(
    private val visionMolecule: Molecule,
    private val targetMolecule: Molecule,
    private val maxCamerasPerTarget: Int,
) : AbstractDoubleExporter() {

    override val columnNames: List<String> = maxCamerasPerTarget.downTo(1).map { "$it-coverage" }

    init {
        require(maxCamerasPerTarget > 0)
    }
    private val resultWithNoTargets = columnNames.associateWith { Double.NaN }

    override fun <T> extractData(
        environment: Environment<T, *>,
        reaction: Actionable<T>?,
        time: Time,
        step: Long,
    ): Map<String, Double> {
        val env = environment as Environment<*, *>
        val nodes: List<Node<*>> = env.nodes
        val numTargets = nodes.count { it.isTarget() }

        val ris = if (numTargets <= 0) {
            resultWithNoTargets
        } else {
            nodes
                .filter { it.isCamera() }
                .flatMap { it.getVisibleTargets() } // all visible targets
                .groupingBy { it.node.id } // group by camera
                .eachCount() // count #cameras for each target
                .values
                .groupingBy { it } // group by #cameras (k coverage)
                .eachCount() // count #targets for each k-coverage
                .let { map ->
                    val k = if (map.keys.isNotEmpty()) max(map.keys.max(), maxCamerasPerTarget) else maxCamerasPerTarget
                    val a = DoubleArray(k) { map.getOrDefault(k - it, 0).toDouble() }
                        .also { values -> // add (k+n)cov to k-cov. so that 1-cov includes 2-cov and 3-cov etc..
                            (1 until k).forEach { idx -> values[idx] += values[idx - 1] }
                        }
                    a
                }
                .takeLast(maxCamerasPerTarget) // make sure to only output k values and forget k+n coverages
                .map { it / numTargets } // percentages
                .toDoubleArray()
                .mapIndexed { index, value ->
                    "${maxCamerasPerTarget - index}-coverage" to coverageRound(value)
                }.toMap()
        }
        return ris
    }

    private fun coverageRound(num: Double): Double {
        val factor = 10.0.pow(4)
        return round(num * factor) / factor
    }

    private fun Node<*>.isTarget() = contains(targetMolecule) && getConcentration(targetMolecule).toBoolean()

    private fun Node<*>.isCamera() = contains(visionMolecule)

    private fun Node<*>.getVisibleTargets() =
        with(getConcentration(visionMolecule)) {
            require(this is List<*>) { "Expected a List but got $this of type ${this?.javaClass}" }
            if (!isEmpty()) {
                get(0)?.also {
                    require(it is VisibleNode<*, *>) {
                        "Expected a List<VisibleNode> but got List<${it::class}> = $this"
                    }
                }
            }
            @Suppress("UNCHECKED_CAST")
            (this as Iterable<VisibleNode<*, *>>).filter { it.node.isTarget() }
        }
}
