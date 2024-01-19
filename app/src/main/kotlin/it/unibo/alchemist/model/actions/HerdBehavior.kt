package it.unibo.alchemist.model.actions

import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.actions.utils.Direction
import it.unibo.alchemist.model.actions.utils.GeometryUtils.Companion.rotateVector
import it.unibo.alchemist.model.actions.utils.Movement
import it.unibo.alchemist.model.actions.zones.AttractionZone
import it.unibo.alchemist.model.actions.zones.NeutralZone
import it.unibo.alchemist.model.actions.zones.RearZone
import it.unibo.alchemist.model.actions.zones.StressZone
import it.unibo.alchemist.model.actions.zones.Zone
import it.unibo.alchemist.model.actions.zones.shapes.ZoneShapeFactoryImpl
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.ContinuousPhysics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import org.protelis.lang.datatype.impl.ArrayTupleImpl
import java.lang.IllegalStateException
import java.lang.Math.random
import java.lang.Math.toRadians
import java.util.ArrayList
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class HerdBehavior @JvmOverloads constructor(
    node: Node<Any>,
    private val environment: ContinuousPhysics2DEnvironment<Any>,
    private val zones_radii: ArrayList<Double>,
    private val repulsionFactor: Double,
    private val slowDownFactor: Double,
    private val speedUpFactor: Double,
    private val movementProbabilities: ArrayList<Double>,
    private val velocities: ArrayList<Double>,
    north: Boolean = true,
) : AbstractAction<Any>(node) {
    val zones: List<Zone>
    private val stressZone: StressZone
    private val neutralZone: NeutralZone
    private val attractionZone: AttractionZone
    private val rearZone: RearZone
    private val movements: Map<Direction, Movement>

    companion object {
        const val STRESS_ZONE_ELLIPSE_RATIO = 2.0
        const val ANGLE_OF_ZONE = 180.0 // degrees
        const val MAINTAIN_DIRECTION_WEIGHT = 0.95
    }

    init {
        node.properties[0].node
        val stressZoneRadius: Double = zones_radii[0]
        val neutralZoneRadius: Double = zones_radii[1]
        val attractionZoneRadius: Double = zones_radii[2]
        val rearZoneRadius: Double = zones_radii[3]
        val y = if (north) 1.0 else -1.0
        environment.setHeading(node, Euclidean2DPosition(0.0, y))
        movements = mapOf(
            Direction.LEFT to Movement(-velocities[0], 0.0, movementProbabilities[0]),
            Direction.FORWARD to Movement(0.0, velocities[1], movementProbabilities[1]),
            Direction.RIGHT to Movement(velocities[0], 0.0, movementProbabilities[2]),
        )

        val zoneShapeFactory = ZoneShapeFactoryImpl(environment.shapeFactory)
        val stressZoneShape = zoneShapeFactory.produceEllipseZoneShape(stressZoneRadius, STRESS_ZONE_ELLIPSE_RATIO)
        stressZone = StressZone(stressZoneShape, node, environment, movements, repulsionFactor)

        val neutralZoneShape = zoneShapeFactory.produceCircularSectorZoneShape(neutralZoneRadius, ANGLE_OF_ZONE)
        neutralZone = NeutralZone(neutralZoneShape, node, environment, movements)

        val attractionZoneShape = zoneShapeFactory.produceCircularSectorZoneShape(attractionZoneRadius, ANGLE_OF_ZONE)
        attractionZone = AttractionZone(attractionZoneShape, node, environment, movements, speedUpFactor)

        val rearZoneShape = zoneShapeFactory.produceCircularSectorZoneShape(rearZoneRadius, ANGLE_OF_ZONE, true)
        rearZone = RearZone(rearZoneShape, node, environment, movements, slowDownFactor)

        zones = listOf(stressZone, neutralZone, attractionZone, rearZone)
    }

    private fun getMoleculeDoubleTupleValues(moleculeName: String): List<Double> {
        return (getMoleculeValue(moleculeName) as ArrayTupleImpl).toList().map { it.toString().toDouble() }
    }

    override fun cloneAction(node: Node<Any>, reaction: Reaction<Any>) =
        HerdBehavior(
            node,
            environment,
            zones_radii,
            repulsionFactor,
            slowDownFactor,
            speedUpFactor,
            movementProbabilities,
            velocities,
        )

    override fun execute() {
        environment.moveNode(node, getNextPosition())
    }

    override fun getContext(): Context = Context.LOCAL

    private fun getAngle(position: Euclidean2DPosition): Double {
        return position.asAngle - Math.PI / 2
    }

    private fun getMoleculeValue(moleculeName: String) =
        node.contents.getValue(SimpleMolecule(moleculeName))

    private fun getNextPosition(): Euclidean2DPosition {
        alignDirection()
        for (zone in zones) {
            if (zone.areNodesInZone()) {
                var movement = zone.getNextMovement().addVelocityModifier(getNoiseModifier(), getNoiseModifier())
                if (!rearZone.areNodesInZone() && Random.nextDouble() <= 0.3) {
                    movement = movement.multiplyVelocity(2.0)
                }

                if (zone is RearZone) {
                    turning()
                }

                setMovementInConcentration(movement, zone::class.toString())

                val relativeMovement = environment.makePosition(movement.lateralVelocity, movement.forwardVelocity)

                val relativeRotatedMovement = rotateVector(relativeMovement, getAngle(environment.getHeading(node)))
                if (zone !is StressZone && stressZone.areNodesInZone(relativeRotatedMovement.plus(environment.getPosition(node)))) {
                    val randomMovement = getRandomMovement()
                    return rotateVector(environment.makePosition(randomMovement.lateralVelocity, randomMovement.forwardVelocity), getAngle(environment.getHeading(node)))
                }
                return relativeRotatedMovement
            }
        }
        if (random() < 0.5) {
            turning()
        }
        val movement = getRandomMovement()
        setMovementInConcentration(movement, "Out of Zone")
        return rotateVector(environment.makePosition(movement.lateralVelocity, movement.forwardVelocity), getAngle(environment.getHeading(node)))
    }

    private fun alignDirection() {
        val nodes = neutralZone.getNodesInZone(environment.getPosition(node))
        val ownerHeading = environment.getHeading(node)
        val avgGroupHeading = nodes.map { environment.getHeading(it) }
            .foldRight(environment.makePosition(0, 0)) { elem, acc -> acc + elem }
        val normAvgGroupHeading = if (avgGroupHeading == environment.makePosition(0, 0)) avgGroupHeading else avgGroupHeading.normalized()
        val newHeading = ownerHeading.times(MAINTAIN_DIRECTION_WEIGHT) + normAvgGroupHeading.times(1 - MAINTAIN_DIRECTION_WEIGHT)
        environment.setHeading(node, newHeading)
    }

    private fun setMovementInConcentration(movement: Movement, zoneName: String) {
        node.setConcentration(SimpleMolecule("x"), movement.lateralVelocity)
        node.setConcentration(SimpleMolecule("y"), movement.forwardVelocity)
        node.setConcentration(SimpleMolecule("zone"), zoneName)
    }

    private fun turning() {
        val position = environment.getPosition(node)
        val angleToOrigin = environment.makePosition(-position.x, -position.y)
        val angle = environment.getHeading(node).angleBetween(angleToOrigin)
        val distanceFromOrigin = hypot(position.x, position.y)
        val coef = distanceFromOrigin / 1000
        val prob = (coef * (angle / Math.PI)) / 10
        node.setConcentration(SimpleMolecule("turn prob"), prob)
        if (Random.nextDouble() < prob) {
            val headingAngle = environment.getHeading(node).asAngle + toRadians(2.0)
            environment.setHeading(node, environment.makePosition(cos(headingAngle), sin(headingAngle)))
        }
    }

    private fun getRandomMovement(): Movement {
        val randomNumber = Random.nextDouble()
        var cumulativeProbability = 0.0

        for (movement in movements.values) {
            cumulativeProbability += movement.probability
            if (randomNumber < cumulativeProbability) {
                return movement.addVelocityModifier(getNoiseModifier(), getNoiseModifier())
            }
        }
        throw IllegalStateException("The sum of movement probabilities is not equal to 1")
    }

    private fun getNoiseModifier(): Double {
        val sign = if (Random.nextBoolean()) 1 else -1
        return (Random.nextDouble() / 10.0) * sign
    }
}
