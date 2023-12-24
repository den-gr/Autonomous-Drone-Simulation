package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.SupportedIncarnations
import it.unibo.alchemist.model.linkingrules.NoLinks
import it.unibo.alchemist.model.physics.environments.ContinuousPhysics2DEnvironment
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.* // ktlint-disable no-wildcard-imports

class NeutralZoneTest : AbstractZoneTest() {
    private lateinit var environment: Physics2DEnvironment<Any>
    private lateinit var node1: Node<Any>
    private lateinit var node2: Node<Any>
    private lateinit var node3: Node<Any>
    private lateinit var neutralZone: NeutralZone

    @BeforeTest
    fun beforeTest() {
        val width = 1.0
        val bodyLen = 2.0
        val incarnation = SupportedIncarnations.get<Any, Euclidean2DPosition>("protelis").orElseThrow()
        environment = ContinuousPhysics2DEnvironment(incarnation)
        environment.linkingRule = NoLinks()
        node1 = createRectangleNode(incarnation, environment, width, bodyLen)
        node2 = createRectangleNode(incarnation, environment, width, bodyLen)
        node3 = createRectangleNode(incarnation, environment, width, bodyLen)
        environment.addNode(node1, Euclidean2DPosition(0.0, 0.0))
        environment.addNode(node2, Euclidean2DPosition(5.0, 9.0))
        environment.addNode(node3, Euclidean2DPosition(5.5, -9.0))
//        setHeading(node1)
//        setHeading(node2)
//        setHeading(node3)
        neutralZone = NeutralZone(node1.id, environment, movements, 6 * bodyLen, 12 * bodyLen)
    }

    private fun setHeading(node: Node<Any>) {
        environment.setHeading(node, Euclidean2DPosition(0.0, 1.0))
    }

    @Test
    fun testSpin() {
        val x: Euclidean2DPosition = environment.getHeading(node1)
        assertEquals(Euclidean2DPosition(0.0, 1.0), x)
        for (i in 1..10) {
            val headingAngle = environment.getHeading(node1).asAngle + 18
            environment.setHeading(node1, environment.makePosition(cos(headingAngle), sin(headingAngle)))
        }
    }
}
