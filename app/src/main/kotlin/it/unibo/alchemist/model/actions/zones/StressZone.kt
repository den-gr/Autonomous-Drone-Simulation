package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.actions.utils.Direction
import it.unibo.alchemist.model.actions.utils.Movement
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import kotlin.math.atan2

class StressZone(
    node: Node<Any>,
    private val environment: Physics2DEnvironment<Any>,
    private val movements: Map<Direction, Movement>,
    stressZoneWidth: Double,
    stressZoneHeight: Double,
    private val repulsionFactor: Double,
) : AbstractZone(node, environment, movements) {
    private var lastDetectedNodes: List<Node<Any>> = listOf()
    private var lastPosition: Euclidean2DPosition = Euclidean2DPosition(0.0, 0.0)

    override val zoneShape = RectangularShape(
        environment.shapeFactory.rectangle(stressZoneWidth * 2, stressZoneHeight * 2),
        stressZoneWidth * 2,
        stressZoneHeight * 2,
    )

    override fun areNodesInZone(): Boolean {
        val position = environment.getPosition(node)
        return areNodesInZone(position)
    }

    override fun areNodesInZone(position: Euclidean2DPosition): Boolean {
        val heading = environment.getHeading(node)
        val nodesInStressZone = findNodesInZone(
            zoneShape.shape.transformed {
                origin(position)
                rotate(heading.asAngle - Math.PI / 2)
            },
        )
        lastDetectedNodes = nodesInStressZone
        node.setConcentration(SimpleMolecule("ids stress"), nodesInStressZone.map { it.id })
        lastPosition = position
        return nodesInStressZone.isNotEmpty()
    }

    override fun getNextMovement(): Movement {
        return getStressZoneMovement(lastPosition, lastDetectedNodes)
    }

    private fun getStressZoneMovement(nodePosition: Euclidean2DPosition, neighbourNodes: List<Node<Any>>): Movement {
        val positions = mutableSetOf<RelativePosition>()
        for (neighbourNode in neighbourNodes) {
            val targetNodePosition = environment.getPosition(neighbourNode)
            val angle = atan2(targetNodePosition.y - nodePosition.y, targetNodePosition.x - nodePosition.x)
            for (relativePos in RelativePosition.values()) {
                if (relativePos == RelativePosition.LEFT && (relativePos.startAngle <= angle || angle <= relativePos.endAngle)) {
                    positions.add(relativePos)
                } else if (relativePos.startAngle <= angle && angle <= relativePos.endAngle) {
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
