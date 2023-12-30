package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.actions.utils.Direction
import it.unibo.alchemist.model.actions.utils.Movement
import it.unibo.alchemist.model.geometry.Euclidean2DShape
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import java.lang.IllegalStateException
import kotlin.random.Random

enum class RelativeLateralZonePosition(val startAngle: Double, val endAngle: Double) {
    LEFT(Math.PI * (0.6), -Math.PI * 0.5),
    RIGHT(-Math.PI * 0.5, Math.PI * (0.4)),
}

abstract class AbstractZone(
    protected val node: Node<Any>,
    private val environment: Physics2DEnvironment<Any>,
    private val movements: Map<Direction, Movement>,
) : Zone {
    abstract val visibleNodes: Molecule

    protected fun findNodesInZone(zoneShape: Euclidean2DShape): List<Node<Any>> {
        val nodes = environment.getNodesWithin(zoneShape)
        return nodes.filter { it.id != node.id }
    }

    protected fun getRandomMovement(): Movement {
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

    override fun areNodesInZone(): Boolean {
        val position = environment.getPosition(node)
        return areNodesInZone(position)
    }

    override fun areNodesInZone(position: Euclidean2DPosition): Boolean {
        val nodesInZone = getNodesInZone(position)
        node.setConcentration(visibleNodes, nodesInZone.map { it.id })
        return nodesInZone.isNotEmpty()
    }

    protected fun getNodesInZone(position: Euclidean2DPosition): List<Node<Any>> {
        val heading = environment.getHeading(node)
        return findNodesInZone(
            zoneShape.shape.transformed {
                origin(getZoneCentroid(position))
                rotate(heading.asAngle - Math.PI / 2)
            },
        )
    }

    abstract fun getZoneCentroid(position: Euclidean2DPosition): Euclidean2DPosition
}
