package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.actions.utils.Direction
import it.unibo.alchemist.model.actions.utils.Movement
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import java.lang.IllegalStateException
import kotlin.math.atan2
import kotlin.random.Random

enum class RelativeLateralZonePosition(val startAngle: Double, val endAngle: Double) {
    LEFT(0.0, Math.PI),
    RIGHT(Math.PI, 2 * Math.PI),
}

data class AngleAndOffset(val angle: Double, val offset: Double)

abstract class AbstractZone(
    protected val owner: Node<Any>,
    private val environment: Physics2DEnvironment<Any>,
    private val movements: Map<Direction, Movement>,
) : Zone {
    abstract val visibleNodes: Molecule

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
        val position = environment.getPosition(owner)
        return areNodesInZone(position)
    }

    override fun areNodesInZone(position: Euclidean2DPosition): Boolean {
        val nodesInZone = getNodesInZone(position)
        owner.setConcentration(visibleNodes, nodesInZone.map { it.id })
        return nodesInZone.isNotEmpty()
    }

    override fun getNodesInZone(position: Euclidean2DPosition): List<Node<Any>> {
        val transformedShape = zoneShape.shape.transformed {
            origin(position)
            rotate(getHeading())
        }
        return environment.getNodesWithin(transformedShape)
            .minusElement(owner)
    }

    protected fun getAngleFromHeadingToNeighbour(nodePosition: Euclidean2DPosition, neighbourPosition: Euclidean2DPosition): AngleAndOffset {
        val neighbourDirectionAngle = atan2(neighbourPosition.y - nodePosition.y, neighbourPosition.x - nodePosition.x)
        val headingAngle = environment.getHeading(owner).asAngle
        val offset = if (neighbourDirectionAngle < headingAngle) 2 * Math.PI else 0.0
        val angle = neighbourDirectionAngle - headingAngle
        return AngleAndOffset(angle, offset)
    }

    open fun getHeading(): Euclidean2DPosition {
        return environment.getHeading(owner)
    }

}
