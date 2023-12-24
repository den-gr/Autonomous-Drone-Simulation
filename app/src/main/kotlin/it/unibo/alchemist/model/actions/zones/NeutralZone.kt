package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.actions.utils.Direction
import it.unibo.alchemist.model.actions.utils.Movement
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import kotlin.math.atan2

enum class RelativeNeutralZonePosition(val startAngle: Double, val endAngle: Double) {
    LEFT(Math.PI * (0.5), -Math.PI * 0.5),
    RIGHT(-Math.PI * 0.5, Math.PI * (0.5)),
}

class NeutralZone(
    ownerNodeId: Int,
    private val environment: Physics2DEnvironment<Any>,
    private val movements: Map<Direction, Movement>,
    neutralZoneWidth: Double,
    private val neutralZoneHeight: Double,
) : AbstractZone(ownerNodeId, environment, movements) {
    private var lastDetectedNodes: List<Node<Any>> = listOf()
    private var lastPosition: Euclidean2DPosition = Euclidean2DPosition(0.0, 0.0)
    private val shape = environment.shapeFactory.rectangle(neutralZoneWidth * 2, neutralZoneHeight)

    override fun areNodesInZone(): Boolean {
        lastPosition = environment.getPosition(environment.getNodeByID(ownerNodeId))
        val heading = environment.getHeading(environment.getNodeByID(ownerNodeId))

        val neutralZone = shape.transformed {
            origin(Euclidean2DPosition(lastPosition.x, lastPosition.y + neutralZoneHeight / 2))
            rotate(heading.asAngle)
        }

        lastDetectedNodes = findNodesInZone(neutralZone)
        return lastDetectedNodes.isNotEmpty()
    }

    override fun getNextMovement(): Movement {
        val positions = mutableSetOf<RelativeNeutralZonePosition>()

        for (neighbourNode in lastDetectedNodes) {
            val targetNodePosition = environment.getPosition(neighbourNode)
            val angle = atan2(targetNodePosition.y - lastPosition.y, targetNodePosition.x - lastPosition.x)
            for (relativePos in RelativeNeutralZonePosition.values()) {
                if (relativePos == RelativeNeutralZonePosition.LEFT && (relativePos.startAngle <= angle || angle <= relativePos.endAngle)) {
                    positions.add(relativePos)
                } else if (relativePos.startAngle <= angle && angle <= relativePos.endAngle) {
                    positions.add(relativePos)
                }
            }
        }
        if (positions.contains(RelativeNeutralZonePosition.LEFT) && !positions.contains(RelativeNeutralZonePosition.RIGHT)) {
            return movements.getValue(Direction.LEFT)
        } else if (!positions.contains(RelativeNeutralZonePosition.LEFT) && positions.contains(RelativeNeutralZonePosition.RIGHT)) {
            return movements.getValue(Direction.LEFT)
        }

        return getRandomMovement()

    }
}
