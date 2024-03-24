package it.unibo.alchemist.model.actions.utils

import it.unibo.alchemist.model.positions.Euclidean2DPosition
import kotlin.random.Random

/**
 * Helps produce individuals movements in base of initial setup.
 * @param lateralVelocity velocity to go left and right.
 * @param forwardVelocity velocity to go forward.
 * @param leftMovementProbability
 * @param forwardMovementProbability
 * @param rightMovementProbability
 * @param randomizer to preserve reproducibility.
 */
class MovementProvider(
    private val lateralVelocity: Double,
    private val forwardVelocity: Double,
    leftMovementProbability: Double,
    forwardMovementProbability: Double,
    rightMovementProbability: Double,
    private val randomizer: Random,
) {
    private val movementsProbabilities = mapOf(
        Direction.LEFT to leftMovementProbability,
        Direction.FORWARD to forwardMovementProbability,
        Direction.RIGHT to rightMovementProbability,
    )

    private val movements: Map<Direction, Euclidean2DPosition> = mapOf(
        Direction.LEFT to Euclidean2DPosition(-lateralVelocity, 0.0),
        Direction.FORWARD to Euclidean2DPosition(0.0, forwardVelocity),
        Direction.RIGHT to Euclidean2DPosition(lateralVelocity, 0.0),
    )

    fun toLeft(): Euclidean2DPosition = movements.getValue(Direction.LEFT)

    fun forward(): Euclidean2DPosition = movements.getValue(Direction.FORWARD)

    fun toRight(): Euclidean2DPosition = movements.getValue(Direction.RIGHT)

    fun getRandomMovement(): Euclidean2DPosition {
        val randomNumber = randomizer.nextDouble()
        var cumulativeProbability = 0.0

        for (probability in movementsProbabilities) {
            cumulativeProbability += probability.value
            if (randomNumber <= cumulativeProbability) {
                return movements.getValue(probability.key) // .addVelocityModifier(getNoiseModifier(), getNoiseModifier())
            }
        }
        error("The sum of movement probabilities is not equal to 1")
    }

    override fun toString(): String {
        return "Velocities: [forward=$forwardVelocity, lateral=$lateralVelocity] \n" +
            "Movement probabilities: [" +
            "left=${movementsProbabilities[Direction.LEFT]}, " +
            "forward=${movementsProbabilities[Direction.FORWARD]}, " +
            "right=${movementsProbabilities[Direction.RIGHT]}] \n"
    }
}
