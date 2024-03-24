package it.unibo.experiment

import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.Position2D
import it.unibo.alchemist.model.VisibleNode
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import it.unibo.alchemist.model.protelis.AlchemistExecutionContext
import it.unibo.alchemist.protelis.properties.ProtelisDevice
import org.protelis.lang.datatype.DeviceUID
import org.protelis.lang.datatype.Field
import org.protelis.lang.datatype.FunctionDefinition
import org.protelis.lang.datatype.Tuple
import org.protelis.lang.datatype.impl.ArrayTupleImpl
import org.protelis.lang.interpreter.util.JavaInteroperabilityUtils
import org.protelis.vm.ExecutionContext
import java.util.stream.Collectors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Utility class for Kotlin - Protelis intercommunication.
 * Protelis is dynamically typed so a lot of adapters and conversions are needed.
 * Protelis can import Java static methods.
 */
class ProtelisUtils {

    companion object {

        /**
         * The algorithm to calculate the best targets for the cameras.
         */
        @JvmStatic
        fun getLinproSolver() = CameraTargetAssignmentProblemForProtelis()

        /**
         * Get the position at [distance] centered in the field of fiew of the caller.
         */
        @Suppress("UNCHECKED_CAST") // unfortunately there is no way around it
        @JvmStatic
        fun getCenterOfFovAtDistance(context: AlchemistExecutionContext<Euclidean2DPosition>, distance: Double): Tuple {
            val env = context.environmentAccess
            require(env is Physics2DEnvironment<Any>)
            val nodee = context.deviceUID
//            require(node is Node<*>)
            nodee as ProtelisDevice<Euclidean2DPosition>
            val node = nodee.node
            val angle = env.getHeading(node).asAngle
            return (env.getPosition(node) + Euclidean2DPosition(distance * cos(angle), distance * sin(angle))).toTuple()
        }

        /**
         * Returns the closest position from the caller to [target] at the given [distance].
         */
        @JvmStatic
        fun closestPositionToTargetAtDistance(context: AlchemistExecutionContext<Euclidean2DPosition>, target: Euclidean2DPosition, distance: Double) =
            closestPositionToTargetAtDistance(
                context.environmentAccess,
                context.devicePosition,
                target,
                distance,
            ).toTuple()

        /**
         * Returns the elements in the [field] appearing for the least number of devices or an empty [Tuple] if the field is empty.
         * The [field] is supposed to contain a tuple of elements for each device.
         */
        @JvmStatic
        fun elementsWithLeastSources(field: Field<Tuple>): Tuple {
            val counts = mutableMapOf<Any, Int>()
            field.values().forEach { tuple ->
                tuple.forEach {
                    counts.merge(it, 1) { v1, v2 -> v1 + v2 }
                }
            }
            val min = if (counts.values.isEmpty()) Int.MAX_VALUE else counts.values.min()
            return counts.filterValues { it <= min }.keys.toTuple()
        }

        /**
         * Finds devices in [field] whose data makes [func] returns a value which can be cast to a boolean.
         * [func] is expected to have the following signature: fun func(data: Any): Any
         */
        @JvmStatic
        fun findDevicesByData(context: ExecutionContext, field: Field<*>, func: FunctionDefinition) =
            field.stream().filter {
                JavaInteroperabilityUtils.runProtelisFunctionWithJavaArguments(context, func, listOf(it.value)) as Boolean
            }.map { it.key }.collect(Collectors.toList()).toTuple()

        /**
         * Finds devices in [field] whose data makes [func] returns a value which can be cast to a boolean.
         * [func] is expected to have the following signature: fun func(data: Any): Boolean
         * Finally map the results with the [mapDevice] strategy which must have the following signture: fun func(device: Any): Any
         */
        @JvmStatic
        fun findDevicesByData(context: ExecutionContext, field: Field<*>, func: FunctionDefinition, mapDevice: FunctionDefinition) =
            field.stream().filter {
                JavaInteroperabilityUtils.runProtelisFunctionWithJavaArguments(context, func, listOf(it.value)) as Boolean
            }.map {
                JavaInteroperabilityUtils.runProtelisFunctionWithJavaArguments(context, mapDevice, listOf(it.value))
            }.collect(Collectors.toList()).toTuple()

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun findNonCollidingPosition(
            context: AlchemistExecutionContext<Euclidean2DPosition>,
            field: Field<*>,
            default: Tuple,
            target: VisibleNode<Any, Euclidean2DPosition>,
            distance: Double,
        ): Tuple {
            val env = context.environmentAccess
            require(env is Physics2DEnvironment<Any>)
            val nodee = context.deviceUID
            nodee as ProtelisDevice<Euclidean2DPosition>
            val node = nodee.node
            val allNodes = field.stream()
                .filter { it.value == target }
                .map { (it.key as ProtelisDevice<Euclidean2DPosition>).node }
                .sorted()
                .collect(Collectors.toList())
            return if (allNodes.size <= 1) {
                default
            } else {
                val myPos = allNodes.indexOf(node)
                val plusAngle = (env.getPosition(allNodes.first()!!) - target.position).asAngle

                val offs = 2 * Math.PI / allNodes.size
                val targetAngle = offs * myPos + plusAngle
                (target.position + env.makePosition(cos(targetAngle) * distance, sin(targetAngle) * distance)).toTuple()
            }
        }

        /**
         * Returns the data contained in the [field] for the [device], or [default] if there is none.
         */
        @JvmStatic
        fun getDataByDevice(default: Any, field: Field<*>, device: DeviceUID) =
            if (field.containsKey(device)) field[device] ?: default else default

        /**
         * See [OverlapRelationsGraph].
         */
        @JvmStatic
        fun buildOverlapRelationsGraph(
            context: AlchemistExecutionContext<Euclidean2DPosition>,
            strengthenValue: Double,
            evaporationBaseFactor: Double,
            evaporationMovementFactor: Double,
        ) =
            OverlapRelationsGraphForProtelis(
                context.deviceUID,
                strengthenValue,
                evaporationBaseFactor,
                evaporationMovementFactor,
            )

        /**
         * Creates a [Tuple] from any collection.
         */
        @JvmStatic
        fun toTuple(col: Collection<*>) = col.toTuple()

        /**
         * Creates a [Map] from any [Field].
         */
        @JvmStatic
        fun cloneFieldToMap(field: Field<*>) = field.toMap()!!

        /**
         * Check if [target#node] contains a molecule named [attribute] and its concentration can be cast to Boolean,
         * in which case it returns it, otherwise false.
         */
        @JvmStatic
        fun isTarget(target: VisibleNode<*, *>, attribute: String) =
            with(SimpleMolecule(attribute)) {
                target.node.contains(this) && target.node.getConcentration(this) as Boolean
            }

        /**
         * Field.map for protelis
         */
        @JvmStatic
        fun mapFieldValues(context: ExecutionContext, field: Field<*>, func: FunctionDefinition) =
            field.map {
                JavaInteroperabilityUtils.runProtelisFunctionWithJavaArguments(context, func, listOf(field.get(it)))
            }
    }
}

/**
 * See [CameraTargetAssignmentProblem].
 */
class CameraTargetAssignmentProblemForProtelis {
    private val problem = CameraTargetAssignmentProblem.getSolver<CameraAdapter, VisibleNode<*, Euclidean2DPosition>>()

    /**
     * Just an adapter for protelis which works for Euclidean2DPosition only.
     * See [CameraTargetAssignmentProblem.solve]
     */
    fun solve(cameras: Field<*>, targets: Tuple, maxCamerasPerDestination: Int, fair: Boolean): Map<String, VisibleNode<*, Euclidean2DPosition>> =
        problem.solve(
            cameras.toCameras(),
            targets.toTargets(),
            maxCamerasPerDestination,
            fair,
        ) { camera, target ->
            camera.position.distanceTo(target.position)
        }.mapKeys { it.key.uid }
}

/**
 * See [OverlapRelationsGraph].
 * This is just an adapter for Protelis.
 */
class OverlapRelationsGraphForProtelis(
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
    fun evaporate(): OverlapRelationsGraphForProtelis {
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
    fun update(map: Map<DeviceUID, Tuple>): OverlapRelationsGraphForProtelis {
        val myobjects = map[myUid]?.asIterable()?.map { require(it is VisibleNode<*, *>); it } ?: emptyList()
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
     * Returns devices chosen according to the Smooth strategy described in "Online Multi-object k-coverage with Mobile Smart Cameras"
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

/**
 * An adapter for protelis. It represents a camera.
 */
class CameraAdapter(
    id: Any,
    pos: Any,
) {
    val uid = id.toString()
    val position: Euclidean2DPosition = if (pos is Euclidean2DPosition) {
        pos
    } else {
        require(pos is Tuple && pos.size() >= 2)
        pos.toPosition()
    }

    override fun toString() =
        "Camera#$uid"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CameraAdapter
        if (uid != other.uid) return false
        return true
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }
}

/**
 * Creates a [Tuple] from any collection.
 */
private fun Collection<*>.toTuple(): Tuple = with(iterator()) { ArrayTupleImpl(*Array(size) { next() }) }

private fun <P : Position2D<P>> Position2D<P>.toTuple(): Tuple = ArrayTupleImpl(x, y)

private fun Field<*>.toCameras() = stream().map {
    CameraAdapter(
        it.key,
        it.value,
    )
}.collect(Collectors.toList())

@Suppress("UNCHECKED_CAST") // it is checked
fun Tuple.toAnyTargets(): List<VisibleNode<*, *>> =
    toList().apply {
        forEach {
            require(it is VisibleNode<*, *>) { "$it is expected to be VisibleNode but it is ${it::class}" }
        }
    } as List<VisibleNode<*, *>>

@Suppress("UNCHECKED_CAST") // it is checked
inline fun <reified P : Position<P>> Tuple.toTargets(): List<VisibleNode<*, P>> =
    toAnyTargets().apply {
        forEach {
            require(it.position is P) { "${it.position} is expected to be ${P::class} but it is ${it.position::class}" }
        }
    } as List<VisibleNode<*, P>>

private fun Tuple.toPosition(): Euclidean2DPosition {
    require(size() == 2)
    val x = get(0)
    val y = get(1)
    require(x is Double && y is Double)
    return Euclidean2DPosition(x, y)
}
