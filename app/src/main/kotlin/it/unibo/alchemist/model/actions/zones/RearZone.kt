package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.actions.utils.MovementProvider
import it.unibo.alchemist.model.actions.zones.shapes.ZoneShape
import it.unibo.alchemist.model.geometry.Euclidean2DShape
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import kotlin.random.Random

class RearZone(
    override val zoneShape: ZoneShape<Euclidean2DShape>,
    node: Node<Any>,
    private val environment: Physics2DEnvironment<Any>,
    movementProvider: MovementProvider,
    private val slowDownFactor: Double,
    private val slowDownProbability: Double,
    numberOfHerds: Int,
    private val randomizer: Random,
) : AbstractZone(node, environment, movementProvider, numberOfHerds) {
    override val visibleNodes: Molecule = SimpleMolecule("Rear zone")

    init {
        require(slowDownFactor in 0.0..1.0)
        require(slowDownProbability in 0.0001..1.0)
    }

    override fun getNextMovement(): Euclidean2DPosition {
        val velocityModifier = if (randomizer.nextDouble() < slowDownProbability) slowDownFactor else 1.0
        return movementProvider.getRandomMovement() * velocityModifier
    }

    override fun getHeading(): Euclidean2DPosition {
        val heading = environment.getHeading(owner)
        return environment.makePosition(-heading.x, -heading.y)
    }
}
