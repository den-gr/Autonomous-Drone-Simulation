package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.actions.utils.MovementProvider
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import kotlin.math.atan2

/**
 * @param owner of the zone
 * @param environment
 * @param movementProvider
 * @param herdRecognitionPredicate define how to determinate if another individual belongs to the same herd.
 */
abstract class AbstractZone(
    protected val owner: Node<Any>,
    private val environment: Physics2DEnvironment<Any>,
    protected val movementProvider: MovementProvider,
    private val herdRecognitionPredicate: (Int) -> Boolean = { _ -> true },
) : Zone {
    private val visibleNodes: Molecule = SimpleMolecule("Neighbors in active zone:")

    override fun areNodesInZone(): Boolean {
        val position = environment.getPosition(owner)
        return areNodesInZone(position)
    }

    private fun areNodesInZone(position: Euclidean2DPosition): Boolean {
        val nodesInZone = getNodesInZone(position)
        owner.setConcentration(visibleNodes, nodesInZone.map { it.id })
        return nodesInZone.isNotEmpty()
    }

    override fun getNodesInZone(): List<Node<Any>> {
        val position = environment.getPosition(owner)
        return getNodesInZone(position)
    }

    private fun getNodesInZone(position: Euclidean2DPosition): List<Node<Any>> {
        val transformedShape = zoneShape.shape.transformed {
            origin(position)
            rotate(getHeading())
        }
        return filterOtherHerds(
            environment.getNodesWithin(transformedShape)
                .minusElement(owner),
        )
    }

    protected open fun filterOtherHerds(nodes: List<Node<Any>>): List<Node<Any>> = nodes.filter { herdRecognitionPredicate(it.id) }

    protected fun getAngleFromHeadingToNeighbour(neighbourPosition: Euclidean2DPosition): Double {
        val nodePosition = environment.getPosition(owner)
        val neighbourDirectionAngle = atan2(neighbourPosition.y - nodePosition.y, neighbourPosition.x - nodePosition.x)
        val headingAngle = environment.getHeading(owner).asAngle
        val offset = if (neighbourDirectionAngle < headingAngle) 2 * Math.PI else 0.0
        val angle = neighbourDirectionAngle - headingAngle
        return angle + offset
    }

    protected open fun getHeading(): Euclidean2DPosition {
        return environment.getHeading(owner)
    }

    protected fun Euclidean2DPosition.addVelocityModifier(lateralModifier: Double, forwardModifier: Double): Euclidean2DPosition {
        return Euclidean2DPosition(
            x + x * lateralModifier,
            y + y * forwardModifier,
        )
    }
}
