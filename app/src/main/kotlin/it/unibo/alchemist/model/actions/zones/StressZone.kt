package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.actions.utils.MovementProvider
import it.unibo.alchemist.model.actions.zones.shapes.ZoneShape
import it.unibo.alchemist.model.geometry.Euclidean2DShape
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition

/**
 * Stress zone of an individual, it is responsible for near distance repulsion.
 */
class StressZone(
    override val zoneShape: ZoneShape<Euclidean2DShape>,
    node: Node<Any>,
    private val environment: Physics2DEnvironment<Any>,
    movementProvider: MovementProvider,
    private val repulsionFactor: Double,
) : AbstractZone(node, environment, movementProvider) {

    override fun getNextMovement(): Euclidean2DPosition {
        return getStressZoneMovement(getNodesInZone())
    }

    override fun filterOtherHerds(nodes: List<Node<Any>>): List<Node<Any>> = nodes

    private fun getStressZoneMovement(neighbourNodes: List<Node<Any>>): Euclidean2DPosition {
        val positions = mutableSetOf<RelativePosition>()
        for (neighbourNode in neighbourNodes) {
            val angle = getAngleFromHeadingToNeighbour(environment.getPosition(neighbourNode))

            for (relativePos in RelativePosition.values()) {
                val startAngle = relativePos.startAngle
                val endAngle = relativePos.endAngle
                if (relativePos == RelativePosition.FORWARD && (angle <= startAngle || angle >= endAngle)) {
                    positions.add(relativePos)
                } else if (relativePos != RelativePosition.FORWARD && startAngle <= angle && angle <= endAngle) {
                    positions.add(relativePos)
                }
            }
        }
        var movement = movementProvider.getRandomMovement()
        if (positions.contains(RelativePosition.RIGHT) && !positions.contains(RelativePosition.LEFT)) {
            movement += movementProvider.toLeft() // add force lateral force
        }
        if (positions.contains(RelativePosition.LEFT) && !positions.contains(RelativePosition.RIGHT)) {
            movement += movementProvider.toRight() // add force lateral force
        }

        // lateral forces are already added, so add only forward force
        if (positions.contains(RelativePosition.BEHIND_RIGHT) && positions.contains(RelativePosition.BEHIND_LEFT)) {
            movement = (movementProvider.forward() + movement).addVelocityModifier(0.0, repulsionFactor)
        } else if (positions.contains(RelativePosition.BEHIND_RIGHT) ||
            positions.contains(RelativePosition.BEHIND_LEFT)
        ) {
            movement += movementProvider.forward()
        }

        // reduce forward velocity only if movement have forward velocity
        if (positions.contains(RelativePosition.FORWARD) && movement.y > repulsionFactor) {
            movement = movement.addVelocityModifier(0.0, -repulsionFactor)
        }
        return movement
    }
}
