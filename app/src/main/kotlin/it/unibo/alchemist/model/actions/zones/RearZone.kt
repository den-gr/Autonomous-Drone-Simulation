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
import kotlin.random.Random

class RearZone(
    override val zoneShape: ZoneShape<Euclidean2DShape>,
    node: Node<Any>,
    private val environment: Physics2DEnvironment<Any>,
    movements: Map<Direction, Movement>,
    private val slowDownFactor: Double,
) : AbstractZone(node, environment, movements) {
    override val visibleNodes: Molecule = SimpleMolecule("Rear zone")

    override fun getZoneCentroid(position: Euclidean2DPosition): Euclidean2DPosition {
        // TODO zone margin with heading consideration
        return Euclidean2DPosition(position.x, position.y)
    }

    override fun getNextMovement(): Movement {
        val velocityModifier = if (Random.nextDouble() > 0.6) slowDownFactor else 1.0
        return getRandomMovement().multiplyVelocity(velocityModifier)
    }

    override fun getHeading(): Euclidean2DPosition {
        val a = environment.getHeading(owner).asAngle
        val angle = if (PI >= 0) a - PI else a + PI
        return Euclidean2DPosition(cos(angle), sin(angle))
    }
}
