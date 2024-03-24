package it.unibo.experiment

import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import org.apache.commons.math3.random.RandomGenerator
import org.danilopianini.util.LinkedListSet
import org.protelis.lang.datatype.Tuple
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

fun RandomGenerator.randomAngle() = 2 * Math.PI * nextDouble()

internal inline fun <reified P : Position<P>> Any?.toPosition(env: Environment<*, P>): P = when (this) {
    is P -> this
    is Tuple -> {
        this.asIterable()
            .map<Any?, Number> {
                require(it is Number) {
                    "The Tuple must contain only Numbers but {$it::class} has been found"
                }
                it
            }.let { env.makePosition(*it.toTypedArray()) }
    }
    else ->
        throw IllegalArgumentException("Expected a Protelis Tuple or Euclidean2DPosition but got a ${this?.javaClass}")
}

internal fun offsetPositionAtDistance(
    env: Environment<*, Euclidean2DPosition>,
    source: Euclidean2DPosition,
    direction: Euclidean2DPosition,
    distance: Double,
) =
    with(direction.asAngle) {
        source + env.makePosition(cos(this) * distance, sin(this) * distance)
    }

internal fun closestPositionToTargetAtDistance(
    env: Environment<*, Euclidean2DPosition>,
    source: Euclidean2DPosition,
    target: Euclidean2DPosition,
    distance: Double,
) =
    offsetPositionAtDistance(env, target, source - target, distance)

internal fun <T> Iterable<T>.toListSet() = LinkedListSet<T>(toList())

fun Any?.toBooleanOrNull(): Boolean? =
    when (this) {
        null -> this
        is Boolean -> this
        is Int -> !equals(0)
        is Double -> compareTo(0.0).toBoolean()
        is Number -> toDouble().toBoolean()
        is String ->
            with(replaceFirstChar { it.lowercase(Locale.getDefault()) }) {
                equals("true") || equals("on") || equals("yes") || toDoubleOrNull().toBooleanOrNull() ?: false
            }
        else -> null
    }

fun Any?.toBoolean() = toBooleanOrNull() ?: throw IllegalArgumentException("Failed to convert $this to Boolean")
