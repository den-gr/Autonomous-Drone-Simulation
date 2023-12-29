package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.SupportedIncarnations
import it.unibo.alchemist.model.actions.zones.shapes.ZoneShapeFactoryImpl
import it.unibo.alchemist.model.actions.zones.shapes.ZoneType
import it.unibo.alchemist.model.linkingrules.NoLinks
import it.unibo.alchemist.model.physics.environments.ContinuousPhysics2DEnvironment
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import org.junit.jupiter.api.BeforeEach
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.* // ktlint-disable no-wildcard-imports

class NeutralZoneTest : AbstractZoneTest() {
    override lateinit var environment: Physics2DEnvironment<Any>
    private lateinit var node1: Node<Any>
    private lateinit var node2: Node<Any>
    private lateinit var neutralZone1: NeutralZone
    private val positionProvider: PositionProvider<Euclidean2DPosition> = PositionProviderImpl(
        BODY_LEN,
        NEUTRAL_ZONE_WIDTH,
        NEUTRAL_ZONE_HEIGHT,
    )

    companion object {
        private const val NEUTRAL_ZONE_WIDTH_FACTOR = 6
        private const val NEUTRAL_ZONE_HEIGHT_FACTOR = 12

        const val NEUTRAL_ZONE_WIDTH = NEUTRAL_ZONE_WIDTH_FACTOR.toDouble() * BODY_LEN * 2
        const val NEUTRAL_ZONE_HEIGHT = NEUTRAL_ZONE_HEIGHT_FACTOR.toDouble() * BODY_LEN
//        val CIAONE: Int = 10
    }

    @BeforeTest
    fun beforeTest() {
        val incarnation = SupportedIncarnations.get<Any, Euclidean2DPosition>("protelis").orElseThrow()
        environment = ContinuousPhysics2DEnvironment(incarnation)
        environment.linkingRule = NoLinks()
        node1 = createRectangleNode(incarnation, environment, BODY_WIDTH, BODY_LEN)
        node2 = createRectangleNode(incarnation, environment, BODY_WIDTH, BODY_LEN)
        environment.addNode(node1, positionProvider.getNextExternalPosition())
        environment.addNode(node2, positionProvider.getNextExternalPosition())

        val zoneShapeFactory = ZoneShapeFactoryImpl(environment.shapeFactory)
        val neutralZoneShape = zoneShapeFactory.produceRectangularZoneShape(
            NEUTRAL_ZONE_WIDTH,
            NEUTRAL_ZONE_HEIGHT,
            ZoneType.FRONT,
        )

        neutralZone1 = NeutralZone(neutralZoneShape, node1, environment, movements)
    }

    private fun setHeading(node: Node<Any>) {
        environment.setHeading(node, Euclidean2DPosition(0.0, 1.0))
    }

    @BeforeEach
    fun resetPositions() {
        setPositionAndVerifySetting(node1, CENTER_POSITION)
        environment.moveNodeToPosition(node2, positionProvider.getNextExternalPosition())
        assertFalse(neutralZone1.areNodesInZone())
    }

    @Test
    fun testSpin() {
        setPositionAndVerifySetting(node1, Euclidean2DPosition(0.0, 10.0))
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
        setPositionAndVerifySetting(node2, positionProvider.getNorthWestInZonePosition())
        assertTrue(neutralZone1.areNodesInZone())

        val movement = neutralZone1.getNextMovement()
        assertTrue(movement.lateralVelocity < 0)
    }

    @Test
    fun testNeutralZoneRightForwardDetection() {
        setPositionAndVerifySetting(node2, positionProvider.getNorthEastInZonePosition())
        assertTrue(neutralZone1.areNodesInZone())

        val movement = neutralZone1.getNextMovement()
        assertTrue(movement.lateralVelocity > 0)
    }

    @Test
    fun testForwardOutOfZone() {
        setPositionAndVerifySetting(node2, positionProvider.getNorthEastOutZonePosition())
        assertFalse(neutralZone1.areNodesInZone())

        setPositionAndVerifySetting(node2, positionProvider.getNorthWestOutZonePosition())
        assertFalse(neutralZone1.areNodesInZone())
    }

    @Test
    fun testBehindOutOfZone() {
        setPositionAndVerifySetting(node2, positionProvider.getSouthEastInZonePosition())
        assertFalse(neutralZone1.areNodesInZone())

        setPositionAndVerifySetting(node2, positionProvider.getSouthWestInZonePosition())
        assertFalse(neutralZone1.areNodesInZone())
    }
}
