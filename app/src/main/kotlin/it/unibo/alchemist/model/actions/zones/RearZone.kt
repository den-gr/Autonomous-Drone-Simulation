package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.actions.utils.MovementProvider
import it.unibo.alchemist.model.actions.zones.shapes.ZoneShape
import it.unibo.alchemist.model.geometry.Euclidean2DShape
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import kotlin.random.Random

/**
 * Rear zone allows to define who is trailer and who leader.
 */
class RearZone(
    override val zoneShape: ZoneShape<Euclidean2DShape>,
    node: Node<Any>,
    private val environment: Physics2DEnvironment<Any>,
    movementProvider: MovementProvider,
    private val slowDownFactor: Double,
    private val slowDownProbability: Double,
    herdRecognitionPredicate: (Int) -> Boolean,
    private val randomizer: Random,
) : AbstractZone(node, environment, movementProvider, herdRecognitionPredicate) {

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
