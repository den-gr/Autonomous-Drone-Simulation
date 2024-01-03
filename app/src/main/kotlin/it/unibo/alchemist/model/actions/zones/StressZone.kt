package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.actions.utils.Direction
import it.unibo.alchemist.model.actions.utils.Movement
import it.unibo.alchemist.model.actions.zones.shapes.ZoneShape
import it.unibo.alchemist.model.geometry.Euclidean2DShape
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition

class StressZone(
    override val zoneShape: ZoneShape<Euclidean2DShape>,
    node: Node<Any>,
    private val environment: Physics2DEnvironment<Any>,
    private val movements: Map<Direction, Movement>,
    private val repulsionFactor: Double,
) : AbstractZone(node, environment, movements) {
    override val visibleNodes: Molecule = SimpleMolecule("Stress zone")

    override fun getZoneCentroid(position: Euclidean2DPosition): Euclidean2DPosition {
        return position
    }

    override fun getNextMovement(): Movement {
        val pos = environment.getPosition(owner)
        return getStressZoneMovement(pos, getNodesInZone(pos))
    }

    private fun getStressZoneMovement(nodePosition: Euclidean2DPosition, neighbourNodes: List<Node<Any>>): Movement {
        val positions = mutableSetOf<RelativePosition>()
        for (neighbourNode in neighbourNodes) {
//            val rotated = rotateVector(Euclidean2DPosition(targetNodePosition.x - nodePosition.x, targetNodePosition.y - nodePosition.y), Euclidean2DPosition(0.0, 1.0).asAngle - environment.getHeading(owner).asAngle) //- Math.PI / 2)
            val (angle, offset) = getAngleFromHeadingToNeighbour(nodePosition, environment.getPosition(neighbourNode))

            for (relativePos in RelativePosition.values()) {
                val startAngle = relativePos.startAngle - offset
                val endAngle = relativePos.endAngle - offset
                if (relativePos == RelativePosition.FORWARD && (angle <= startAngle || angle >= endAngle)) {
                    positions.add(relativePos)
                } else if (relativePos != RelativePosition.FORWARD && startAngle <= angle && angle <= endAngle) {
                    positions.add(relativePos)
                }
            }
        }
        if (positions.contains(RelativePosition.FORWARD)) {
            return movements.getValue(Direction.FORWARD).addVelocityModifier(0.0, -repulsionFactor)
        } else if (positions.contains(RelativePosition.RIGHT) && positions.contains(RelativePosition.LEFT)) {
            return movements.getValue(Direction.FORWARD)
        } else if (positions.contains(RelativePosition.BEHIND_LEFT) && positions.contains(RelativePosition.BEHIND_RIGHT)) {
            return movements.getValue(Direction.FORWARD).addVelocityModifier(0.0, repulsionFactor)
        } else if (positions.containsAll(listOf(RelativePosition.BEHIND_LEFT, RelativePosition.RIGHT))) {
            return movements.getValue(Direction.FORWARD)
        } else if (positions.containsAll(listOf(RelativePosition.BEHIND_RIGHT, RelativePosition.LEFT))) {
            return movements.getValue(Direction.FORWARD)
        } else if (positions.contains(RelativePosition.RIGHT) || positions.contains(RelativePosition.BEHIND_RIGHT)) {
            return movements.getValue(Direction.LEFT)
        } else if (positions.contains(RelativePosition.LEFT) || positions.contains(RelativePosition.BEHIND_LEFT)) {
            return movements.getValue(Direction.RIGHT)
        }
        return Movement(0.0, 0.0)
    }
}
