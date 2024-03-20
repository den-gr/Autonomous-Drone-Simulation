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
    herdRecognitionPredicate: (Int) -> Boolean,
) : AbstractZone(node, environment, movementProvider, herdRecognitionPredicate) {

    override val visibleNodes: Molecule = SimpleMolecule("Neutral zone")

    override fun getNextMovement(): Euclidean2DPosition {
        val positions = mutableSetOf<RelativeLateralPosition>()

        for (neighbourNode in getNodesInZone()) {
            val angle = getAngleFromHeadingToNeighbour(environment.getPosition(neighbourNode))
            for (relativePos in RelativeLateralPosition.values()) {
                if (angle in relativePos.startAngle..relativePos.endAngle) {
                    positions.add(relativePos)
                }
            }
        }

        val movement = movementProvider.getRandomMovement()
        if (positions.contains(RelativeLateralPosition.LEFT) && !positions.contains(RelativeLateralPosition.RIGHT)) {
            return movement + movementProvider.toLeft()
        } else if (!positions.contains(RelativeLateralPosition.LEFT) && positions.contains(RelativeLateralPosition.RIGHT)) {
            return movement + movementProvider.toRight()
        }
        return movement
    }
}
