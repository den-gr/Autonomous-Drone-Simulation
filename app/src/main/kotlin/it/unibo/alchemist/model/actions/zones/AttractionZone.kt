package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.actions.utils.MovementProvider
import it.unibo.alchemist.model.actions.zones.shapes.ZoneShape
import it.unibo.alchemist.model.geometry.Euclidean2DShape
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition

/**
 * Attraction zone responsible of group cohesion.
 */
class AttractionZone(
    override val zoneShape: ZoneShape<Euclidean2DShape>,
    node: Node<Any>,
    private val environment: Physics2DEnvironment<Any>,
    movementProvider: MovementProvider,
    private val speedUpFactor: Double,
    herdRecognitionPredicate: (Int) -> Boolean,
) : AbstractZone(node, environment, movementProvider, herdRecognitionPredicate) {

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
        return if (positions.contains(RelativeLateralPosition.LEFT) && !positions.contains(RelativeLateralPosition.RIGHT)) {
            movement + (movementProvider.toLeft() * speedUpFactor)
        } else if (!positions.contains(RelativeLateralPosition.LEFT) && positions.contains(RelativeLateralPosition.RIGHT)) {
            movement + (movementProvider.toRight() * speedUpFactor)
        } else if (positions.contains(RelativeLateralPosition.LEFT) && positions.contains(RelativeLateralPosition.RIGHT)) {
            movement + (movementProvider.forward() * speedUpFactor)
        } else {
            error("Nodes not found in attraction zone")
        }
    }
}
