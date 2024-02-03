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
import it.unibo.alchemist.model.actions.zones.test.StressZoneT
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.ContinuousPhysics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import org.apache.commons.math3.util.FastMath
import java.lang.Math.random
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
) : AbstractAction<Any>(node) {
    val zones: List<Zone>
    private val stressZone: StressZoneT
    private val neutralZone: NeutralZone
    private val attractionZone: AttractionZone
    private val rearZone: RearZone
    private val movementProvider: MovementProvider

    private val trailersSpeedUpFactor: Double = trailerSpeedUp[0]
    private val trailersSpeedUpProbability: Double = trailerSpeedUp[0]

    private val turningDirection: Int
    private val additionalTurningForce: Double // degrees
    companion object {
        const val STRESS_ZONE_ELLIPSE_RATIO = 2.0
        const val ANGLE_OF_ZONE = 180.0 // degrees
        const val MAINTAIN_DIRECTION_WEIGHT = 0.8
    }

    init {
        val seed = if (numberOfHerds == 1) node.id else node.id % numberOfHerds
        val r = Random(seed)
        turningDirection = if (r.nextDouble() > 0.5) 1 else -1
        additionalTurningForce = r.nextDouble()
        val angle = r.nextDouble(0.0, 2 * FastMath.PI)

        node.properties[0].node
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
        )
        environment.setHeading(node, Euclidean2DPosition(cos(angle), sin(angle)))

        val zoneShapeFactory = ZoneShapeFactoryImpl(environment.shapeFactory)
        val stressZoneShape = zoneShapeFactory.produceEllipseZoneShape(stressZoneRadius, STRESS_ZONE_ELLIPSE_RATIO)
        stressZone = StressZoneT(stressZoneShape, node, environment, movementProvider, stressZoneRepulsionFactor)

        val neutralZoneShape = zoneShapeFactory.produceCircularSectorZoneShape(neutralZoneRadius, ANGLE_OF_ZONE)
        neutralZone = NeutralZone(neutralZoneShape, node, environment, movementProvider, numberOfHerds)

        val attractionZoneShape = zoneShapeFactory.produceCircularSectorZoneShape(attractionZoneRadius, ANGLE_OF_ZONE)
        attractionZone = AttractionZone(attractionZoneShape, node, environment, movementProvider, attractionZoneSpeedUpFactor, numberOfHerds)

        val leaderSlowDownFactor = leaderSlowDown[0]
        val leaderSlowDownProbability = leaderSlowDown[1]
        val rearZoneShape = zoneShapeFactory.produceCircularSectorZoneShape(rearZoneRadius, ANGLE_OF_ZONE, true)
        rearZone = RearZone(rearZoneShape, node, environment, movementProvider, leaderSlowDownFactor, leaderSlowDownProbability, numberOfHerds)

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
        environment.moveNode(node, getNextPosition())
    }

    override fun getContext(): Context = Context.LOCAL

    private fun getAngle(position: Euclidean2DPosition): Double {
        return position.asAngle - Math.PI / 2
    }

    private fun getNextPosition(): Euclidean2DPosition {
        alignDirection()
        for (zone in zones) {
            if (zone.areNodesInZone()) {
                var movement = zone.getNextMovement() // .addVelocityModifier(getNoiseModifier(), getNoiseModifier())
                if (!rearZone.areNodesInZone() && Random.nextDouble() <= trailersSpeedUpProbability) {
                    movement *= trailersSpeedUpFactor
                }

                if (zone is RearZone) turning()

                setMovementInConcentration(movement, zone::class.toString())

                val relativeRotatedMovement = rotateVector(movement, getAngle(environment.getHeading(node)))
                if (zone !is StressZone && stressZone.areNodesInZone(relativeRotatedMovement.plus(environment.getPosition(node)))) {
                    val randomMovement = movementProvider.getRandomMovement()
                    return rotateVector(randomMovement, getAngle(environment.getHeading(node)))
                }
                return relativeRotatedMovement
            }
        }
        if (random() < 0.5) turning()
        val movement = movementProvider.getRandomMovement()
        setMovementInConcentration(movement, "Out of Zone")
        return rotateVector(movement, getAngle(environment.getHeading(node)))
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

    private fun setMovementInConcentration(movement: Euclidean2DPosition, zoneName: String) {
        node.setConcentration(SimpleMolecule("x"), movement.x)
        node.setConcentration(SimpleMolecule("y"), movement.y)
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
            val headingAngle = environment.getHeading(node).asAngle + (toRadians(2.0) * turningDirection)
            environment.setHeading(node, environment.makePosition(cos(headingAngle), sin(headingAngle)))
        }
    }

    private fun getNoiseModifier(): Double {
        val sign = if (Random.nextBoolean()) 1 else -1
        return (Random.nextDouble() / 10.0) * sign
    }
}
