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

class AttractionZone(
    override val zoneShape: ZoneShape<Euclidean2DShape>,
    node: Node<Any>,
    private val environment: Physics2DEnvironment<Any>,
    private val movements: Map<Direction, Movement>,
    val speedUpFactor: Double,
) : AbstractZone(node, environment, movements) {
    private var lastPosition: Euclidean2DPosition = Euclidean2DPosition(0.0, 0.0)
    private var lastDetectedNodes: List<Node<Any>> = listOf()
    override val visibleNodes: Molecule = SimpleMolecule("Attraction zone")


    override fun getZoneCentroid(position: Euclidean2DPosition): Euclidean2DPosition {
        // TODO zone margin with heading consideration
        return Euclidean2DPosition(position.x, position.y + zoneShape.offset)
    }

    override fun getNextMovement(): Movement {
        val positions = mutableSetOf<RelativeLateralZonePosition>()

        for (neighbourNode in lastDetectedNodes) {
            val targetNodePosition = environment.getPosition(neighbourNode)
            val angle = atan2(targetNodePosition.y - lastPosition.y, targetNodePosition.x - lastPosition.x)
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
            return Movement(lateralV.lateralVelocity, forwardV.forwardVelocity).addVelocityModifier(speedUpFactor, -0.4)
        } else if (!positions.contains(RelativeLateralZonePosition.LEFT) && positions.contains(RelativeLateralZonePosition.RIGHT)) {
            return Movement(-lateralV.lateralVelocity, forwardV.forwardVelocity).addVelocityModifier(speedUpFactor, -0.4)
        } else if (positions.contains(RelativeLateralZonePosition.LEFT) && positions.contains(RelativeLateralZonePosition.RIGHT)) {
            return movements.getValue(Direction.FORWARD).addVelocityModifier(0.0, speedUpFactor)
        }

        throw IllegalStateException("Attraction zone is not active with no nodes in zone")
    }
}
