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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
        return position
    }

    override fun getNextMovement(): Movement {
        val positions = mutableSetOf<RelativeLateralZonePosition>()
        val nodePosition = environment.getPosition(owner)

        for (neighbourNode in getNodesInZone(nodePosition)) {
            val (angle, offset) = getAngleFromHeadingToNeighbour(nodePosition, environment.getPosition(neighbourNode))
            for (relativePos in RelativeLateralZonePosition.values()) {
                val startAngle = relativePos.startAngle - offset
                val endAngle = relativePos.endAngle - offset
                if (angle in startAngle..endAngle) {
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

    fun angleBetweenAngles(angle1: Double, angle2: Double): Double {
        // Calculate the angle difference
        var angleDiff = angle2 - angle1

        // Normalize the angle to be within the range -PI to PI
        while (angleDiff < -PI) {
            angleDiff += 2 * PI
        }

        while (angleDiff > PI) {
            angleDiff -= 2 * PI
        }

        return angleDiff
    }

    fun rotateVector(vector: Euclidean2DPosition, angle: Double): Euclidean2DPosition {
        val newX = vector.x * cos(angle) - vector.y * sin(angle)
        val newY = vector.x * sin(angle) + vector.y * cos(angle)
        return environment.makePosition(newX, newY)
    }
}
