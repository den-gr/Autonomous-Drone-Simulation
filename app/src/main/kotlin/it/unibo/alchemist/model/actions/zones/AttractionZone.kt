package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.actions.utils.MovementProvider
import it.unibo.alchemist.model.actions.zones.shapes.ZoneShape
import it.unibo.alchemist.model.geometry.Euclidean2DShape
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition

class AttractionZone(
    override val zoneShape: ZoneShape<Euclidean2DShape>,
    node: Node<Any>,
    private val environment: Physics2DEnvironment<Any>,
    movementProvider: MovementProvider,
    private val speedUpFactor: Double,
    numberOfHerds: Int,
) : AbstractZone(node, environment, movementProvider, numberOfHerds) {
    override val visibleNodes: Molecule = SimpleMolecule("Attraction zone")

    override fun getNextMovement(): Euclidean2DPosition {
        val positions = mutableSetOf<RelativeLateralZonePosition>()
        for (neighbourNode in getNodesInZone()) {
            val angle = getAngleFromHeadingToNeighbour(environment.getPosition(neighbourNode))
            for (relativePos in RelativeLateralZonePosition.values()) {
                if (angle in relativePos.startAngle..relativePos.endAngle) {
                    positions.add(relativePos)
                }
            }
        }

        val movement = movementProvider.getRandomMovement()
        if (positions.contains(RelativeLateralZonePosition.LEFT) && !positions.contains(RelativeLateralZonePosition.RIGHT)) {
            return movement + (movementProvider.toLeft() * speedUpFactor)
        } else if (!positions.contains(RelativeLateralZonePosition.LEFT) && positions.contains(RelativeLateralZonePosition.RIGHT)) {
            return movement + (movementProvider.toRight() * speedUpFactor)
        } else if (positions.contains(RelativeLateralZonePosition.LEFT) && positions.contains(RelativeLateralZonePosition.RIGHT)) {
            return movement + (movementProvider.forward() * speedUpFactor)
        }
        throw IllegalStateException("Nodes not found in attraction zone")
    }
}
