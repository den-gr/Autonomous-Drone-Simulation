package it.unibo.alchemist.model.actions

import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.actions.utils.Direction
import it.unibo.alchemist.model.actions.utils.Movement
import it.unibo.alchemist.model.actions.zones.NeutralZone
import it.unibo.alchemist.model.actions.zones.StressZone
import it.unibo.alchemist.model.actions.zones.Zone
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.ContinuousPhysics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import org.protelis.lang.datatype.impl.ArrayTupleImpl
import java.lang.IllegalStateException
import kotlin.random.Random

class Grouping @JvmOverloads constructor(
    node: Node<Any>,
    private val environment: ContinuousPhysics2DEnvironment<Any>,
    private val stressZoneWidth: Double,
    private val stressZoneHeight: Double,
    private val repulsionFactor: Double = 0.5,
) : AbstractAction<Any>(node) {
    private val zones: List<Zone>
//    private val stressZone: StressZone

    private val bodyLen = getMoleculeValue("bodyLen")
    private val movements: Map<Direction, Movement>

    private var largestShapeDiameter: Double = 0.0
    private var i = 0

    init {
        environment.setHeading(node, Euclidean2DPosition(0.0, 1.0))
        val probabilities = getMoleculeDoubleTupleValues("MovementProbabilities")
        val velocities = getMoleculeDoubleTupleValues("Velocities")
        movements = mapOf(
            Direction.LEFT to Movement(-velocities[0], 0.0, probabilities[0]),
            Direction.FORWARD to Movement(0.0, velocities[1], probabilities[1]),
            Direction.RIGHT to Movement(velocities[0], 0.0, probabilities[2]),
        )
        val list: MutableList<Zone> = mutableListOf()
        val stressZone = StressZone(node.id, environment, movements, stressZoneWidth, stressZoneHeight, repulsionFactor)
        list.add(stressZone)
        list.add(NeutralZone(node.id, environment, movements, 6.0, 12.0))
        zones = list.toList()
    }

    private fun getMoleculeDoubleTupleValues(moleculeName: String): List<Double> {
        return (getMoleculeValue(moleculeName) as ArrayTupleImpl).toList().map { it.toString().toDouble() }
    }

    override fun cloneAction(node: Node<Any>, reaction: Reaction<Any>) =
        Grouping(node, environment, repulsionFactor, stressZoneWidth, stressZoneHeight)

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
        println()
        for (zone in zones) {
            if (zone.areNodesInZone()) {
                val movement = zone.getNextMovement()
                node.setConcentration(SimpleMolecule("zone"), zone::class)
                node.setConcentration(SimpleMolecule("x"), movement.lateralVelocity)
                node.setConcentration(SimpleMolecule("y"), movement.forwardVelocity)
                return environment.makePosition(movement.lateralVelocity, movement.forwardVelocity)
            }
        }
        val movement = getRandomMovement()
        node.setConcentration(SimpleMolecule("zone"), " ")
        node.setConcentration(SimpleMolecule("x"), movement.lateralVelocity)
        node.setConcentration(SimpleMolecule("y"), movement.forwardVelocity)
        return environment.makePosition(movement.lateralVelocity, movement.forwardVelocity)
    }

    private fun getRandomMovement(): Movement {
        val randomNumber = Random.nextDouble()
        var cumulativeProbability = 0.0

        for (movement in movements.values) {
            cumulativeProbability += movement.probability
            if (randomNumber < cumulativeProbability) {
                return movement
            }
        }
        throw IllegalStateException("The sum of movement probabilities is not equal to 1")
    }

    private fun movePointAway(from: Euclidean2DPosition, to: Euclidean2DPosition, distance: Double): Euclidean2DPosition {
        val deltaX = to.x - from.x
        val deltaY = to.y - from.y

        val currentDistance = kotlin.math.hypot(deltaX, deltaY)

        val unitVectorX = deltaX / currentDistance
        val unitVectorY = deltaY / currentDistance

        val newX = unitVectorX * distance
        val newY = unitVectorY * distance

        return Euclidean2DPosition(newX, newY)
    }
}
