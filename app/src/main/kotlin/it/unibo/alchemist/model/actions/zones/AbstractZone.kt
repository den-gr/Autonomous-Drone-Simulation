package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.actions.utils.MovementProvider
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import kotlin.math.atan2

enum class RelativeLateralZonePosition(val startAngle: Double, val endAngle: Double) {
    LEFT(0.0, Math.PI),
    RIGHT(Math.PI, 2 * Math.PI),
}

abstract class AbstractZone(
    protected val owner: Node<Any>,
    private val environment: Physics2DEnvironment<Any>,
    protected val movementProvider: MovementProvider,
) : Zone {
    abstract val visibleNodes: Molecule

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

    protected fun getAngleFromHeadingToNeighbour(nodePosition: Euclidean2DPosition, neighbourPosition: Euclidean2DPosition): Double {
        val neighbourDirectionAngle = atan2(neighbourPosition.y - nodePosition.y, neighbourPosition.x - nodePosition.x)
        val headingAngle = environment.getHeading(owner).asAngle
        val offset = if (neighbourDirectionAngle < headingAngle) 2 * Math.PI else 0.0
        val angle = neighbourDirectionAngle - headingAngle
        return angle + offset
    }

    open fun getHeading(): Euclidean2DPosition {
        return environment.getHeading(owner)
    }

    protected fun Euclidean2DPosition.addVelocityModifier(lateralModifier: Double, forwardModifier: Double): Euclidean2DPosition {
        return Euclidean2DPosition(
            x + x * lateralModifier,
            y + y * forwardModifier,
        )
    }
}
