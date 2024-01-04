package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Incarnation
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.actions.utils.Direction
import it.unibo.alchemist.model.actions.utils.Movement
import it.unibo.alchemist.model.nodes.GenericNode
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.physics.properties.RectangularArea
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import kotlin.test.* // ktlint-disable no-wildcard-imports

abstract class AbstractZoneTest {
    abstract val environment: Physics2DEnvironment<Any>

    companion object {
        const val BODY_LEN = 1.0
        const val BODY_WIDTH = BODY_LEN / 2
        val CENTER_POSITION = Euclidean2DPosition(0.0, 0.0)
    }

    protected val movements = mapOf(
        Direction.LEFT to Movement(-RectangleStressZoneTest.LATERAL_VELOCITY, 0.0, 0.25),
        Direction.FORWARD to Movement(0.0, RectangleStressZoneTest.FORWARD_VELOCITY, 0.5),
        Direction.RIGHT to Movement(RectangleStressZoneTest.LATERAL_VELOCITY, 0.0, 0.25),
    )

    protected fun createRectangleNode(
        incarnation: Incarnation<Any, Euclidean2DPosition>,
        environment: Physics2DEnvironment<Any>,
        width: Double,
        height: Double,
    ): Node<Any> {
        val node = GenericNode(incarnation, environment).apply {
            addProperty(RectangularArea(environment, this, width, height))
        }
        environment.setHeading(node, Euclidean2DPosition(0.0, 1.0))
        return node
    }

    protected fun setPositionAndVerifySetting(node: Node<Any>, position: Euclidean2DPosition) {
        environment.moveNodeToPosition(node, position)
        assertEquals(position, environment.getPosition(node))
    }

    protected fun setDefaultHeading(node: Node<Any>) {
        environment.setHeading(node, Euclidean2DPosition(0.0, 1.0))
    }

    protected fun addNode(node: Node<Any>, position: Euclidean2DPosition) {
        environment.addNode(node, position)
        setDefaultHeading(node)
    }
}
