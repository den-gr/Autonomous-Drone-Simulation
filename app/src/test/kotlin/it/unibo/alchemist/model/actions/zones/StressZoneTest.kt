package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.SupportedIncarnations
import it.unibo.alchemist.model.actions.utils.Direction
import it.unibo.alchemist.model.linkingrules.NoLinks
import it.unibo.alchemist.model.physics.environments.ContinuousPhysics2DEnvironment
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import kotlin.test.* // ktlint-disable no-wildcard-imports

class StressZoneTest : AbstractZoneTest() {
    override lateinit var environment: Physics2DEnvironment<Any>
    private lateinit var node1: Node<Any>
    private lateinit var node2: Node<Any>
    private lateinit var node3: Node<Any>
    private lateinit var node4: Node<Any>
    private lateinit var stressZone1: StressZone
    private lateinit var stressZone2: StressZone
    private lateinit var stressZone4: StressZone

    companion object {
        const val REPULSION_FACTOR = 0.5
        const val FORWARD_VELOCITY = 2.0
        const val LATERAL_VELOCITY = 1.0
        val LEFT_POSITION = Euclidean2DPosition(-2.0, 0.0)
        val FORWARD_POSITION = Euclidean2DPosition(0.0, 4.0)
    }

    @BeforeTest
    fun beforeTest() {
        val bodyLen = 1.0
        val width = bodyLen / 2.0
        val incarnation = SupportedIncarnations.get<Any, Euclidean2DPosition>("protelis").orElseThrow()
        environment = ContinuousPhysics2DEnvironment(incarnation)
        environment.linkingRule = NoLinks()
        node1 = createRectangleNode(incarnation, environment, width, bodyLen)
        node2 = createRectangleNode(incarnation, environment, width, bodyLen)
        node3 = createRectangleNode(incarnation, environment, width, bodyLen)
        node4 = createRectangleNode(incarnation, environment, width, bodyLen)
        environment.addNode(node1, Euclidean2DPosition(0.0, 0.0))
        environment.addNode(node2, Euclidean2DPosition(1.0, 2.0))
        environment.addNode(node3, Euclidean2DPosition(999.0, 999.0))
        environment.addNode(node4, Euclidean2DPosition(10.0, 10.0))
        stressZone1 = StressZone(node1.id, environment, movements, 2 * bodyLen, 4 * bodyLen, REPULSION_FACTOR)
        stressZone2 = StressZone(node2.id, environment, movements, 2 * bodyLen, 4 * bodyLen, REPULSION_FACTOR)
        stressZone4 = StressZone(node4.id, environment, movements, 2 * bodyLen, 4 * bodyLen, REPULSION_FACTOR)
    }

    @Test
    fun testStressZoneDetection() {
        assertTrue(stressZone1.areNodesInZone())
        assertFalse(stressZone4.areNodesInZone())
    }

    @Test
    fun testStressZonesSlowDownMovement() {
        assertTrue(stressZone1.areNodesInZone())
        val movement = stressZone1.getNextMovement()
        assertEquals(movements.getValue(Direction.FORWARD).lateralVelocity, movement.lateralVelocity)
        assertEquals(movements.getValue(Direction.FORWARD).forwardVelocity, movement.forwardVelocity + REPULSION_FACTOR * FORWARD_VELOCITY)
    }

    @Test
    fun testStressZoneLeftAndRightMovement() {
        setPositionAndVerifySetting(node2, LEFT_POSITION)

        assertTrue(stressZone1.areNodesInZone())
        assertTrue(stressZone2.areNodesInZone())

        val movement1 = stressZone1.getNextMovement()
        assertEquals(movements.getValue(Direction.RIGHT).lateralVelocity, movement1.lateralVelocity)
        assertEquals(0.0, movement1.forwardVelocity)

        val movement2 = stressZone2.getNextMovement()
        assertEquals(movements.getValue(Direction.LEFT).lateralVelocity, movement2.lateralVelocity)
        assertEquals(0.0, movement2.forwardVelocity)
    }
}
