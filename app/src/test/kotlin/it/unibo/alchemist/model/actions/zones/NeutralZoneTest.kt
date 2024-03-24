package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.SupportedIncarnations
import it.unibo.alchemist.model.actions.zones.shapes.ZoneShapeFactoryImpl
import it.unibo.alchemist.model.linkingrules.NoLinks
import it.unibo.alchemist.model.physics.environments.ContinuousPhysics2DEnvironment
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import org.junit.jupiter.api.BeforeEach
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.* // ktlint-disable no-wildcard-imports

class NeutralZoneTest : AbstractZoneTest() {
    override lateinit var environment: Physics2DEnvironment<Any>
    private lateinit var node1: Node<Any>
    private lateinit var node2: Node<Any>
    private lateinit var neutralZone1: NeutralZone
    private lateinit var neutralZone2: NeutralZone
    private val positionProvider = CircularPositionProvider(
        NEUTRAL_ZONE_RADIUS,
        BODY_LEN,
    )

    companion object {
        const val NEUTRAL_ZONE_RADIUS = 12 * BODY_LEN
        const val NEUTRAL_ZONE_ANGLE = 180.0
    }

    @BeforeTest
    fun beforeTest() {
        val incarnation = SupportedIncarnations.get<Any, Euclidean2DPosition>("protelis").orElseThrow()
        environment = ContinuousPhysics2DEnvironment(incarnation)
        environment.linkingRule = NoLinks()
        node1 = createRectangleNode(incarnation, environment, BODY_LEN, BODY_WIDTH)
        node2 = createRectangleNode(incarnation, environment, BODY_LEN, BODY_WIDTH)
        environment.addNode(node1, positionProvider.getNextExternalPosition())
        environment.addNode(node2, positionProvider.getNextExternalPosition())

        val zoneShapeFactory = ZoneShapeFactoryImpl(environment.shapeFactory)
        val neutralZoneShape = zoneShapeFactory.produceCircularSectorZoneShape(
            NEUTRAL_ZONE_RADIUS,
            NEUTRAL_ZONE_ANGLE,
        )

        val sameHerdPredicate: (Int) -> Boolean = { _ -> true }
        neutralZone1 =
            NeutralZone(neutralZoneShape, node1, environment, movementsProvider, sameHerdPredicate)
        neutralZone2 =
            NeutralZone(neutralZoneShape.makeCopy(), node2, environment, movementsProvider, sameHerdPredicate)
    }

    @BeforeEach
    fun resetPositions() {
        setDefaultHeading(node1)
        environment.removeNode(node2)
        setPositionAndVerifySetting(node1, CENTER_POSITION)
        assertFalse(neutralZone1.areNodesInZone())
    }

    @Test
    fun testNeutralZoneLeftForwardDetection() {
        addNode(node2, positionProvider.getNorthWestInZonePosition())
        assertTrue(neutralZone1.areNodesInZone())

        val movement = neutralZone1.getNextMovement()
        assertTrue(movement.x < 0)
    }

    @Test
    fun testNeutralZoneRightForwardDetection() {
        addNode(node2, positionProvider.getNorthEastInZonePosition())
        assertTrue(neutralZone1.areNodesInZone())

        val movement = neutralZone1.getNextMovement()
        assertTrue(movement.x > 0)
    }

    @Test
    fun testOutOfZone() {
        val points = positionProvider.getPointsInRadius(NEUTRAL_ZONE_RADIUS + BODY_LEN + EPSILON)
        addNode(node2, points.first())
        for (p in points) {
            setPositionAndVerifySetting(node2, p)
            assertFalse(neutralZone1.areNodesInZone())
            assertFalse(neutralZone2.areNodesInZone())
        }
    }

    @Test
    fun testInZoneNorth() {
        val points = positionProvider.generateEquidistantPointsInHalfCircle(
            NEUTRAL_ZONE_RADIUS - EPSILON,
            10,
            environment.getHeading(node1).asAngle,
        )
        addNode(node2, points.first())
        for (p in points) {
            setPositionAndVerifySetting(node2, p)
            assertTrue(neutralZone1.areNodesInZone())
        }
    }

    @Test
    fun testOutZoneSouth() {
        val a = environment.getHeading(node1).asAngle
        val inverseHeading = if (PI >= 0) a - PI else a + PI

        val points = positionProvider.generateEquidistantPointsInHalfCircle(
            NEUTRAL_ZONE_RADIUS / 2,
            10,
            inverseHeading,
        )
        addNode(node2, points.first())
        for (p in points) {
            val p2 = p.minus(Euclidean2DPosition(0.0, BODY_LEN / 2))
            setPositionAndVerifySetting(node2, p2)
            assertFalse(neutralZone1.areNodesInZone())
        }
    }

    @Test
    fun testSpin() {
        setPositionAndVerifySetting(node1, Euclidean2DPosition(0.0, 10.0))
        val x: Euclidean2DPosition = environment.getHeading(node1)
        assertEquals(Euclidean2DPosition(0.0, 1.0), x)
        repeat(10) {
            val headingAngle = environment.getHeading(node1).asAngle + 18
            environment.setHeading(node1, environment.makePosition(cos(headingAngle), sin(headingAngle)))
            val heading = environment.getHeading(node1)
            assertEquals(1.0, sqrt(heading.x * heading.x + heading.y * heading.y), 0.001)
        }
    }
}
