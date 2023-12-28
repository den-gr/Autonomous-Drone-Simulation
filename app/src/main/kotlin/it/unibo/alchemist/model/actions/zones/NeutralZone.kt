package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.actions.utils.Direction
import it.unibo.alchemist.model.actions.utils.Movement
import it.unibo.alchemist.model.geometry.Euclidean2DShape
import it.unibo.alchemist.model.molecules.SimpleMolecule
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
    override val zoneShape: ZoneShape<Euclidean2DShape> = RectangularShape(
        environment.shapeFactory.rectangle(neutralZoneWidth * 2, neutralZoneHeight),
        neutralZoneWidth * 2,
        neutralZoneHeight,
        neutralZoneHeight / 2,
    )
//    override val shape: Euclidean2DShape
//
//    init {
//        shape = environment.shapeFactory.circleSector(
//            neutralZoneHeight,
//            180.0,
//            Euclidean2DPosition(0.0, 1.0).asAngle,
//        )
//    }

    override fun areNodesInZone(): Boolean {
        val node = environment.getNodeByID(ownerNodeId)
        lastPosition = environment.getPosition(node)
        val heading = environment.getHeading(node)

        val neutralZone = zoneShape.shape.transformed {
            origin(Euclidean2DPosition(lastPosition.x, lastPosition.y + zoneShape.gap)) // TODO zone margin with heading consideration
            rotate(heading.asAngle - Math.PI / 2)
        }
        lastDetectedNodes = findNodesInZone(neutralZone)
        node.setConcentration(SimpleMolecule("ids neutral"), lastDetectedNodes.map { it.id })
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
            return movements.getValue(Direction.RIGHT)
        }

        return getRandomMovement()
    }
}
