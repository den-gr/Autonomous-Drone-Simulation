package it.unibo.experiment.clustering

import it.unibo.alchemist.model.VisibleNode
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import it.unibo.experiment.OverlapRelationsGraph
import org.protelis.lang.datatype.DeviceUID
import org.protelis.lang.datatype.Field
import org.protelis.lang.datatype.Tuple
import org.protelis.lang.datatype.impl.ArrayTupleImpl
import org.protelis.vm.ExecutionContext

/**
 * See [OverlapRelationsGraph].
 * This is just an adapter for Protelis.
 */
class OverlapRelationsGraphForProtelisClusters(
    private val myUid: DeviceUID,
    strengthenValue: Double,
    evaporationBaseFactor: Double,
    evaporationMovementFactor: Double,
) {

    private val graph = OverlapRelationsGraph<DeviceUID>(
        strengthenValue,
        evaporationBaseFactor,
        evaporationMovementFactor,
    )

    /**
     * See [OverlapRelationsGraph#evaporateAllLinks].
     */
    fun evaporate(): OverlapRelationsGraphForProtelisClusters {
        graph.evaporateAllLinks(true)
        return this
    }

    /**
     * Calls [OverlapRelationsGraph#strengthenLink] for each object in common with each camera contained in the [field].
     * The [field] is supposed to contain a [Tuple] of [VisibleNode] for each device.
     */
    fun update(field: Field<Tuple>) = update(field.toMap())

    /**
     * Calls [OverlapRelationsGraph#strengthenLink] for each object in common with each camera contained in the [map].
     * The [map] is supposed to contain a [Tuple] of [VisibleNode] for each device.
     */
    fun update(map: Map<DeviceUID, Tuple>): OverlapRelationsGraphForProtelisClusters {
        val myobjects = map[myUid]?.asIterable()?.map { require(it is Euclidean2DPosition); it } ?: emptyList()
        map.keys.filterNot { it == myUid }.forEach { camera ->
            map[camera]?.forEach { if (myobjects.contains(it)) graph.strengthenLink(camera) }
        }
        return this
    }

    /**
     * Returns a [Tuple] of [atMost] devices with the strongest links.
     */
    fun strongestLinks(atMost: Int) =
        graph.links.entries.sortedByDescending { it.value }.take(atMost).map { it.key }.toTuple()

    /**
     * Returns devices chosen according to the Smooth strategy described in
     * "Online Multi-object k-coverage with Mobile Smart Cameras".
     */
    fun smooth(ctx: ExecutionContext): Tuple {
        return if (graph.links.isEmpty()) {
            ArrayTupleImpl()
        } else {
            graph.links.values.max().let { max ->
                graph.links.entries.filter { entry ->
                    val odds = (1 + entry.value) / (1 + max)
                    ctx.nextRandomDouble() <= odds
                }.map { it.key }.toTuple()
            }
        }
    }

    override fun toString() = "OverlapRelationsGraph(${graph.links})"
}
