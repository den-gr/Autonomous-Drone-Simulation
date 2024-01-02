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
import kotlin.math.atan2

class NeutralZone(
    override val zoneShape: ZoneShape<Euclidean2DShape>,
    node: Node<Any>,
    private val environment: Physics2DEnvironment<Any>,
    private val movements: Map<Direction, Movement>,
) : AbstractZone(node, environment, movements) {

    override val visibleNodes: Molecule = SimpleMolecule("Neutral zone")

//    init {
//        shape = environment.shapeFactory.circleSector(
//            neutralZoneHeight,
//            180.0,
//            Euclidean2DPosition(0.0, 1.0).asAngle,
//        )
//    }

    override fun getZoneCentroid(position: Euclidean2DPosition): Euclidean2DPosition {
        // TODO zone margin with heading consideration
        return Euclidean2DPosition(position.x, position.y + zoneShape.offset)
    }

    override fun getNextMovement(): Movement {
        val positions = mutableSetOf<RelativeLateralZonePosition>()
        val pos = environment.getPosition(node)

        for (neighbourNode in getNodesInZone(pos)) {
            val targetNodePosition = environment.getPosition(neighbourNode)
            val angle = atan2(targetNodePosition.y - pos.y, targetNodePosition.x - pos.x)
            for (relativePos in RelativeLateralZonePosition.values()) {
                if (relativePos == RelativeLateralZonePosition.LEFT && (relativePos.startAngle <= angle || angle <= relativePos.endAngle)) {
                    positions.add(relativePos)
                } else if (relativePos.startAngle <= angle && angle <= relativePos.endAngle) {
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
