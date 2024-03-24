package it.unibo.alchemist.model.actions

import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.actions.utils.GeometryUtils.rotateVector
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

/**
 * Action for agent based herd movement behavior.
 * @param node action owner.
 * @param environment
 * @param zonesRadii array that contains radii of [stress, neutral, attraction, rear] zones.
 * @param velocities array that contains [lateral, forward] intrinsic velocities.
 * @param movementProbabilities contains probabilities to move [left, forward, right] directions.
 * @param stressZoneRepulsionFactor [0; 1] Slow down by a factor if there are neighbors ahead of the individual in the stress zone.
 * @param attractionZoneSpeedUpFactor [1; inf] multiply velocity to this factor if an individual is attracted.
 * @param leaderSlowDown array with leaders' [slowDownFactor, slowDownProbability]]. reduce leader's velocity by multiplying it slow down factor.
 * @param trailerSpeedUp array with trailers' [trailersSpeedUpFactor, trailerSpeedUpProbability]
 */
class HerdBehavior @JvmOverloads constructor(
    node: Node<Any>,
    private val environment: ContinuousPhysics2DEnvironment<Any>,
    private val zonesRadii: ArrayList<Double>,
    private val velocities: ArrayList<Double>,
    private val movementProbabilities: ArrayList<Double>,
    private val stressZoneRepulsionFactor: Double,
    private val attractionZoneSpeedUpFactor: Double,
    private val leaderSlowDown: ArrayList<Double>,
    private val trailerSpeedUp: ArrayList<Double>,
    private val numberOfHerds: Int = 1,
    private val radiusPreference: Int = 1000,
    private val seed: Int = 1,
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
        /**
         * How many times stress zone length is bigger than width.
         */
        const val STRESS_ZONE_ELLIPSE_RATIO = 2.0

        /**
         * Zones angle in degrees (except stress zone).
         */
        const val ANGLE_OF_ZONE = 180.0 // degrees

        /**
         * The tendency to preserve individual's own direction.
         */
        const val MAINTAIN_DIRECTION_WEIGHT = 0.8

        /**
         * The probability to change direction while inside desired world borders.
         */
        const val TURNING_PROBABILITY_INSIDE_WORLD = 0.1
    }

    init {
        // allows to recognize individuals from the same herd
        val herdRecognitionPredicate: (Int) -> Boolean =
            { neighborId -> node.id % numberOfHerds == neighborId % numberOfHerds }

        herdRandomizer = Random((node.id % numberOfHerds) + seed)
        nodeRandomizer = Random(node.id + seed)
        additionalTurningForce = herdRandomizer.nextDouble(0.0, 2.0)
        val herdDirectionAngle = herdRandomizer.nextDouble(0.0, 4 * FastMath.PI)

        turningDirection = if (nodeRandomizer.nextDouble() > 0.5) 1 else -1

        movementProvider = MovementProvider(
            velocities[0], // lateral velocity
            velocities[1], // forward velocity
            movementProbabilities[0], // left movement probability
            movementProbabilities[1], // forward movement probability
            movementProbabilities[2], // right movement probability
            nodeRandomizer,
        )
        environment.setHeading(node, Euclidean2DPosition(cos(herdDirectionAngle), sin(herdDirectionAngle)))

        val stressZoneRadius: Double = zonesRadii[0]
        val neutralZoneRadius: Double = zonesRadii[1]
        val attractionZoneRadius: Double = zonesRadii[2]
        val rearZoneRadius: Double = zonesRadii[3]

        val zoneShapeFactory = ZoneShapeFactoryImpl(environment.shapeFactory)
        val stressZoneShape = zoneShapeFactory.produceEllipseZoneShape(stressZoneRadius, STRESS_ZONE_ELLIPSE_RATIO)
        stressZone = StressZone(stressZoneShape, node, environment, movementProvider, stressZoneRepulsionFactor)

        val neutralZoneShape = zoneShapeFactory.produceCircularSectorZoneShape(neutralZoneRadius, ANGLE_OF_ZONE)
        neutralZone = NeutralZone(neutralZoneShape, node, environment, movementProvider, herdRecognitionPredicate)

        val attractionZoneShape = zoneShapeFactory.produceCircularSectorZoneShape(attractionZoneRadius, ANGLE_OF_ZONE)
        attractionZone = AttractionZone(
            attractionZoneShape,
            node,
            environment,
            movementProvider,
            attractionZoneSpeedUpFactor,
            herdRecognitionPredicate,
        )

        val leaderSlowDownFactor = leaderSlowDown[0]
        val leaderSlowDownProbability = leaderSlowDown[1]
        val rearZoneShape = zoneShapeFactory
            .produceCircularSectorZoneShape(rearZoneRadius, ANGLE_OF_ZONE, true)
        rearZone = RearZone(
            rearZoneShape,
            node,
            environment,
            movementProvider,
            leaderSlowDownFactor,
            leaderSlowDownProbability,
            herdRecognitionPredicate,
            nodeRandomizer,
        )

        zones = listOf(stressZone, neutralZone, attractionZone, rearZone)
    }

    override fun cloneAction(node: Node<Any>, reaction: Reaction<Any>) =
        HerdBehavior(
            node,
            environment,
            zonesRadii,
            velocities,
            movementProbabilities,
            stressZoneRepulsionFactor,
            attractionZoneSpeedUpFactor,
            leaderSlowDown,
            trailerSpeedUp,
            numberOfHerds,
            radiusPreference,
            seed,
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
        val normAvgGroupHeading = if (avgGroupHeading == environment.makePosition(0, 0)) {
            avgGroupHeading
        } else {
            avgGroupHeading.normalized()
        }
        val newHeading = ownerHeading.times(MAINTAIN_DIRECTION_WEIGHT) +
            normAvgGroupHeading.times(1 - MAINTAIN_DIRECTION_WEIGHT)
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

    private fun getTurningAngle(additionalAngles: Double = 0.0): Double =
        toRadians(1.0 + additionalTurningForce + additionalAngles) * turningDirection
}
