package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.actions.utils.Direction
import it.unibo.alchemist.model.actions.utils.Movement
import it.unibo.alchemist.model.actions.zones.shapes.ZoneShape
import it.unibo.alchemist.model.geometry.Euclidean2DShape
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment

class NeutralZone(
    override val zoneShape: ZoneShape<Euclidean2DShape>,
    node: Node<Any>,
    private val environment: Physics2DEnvironment<Any>,
    private val movements: Map<Direction, Movement>,
) : AbstractZone(node, environment, movements) {

    override val visibleNodes: Molecule = SimpleMolecule("Neutral zone")

    override fun getNextMovement(): Movement {
        val positions = mutableSetOf<RelativeLateralZonePosition>()
        val nodePosition = environment.getPosition(owner)

        for (neighbourNode in getNodesInZone(nodePosition)) {
            val angle = getAngleFromHeadingToNeighbour(nodePosition, environment.getPosition(neighbourNode))
            for (relativePos in RelativeLateralZonePosition.values()) {
                if (angle in relativePos.startAngle..relativePos.endAngle) {
                    positions.add(relativePos)
                }
            }
        }

        val forwardV = movements.getValue(Direction.FORWARD)
        val lateralV = movements.getValue(Direction.LEFT)
        if (positions.contains(RelativeLateralZonePosition.LEFT) && !positions.contains(RelativeLateralZonePosition.RIGHT)) {
            return Movement(lateralV.lateralVelocity, forwardV.forwardVelocity)
        } else if (!positions.contains(RelativeLateralZonePosition.LEFT) && positions.contains(RelativeLateralZonePosition.RIGHT)) {
            return Movement(-lateralV.lateralVelocity, forwardV.forwardVelocity)
        }

        return getRandomMovement()
    }
}
