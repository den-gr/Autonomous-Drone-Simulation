package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.actions.utils.MovementProvider
import it.unibo.alchemist.model.actions.zones.shapes.ZoneShape
import it.unibo.alchemist.model.geometry.Euclidean2DShape
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition

class NeutralZone(
    override val zoneShape: ZoneShape<Euclidean2DShape>,
    node: Node<Any>,
    private val environment: Physics2DEnvironment<Any>,
    movementProvider: MovementProvider,
) : AbstractZone(node, environment, movementProvider) {

    override val visibleNodes: Molecule = SimpleMolecule("Neutral zone")

    override fun getNextMovement(): Euclidean2DPosition {
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

        if (positions.contains(RelativeLateralZonePosition.LEFT) && !positions.contains(RelativeLateralZonePosition.RIGHT)) {
            return movementProvider.toLeft() + movementProvider.forward()
        } else if (!positions.contains(RelativeLateralZonePosition.LEFT) && positions.contains(RelativeLateralZonePosition.RIGHT)) {
            return movementProvider.toRight() + movementProvider.forward()
        }

        return movementProvider.getRandomMovement()
    }
}
