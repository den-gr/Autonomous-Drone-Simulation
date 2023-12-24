package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.actions.utils.Direction
import it.unibo.alchemist.model.actions.utils.Movement
import it.unibo.alchemist.model.geometry.Euclidean2DShape
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import kotlin.math.atan2

class StressZone(
    ownerNodeId: Int,
    private val environment: Physics2DEnvironment<Any>,
    private val movements: Map<Direction, Movement>,
    stressZoneWidth: Double,
    stressZoneHeight: Double,
    private val repulsionFactor: Double,
) : AbstractZone(ownerNodeId, environment, movements) {
    private var lastDetectedNodes: List<Node<Any>> = listOf()
    private var lastPosition: Euclidean2DPosition = Euclidean2DPosition(0.0, 0.0)
    private val shape: Euclidean2DShape

    init {
        shape = environment.shapeFactory.rectangle(stressZoneWidth * 2, stressZoneHeight * 2)
    }

    override fun areNodesInZone(): Boolean {
        val node = environment.getNodeByID(ownerNodeId)
        val position = environment.getPosition(node)
        val heading = environment.getHeading(node)
        val nodesInStressZone = findNodesInZone(
            shape.transformed {
                origin(position)
                rotate(heading.asAngle)
            },
        )
        lastDetectedNodes = nodesInStressZone
        lastPosition = position
        return nodesInStressZone.isNotEmpty()
    }

    override fun getNextMovement(): Movement {
        return getStressZoneMovement(lastPosition, lastDetectedNodes)
    }

    private fun getStressZoneMovement(nodePosition: Euclidean2DPosition, neighbourNodes: List<Node<Any>>): Movement {
        val positions = mutableSetOf<RelativeNeighbourPosition>()
        for (neighbourNode in neighbourNodes) {
            val targetNodePosition = environment.getPosition(neighbourNode)
            val angle = atan2(targetNodePosition.y - nodePosition.y, targetNodePosition.x - nodePosition.x)
            for (relativePos in RelativeNeighbourPosition.values()) {
                if (relativePos == RelativeNeighbourPosition.LEFT && (relativePos.startAngle <= angle || angle <= relativePos.endAngle)) {
                    positions.add(relativePos)
                } else if (relativePos.startAngle <= angle && angle <= relativePos.endAngle) {
                    positions.add(relativePos)
                }
            }
        }
        if (positions.contains(RelativeNeighbourPosition.FORWARD)) {
            return movements.getValue(Direction.FORWARD).addVelocityModifier(0.0, -repulsionFactor)
        } else if (positions.contains(RelativeNeighbourPosition.RIGHT) && positions.contains(RelativeNeighbourPosition.LEFT)) {
            return movements.getValue(Direction.FORWARD)
        } else if (positions.contains(RelativeNeighbourPosition.BEHIND_LEFT) && positions.contains(RelativeNeighbourPosition.BEHIND_RIGHT)) {
            return movements.getValue(Direction.FORWARD).addVelocityModifier(0.0, repulsionFactor)
        } else if (positions.contains(RelativeNeighbourPosition.RIGHT) || positions.contains(RelativeNeighbourPosition.BEHIND_RIGHT)) {
            return movements.getValue(Direction.LEFT)
        } else if (positions.contains(RelativeNeighbourPosition.LEFT) || positions.contains(RelativeNeighbourPosition.BEHIND_LEFT)) {
            return movements.getValue(Direction.RIGHT)
        }
        return Movement(0.0, 0.0)
    }
}
