package it.unibo.alchemist.model.actions.zones.test

import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.actions.utils.MovementProvider
import it.unibo.alchemist.model.actions.zones.AbstractZone
import it.unibo.alchemist.model.actions.zones.shapes.ZoneShape
import it.unibo.alchemist.model.geometry.Euclidean2DShape
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition

class StressZoneT(
    override val zoneShape: ZoneShape<Euclidean2DShape>,
    node: Node<Any>,
    private val environment: Physics2DEnvironment<Any>,
    movementProvider: MovementProvider,
    private val repulsionFactor: Double,
) : AbstractZone(node, environment, movementProvider) {
    override val visibleNodes: Molecule = SimpleMolecule("Stress zone T")

    override fun getNextMovement(): Euclidean2DPosition {
        val pos = environment.getPosition(owner)
        return getStressZoneMovement(pos, getNodesInZone(pos))
    }

    private fun getStressZoneMovement(nodePosition: Euclidean2DPosition, neighbourNodes: List<Node<Any>>): Euclidean2DPosition {
        val positions = mutableSetOf<RelativePositionT>()
        for (neighbourNode in neighbourNodes) {
            val angle = getAngleFromHeadingToNeighbour(nodePosition, environment.getPosition(neighbourNode))

            for (relativePos in RelativePositionT.values()) {
                val startAngle = relativePos.startAngle
                val endAngle = relativePos.endAngle
                if (relativePos == RelativePositionT.FORWARD && (angle <= startAngle || angle >= endAngle)) {
                    positions.add(relativePos)
                } else if (relativePos != RelativePositionT.FORWARD && startAngle <= angle && angle <= endAngle) {
                    positions.add(relativePos)
                }
            }
        }
        var movement = movementProvider.getRandomMovement()
        if (positions.contains(RelativePositionT.RIGHT) && !positions.contains(RelativePositionT.LEFT)) {
            movement += movementProvider.toLeft()
        } else if (positions.contains(RelativePositionT.LEFT) && !positions.contains(RelativePositionT.RIGHT)) {
            movement += movementProvider.toRight()
        } else if (positions.contains(RelativePositionT.BEHIND_RIGHT) && positions.contains(RelativePositionT.BEHIND_LEFT)) {
            movement = (movementProvider.forward() + movement).addVelocityModifier(0.0, repulsionFactor)
        } else if (positions.contains(RelativePositionT.BEHIND_RIGHT)) {
            movement += movementProvider.toLeftForward()
        } else if (positions.contains(RelativePositionT.BEHIND_LEFT)) {
            movement += movementProvider.toRightForward()
        }

        if (positions.contains(RelativePositionT.FORWARD)) {
            movement += movement.addVelocityModifier(0.0, -repulsionFactor)
        }
        return movement
    }
}
