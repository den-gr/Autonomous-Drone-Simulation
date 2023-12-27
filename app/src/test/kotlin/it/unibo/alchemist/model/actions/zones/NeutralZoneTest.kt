package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.SupportedIncarnations
import it.unibo.alchemist.model.linkingrules.NoLinks
import it.unibo.alchemist.model.physics.environments.ContinuousPhysics2DEnvironment
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import org.junit.jupiter.api.BeforeEach
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.* // ktlint-disable no-wildcard-imports

class NeutralZoneTest : AbstractZoneTest() {
    override lateinit var environment: Physics2DEnvironment<Any>
    private lateinit var node1: Node<Any>
    private lateinit var node2: Node<Any>
    private lateinit var node3: Node<Any>
    private lateinit var neutralZone1: NeutralZone

    companion object {
        private const val BODY_LEN = 1.0
        const val NEUTRAL_ZONE_WIDTH_FACTOR = 6
        const val NEUTRAL_ZONE_HEIGHT_FACTOR = 12
        val CENTER_POSITION = Euclidean2DPosition(0.0, 0.0)
        val FORWARD_RIGHT_IN_ZONE_POSITION = Euclidean2DPosition(
            round(NEUTRAL_ZONE_WIDTH_FACTOR.toDouble() / 2),
            round(NEUTRAL_ZONE_HEIGHT_FACTOR.toDouble() / 2),
        )

        val FORWARD_RIGHT_OUT_ZONE_POSITION = Euclidean2DPosition(
            NEUTRAL_ZONE_WIDTH_FACTOR.toDouble() + BODY_LEN,
            NEUTRAL_ZONE_HEIGHT_FACTOR.toDouble() + 0.1,
        )

        val FORWARD_LEFT_OUT_ZONE_POSITION = Euclidean2DPosition(
            -NEUTRAL_ZONE_WIDTH_FACTOR.toDouble() - BODY_LEN,
            NEUTRAL_ZONE_HEIGHT_FACTOR.toDouble() + 0.1,
        )

        val FORWARD_LEFT_IN_ZONE_POSITION = Euclidean2DPosition(
            -round(NEUTRAL_ZONE_WIDTH_FACTOR.toDouble() / 2),
            round(NEUTRAL_ZONE_HEIGHT_FACTOR.toDouble() / 2),
        )

        val BEHIND_LEFT_OUT_ZONE_POSITION = Euclidean2DPosition(
            -round(NEUTRAL_ZONE_WIDTH_FACTOR.toDouble() / 2),
            -round(NEUTRAL_ZONE_HEIGHT_FACTOR.toDouble() / 2),
        )

        val BEHIND_RIGHT_OUT_ZONE_POSITION = Euclidean2DPosition(
            round(NEUTRAL_ZONE_WIDTH_FACTOR.toDouble() / 2),
            -round(NEUTRAL_ZONE_HEIGHT_FACTOR.toDouble() / 2),
        )

        var count = 9999.0

        fun nextOutPosition(): Euclidean2DPosition {
            count++
            return Euclidean2DPosition(9999.0, 9999.0 + count * BODY_LEN + 0.01)
        }
    }

    @BeforeTest
    fun beforeTest() {
        val width = 0.5
        val incarnation = SupportedIncarnations.get<Any, Euclidean2DPosition>("protelis").orElseThrow()
        environment = ContinuousPhysics2DEnvironment(incarnation)
        environment.linkingRule = NoLinks()
        node1 = createRectangleNode(incarnation, environment, width, BODY_LEN)
        node2 = createRectangleNode(incarnation, environment, width, BODY_LEN)
        node3 = createRectangleNode(incarnation, environment, width, BODY_LEN)
        environment.addNode(node1, nextOutPosition())
        environment.addNode(node2, nextOutPosition())
        environment.addNode(node3, nextOutPosition())
        neutralZone1 = NeutralZone(node1.id, environment, movements, NEUTRAL_ZONE_WIDTH_FACTOR * BODY_LEN, NEUTRAL_ZONE_HEIGHT_FACTOR * BODY_LEN)
    }

    private fun setHeading(node: Node<Any>) {
        environment.setHeading(node, Euclidean2DPosition(0.0, 1.0))
    }

    @BeforeEach
    fun resetPositions() {
        setPositionAndVerifySetting(node1, CENTER_POSITION)
        assertFalse(neutralZone1.areNodesInZone())
        environment.moveNodeToPosition(node2, nextOutPosition())
        environment.moveNodeToPosition(node3, nextOutPosition())
    }

    @Test
    fun testSpin() {
        val x: Euclidean2DPosition = environment.getHeading(node1)
        assertEquals(Euclidean2DPosition(0.0, 1.0), x)
        for (i in 1..10) {
            val headingAngle = environment.getHeading(node1).asAngle + 18
            environment.setHeading(node1, environment.makePosition(cos(headingAngle), sin(headingAngle)))
            val heading = environment.getHeading(node1)
            assertEquals(1.0, sqrt(heading.x * heading.x + heading.y * heading.y), 0.001)
        }
    }

    @Test
    fun testNeutralZoneLeftForwardDetection() {
        setPositionAndVerifySetting(node2, FORWARD_LEFT_IN_ZONE_POSITION)
        assertTrue(neutralZone1.areNodesInZone())

        val movement = neutralZone1.getNextMovement()
        assertTrue(movement.lateralVelocity > 0)
    }

    @Test
    fun testNeutralZoneRightForwardDetection() {
        setPositionAndVerifySetting(node2, FORWARD_RIGHT_IN_ZONE_POSITION)
        assertTrue(neutralZone1.areNodesInZone())

        val movement = neutralZone1.getNextMovement()
        assertTrue(movement.lateralVelocity < 0)
    }

    @Test
    fun testForwardOutOfZone() {
        setPositionAndVerifySetting(node2, FORWARD_RIGHT_OUT_ZONE_POSITION)
        assertFalse(neutralZone1.areNodesInZone())

        setPositionAndVerifySetting(node2, FORWARD_LEFT_OUT_ZONE_POSITION)
        assertFalse(neutralZone1.areNodesInZone())
    }

    @Test
    fun testBehindOutOfZone() {
        setPositionAndVerifySetting(node2, BEHIND_RIGHT_OUT_ZONE_POSITION)
        assertFalse(neutralZone1.areNodesInZone())

        setPositionAndVerifySetting(node2, BEHIND_LEFT_OUT_ZONE_POSITION)
        assertFalse(neutralZone1.areNodesInZone())
    }
}
