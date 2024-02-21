package it.unibo.alchemist.model.actions

import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.actions.utils.GeometryUtils.Companion.rotateVector
import it.unibo.alchemist.model.actions.utils.MovementProvider
import it.unibo.alchemist.model.actions.zones.AttractionZone
import it.unibo.alchemist.model.actions.zones.NeutralZone
import it.unibo.alchemist.model.actions.zones.RearZone
import it.unibo.alchemist.model.actions.zones.StressZone
import it.unibo.alchemist.model.actions.zones.Zone
import it.unibo.alchemist.model.actions.zones.shapes.ZoneShapeFactoryImpl
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.ContinuousPhysics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import org.apache.commons.math3.util.FastMath
import java.lang.Math.toRadians
import kotlin.collections.ArrayList
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class HerdBehavior @JvmOverloads constructor(
    node: Node<Any>,
    private val environment: ContinuousPhysics2DEnvironment<Any>,
    private val zones_radii: ArrayList<Double>,
    private val velocities: ArrayList<Double>,
    private val movementProbabilities: ArrayList<Double>,
    private val stressZoneRepulsionFactor: Double,
    private val attractionZoneSpeedUpFactor: Double,
    private val leaderSlowDown: ArrayList<Double>,
    private val trailerSpeedUp: ArrayList<Double>,
    numberOfHerds: Int = 1,
    private val radiusPreference: Int = 1000,
    seed: Int = 1,
) : AbstractAction<Any>(node) {
    val zones: List<Zone>
    private val stressZone: StressZone
    private val neutralZone: NeutralZone
    private val attractionZone: AttractionZone
    private val rearZone: RearZone
    private val movementProvider: MovementProvider

    private val trailersSpeedUpFactor: Double = trailerSpeedUp[0]
    private val trailersSpeedUpProbability: Double = trailerSpeedUp[0]

    private val turningDirection: Int
    private val additionalTurningForce: Double // degrees

    private val herdRandomizer: Random
    private val nodeRandomizer: Random

    companion object {
        const val STRESS_ZONE_ELLIPSE_RATIO = 2.0
        const val ANGLE_OF_ZONE = 180.0 // degrees
        const val MAINTAIN_DIRECTION_WEIGHT = 0.8
        const val TURNING_PROBABILITY_INSIDE_WORLD = 0.1
    }

    init {
        val nodeSeed = (node.id % numberOfHerds) + seed
        herdRandomizer = Random(nodeSeed)
        nodeRandomizer = Random(node.id + seed)
        additionalTurningForce = herdRandomizer.nextDouble(0.0, 2.0)
        val angle = herdRandomizer.nextDouble(0.0, 4 * FastMath.PI)

        turningDirection = if (nodeRandomizer.nextDouble() > 0.5) 1 else -1

        val stressZoneRadius: Double = zones_radii[0]
        val neutralZoneRadius: Double = zones_radii[1]
        val attractionZoneRadius: Double = zones_radii[2]
        val rearZoneRadius: Double = zones_radii[3]
        movementProvider = MovementProvider(
            velocities[0],
            velocities[1],
            movementProbabilities[0],
            movementProbabilities[1],
            movementProbabilities[2],
            nodeRandomizer,
        )
        environment.setHeading(node, Euclidean2DPosition(cos(angle), sin(angle)))

        val zoneShapeFactory = ZoneShapeFactoryImpl(environment.shapeFactory)
        val stressZoneShape = zoneShapeFactory.produceEllipseZoneShape(stressZoneRadius, STRESS_ZONE_ELLIPSE_RATIO)
        stressZone = StressZone(stressZoneShape, node, environment, movementProvider, stressZoneRepulsionFactor)

        val neutralZoneShape = zoneShapeFactory.produceCircularSectorZoneShape(neutralZoneRadius, ANGLE_OF_ZONE)
        neutralZone = NeutralZone(neutralZoneShape, node, environment, movementProvider, numberOfHerds)

        val attractionZoneShape = zoneShapeFactory.produceCircularSectorZoneShape(attractionZoneRadius, ANGLE_OF_ZONE)
        attractionZone = AttractionZone(attractionZoneShape, node, environment, movementProvider, attractionZoneSpeedUpFactor, numberOfHerds)

        val leaderSlowDownFactor = leaderSlowDown[0]
        val leaderSlowDownProbability = leaderSlowDown[1]
        val rearZoneShape = zoneShapeFactory.produceCircularSectorZoneShape(rearZoneRadius, ANGLE_OF_ZONE, true)
        rearZone = RearZone(rearZoneShape, node, environment, movementProvider, leaderSlowDownFactor, leaderSlowDownProbability, numberOfHerds, nodeRandomizer)

        zones = listOf(stressZone, neutralZone, attractionZone, rearZone)
    }

    override fun cloneAction(node: Node<Any>, reaction: Reaction<Any>) =
        HerdBehavior(
            node,
            environment,
            zones_radii,
            velocities,
            movementProbabilities,
            stressZoneRepulsionFactor,
            attractionZoneSpeedUpFactor,
            leaderSlowDown,
            trailerSpeedUp,
        )

    override fun execute() {
        alignDirection()
        environment.moveNode(node, getNextPosition())
    }

    override fun getContext(): Context = Context.LOCAL

    private fun getNextPosition(): Euclidean2DPosition {
        for (zone in zones) {
            if (zone.areNodesInZone()) {
                if (zone == rearZone) turning() // Leader
                var movement = zone.getNextMovement()
                if (!rearZone.areNodesInZone() && nodeRandomizer.nextDouble() <= trailersSpeedUpProbability) {
                    movement *= trailersSpeedUpFactor
                }
                setMovementInConcentration(movement, zone::class.toString())
                return rotateVector(movement, getAngle(environment.getHeading(node)))
            }
        }
        // Single trailer
        turning()
        val movement = movementProvider.getRandomMovement()
        setMovementInConcentration(movement, "Out of Zone")
        return rotateVector(movement, getAngle(environment.getHeading(node)))
    }

    private fun getAngle(position: Euclidean2DPosition): Double {
        return position.asAngle - Math.PI / 2
    }

    private fun alignDirection() {
        val nodes = neutralZone.getNodesInZone()
        val ownerHeading = environment.getHeading(node)
        val avgGroupHeading = nodes.map { environment.getHeading(it) }
            .foldRight(environment.makePosition(0, 0)) { elem, acc -> acc + elem }
        val normAvgGroupHeading = if (avgGroupHeading == environment.makePosition(0, 0)) avgGroupHeading else avgGroupHeading.normalized()
        val newHeading = ownerHeading.times(MAINTAIN_DIRECTION_WEIGHT) + normAvgGroupHeading.times(1 - MAINTAIN_DIRECTION_WEIGHT)
        environment.setHeading(node, newHeading)
    }

    private fun setMovementInConcentration(movement: Euclidean2DPosition, zoneName: String) {
        node.setConcentration(SimpleMolecule("x"), movement.x)
        node.setConcentration(SimpleMolecule("y"), movement.y)
        node.setConcentration(SimpleMolecule("zone"), zoneName)
    }

    private fun turning() {
        val position = environment.getPosition(node)
        val headingToOrigin = environment.makePosition(-position.x, -position.y)
        val angle = environment.getHeading(node).angleBetween(headingToOrigin)
        val distanceFromOrigin = hypot(position.x, position.y)

        if (distanceFromOrigin > radiusPreference) {
            val coef = distanceFromOrigin / (radiusPreference * 10)
            val prob = (coef * (angle / Math.PI))
            if (nodeRandomizer.nextDouble() < prob) {
                val additionalTurnBoost = 5.0
                val turningAngle = getTurningAngle(additionalTurnBoost)
                val newHeading = getHeadingAfterTurning(turningAngle)
                val newAngleBetween = newHeading.angleBetween(headingToOrigin)
                val finalHeading = if (newAngleBetween < angle || (angle / Math.PI) > 0.9) {
                    newHeading
                } else {
                    getHeadingAfterTurning(turningAngle * -1)
                }
                environment.setHeading(node, finalHeading)
            }
        } else if (nodeRandomizer.nextDouble() < TURNING_PROBABILITY_INSIDE_WORLD) {
            environment.setHeading(node, getHeadingAfterTurning(getTurningAngle()))
        }
    }

    private fun getHeadingAfterTurning(turningAngle: Double): Euclidean2DPosition {
        val headingAngle = environment.getHeading(node).asAngle + turningAngle
        return environment.makePosition(cos(headingAngle), sin(headingAngle))
    }

    private fun getTurningAngle(additionalAngles: Double = 0.0): Double = toRadians(1.0 + additionalTurningForce + additionalAngles) * turningDirection
}
