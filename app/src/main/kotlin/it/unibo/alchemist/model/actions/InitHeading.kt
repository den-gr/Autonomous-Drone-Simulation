package it.unibo.alchemist.model.actions

import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.experiment.randomAngle
import org.apache.commons.math3.random.RandomGenerator
import kotlin.math.cos
import kotlin.math.sin

/**
 * Sets heading once, and then removes itself.
 * Should be part of node initialization via constructor parameter?
 */
class InitHeading @JvmOverloads constructor(
    node: Node<Any>,
    private val reaction: Reaction<Any>,
    private val env: Physics2DEnvironment<Any>,
    private val rng: RandomGenerator,
    private val initialAngle: Double = rng.randomAngle(),
) : AbstractAction<Any>(node) {

    init {
        execute()
    }

    override fun cloneAction(n: Node<Any>, r: Reaction<Any>) = InitHeading(n, r, env, rng, initialAngle)

    override fun execute() {
        reaction.actions = reaction.actions.minusElement(this)
        env.setHeading(node, env.makePosition(cos(initialAngle), sin(initialAngle)))
    }

    override fun getContext() = Context.LOCAL
}
