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
import kotlin.random.Random
import kotlin.test.* // ktlint-disable no-wildcard-imports

class RearZoneTest : AbstractZoneTest() {
    override lateinit var environment: Physics2DEnvironment<Any>
    private lateinit var node1: Node<Any>
    private lateinit var node2: Node<Any>
    private lateinit var rearZone: RearZone
    private val positionProvider = CircularPositionProvider(
        REAR_ZONE_RADIUS,
        BODY_LEN,
    )

    companion object {
        const val REAR_ZONE_RADIUS = 12 * BODY_LEN
        const val REAR_ZONE_ANGLE = 180.0
        const val SLOW_DOWN_FACTOR = 0.5
        const val SLOW_DOWN_PROBABILITY = 0.6
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
        val rearZoneShape = zoneShapeFactory.produceCircularSectorZoneShape(
            REAR_ZONE_RADIUS,
            REAR_ZONE_ANGLE,
            true,
        )
        val sameHerdPredicate: (Int) -> Boolean = { _ -> true }
        rearZone = RearZone(
            rearZoneShape,
            node1,
            environment,
            movementsProvider,
            SLOW_DOWN_FACTOR,
            SLOW_DOWN_PROBABILITY,
            sameHerdPredicate,
            Random(1),
        )
    }

    @BeforeEach
    fun resetPositions() {
        setDefaultHeading(node1)
        environment.removeNode(node2)
        setPositionAndVerifySetting(node1, CENTER_POSITION)
        assertFalse(rearZone.areNodesInZone())
    }

    @Test
    fun testOutZoneNorth() {
        val points = positionProvider.generateEquidistantPointsInHalfCircle(
            REAR_ZONE_RADIUS / 2,
            10,
            environment.getHeading(node1).asAngle,
        )
        addNode(node2, points.first())
        for (p in points) {
            setPositionAndVerifySetting(node2, p.plus(Euclidean2DPosition(0.0, BODY_LEN + EPSILON)))
            assertFalse(rearZone.areNodesInZone())
        }
    }

    @Test
    fun testInZoneSouth() {
        val a = environment.getHeading(node1).asAngle
        val inverseHeading = if (PI >= 0) a - PI else a + PI

        val points = positionProvider.generateEquidistantPointsInHalfCircle(
            REAR_ZONE_RADIUS / 2,
            10,
            inverseHeading,
        )
        addNode(node2, points.first())
        for (p in points) {
            setPositionAndVerifySetting(node2, p)
            assertTrue(rearZone.areNodesInZone())
        }
    }
}
