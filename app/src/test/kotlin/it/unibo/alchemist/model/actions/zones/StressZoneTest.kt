package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.SupportedIncarnations
import it.unibo.alchemist.model.actions.zones.shapes.ZoneShapeFactoryImpl
import it.unibo.alchemist.model.linkingrules.NoLinks
import it.unibo.alchemist.model.physics.environments.ContinuousPhysics2DEnvironment
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import kotlin.test.* // ktlint-disable no-wildcard-imports

class StressZoneTest : AbstractZoneTest() {
    override lateinit var environment: Physics2DEnvironment<Any>
    private lateinit var node1: Node<Any>
    private lateinit var node2: Node<Any>
    private lateinit var node4: Node<Any>
    private lateinit var stressZone1: StressZone
    private lateinit var stressZone2: StressZone
    private lateinit var stressZone4: StressZone
    private val positionProvider: CircularPositionProvider = CircularPositionProvider(
        STRESS_ZONE_RADIUS,
        BODY_LEN,
    )

    companion object {
        const val STRESS_ZONE_RADIUS = 8.0 * BODY_LEN
        const val REPULSION_FACTOR = 0.5

        val LEFT_POSITION = Euclidean2DPosition(-2.0, 0.0)
        const val EPSILON = 0.0001
    }

    @BeforeTest
    fun beforeTest() {
        val incarnation = SupportedIncarnations.get<Any, Euclidean2DPosition>("protelis").orElseThrow()
        environment = ContinuousPhysics2DEnvironment(incarnation)
        environment.linkingRule = NoLinks()
        node1 = createRectangleNode(incarnation, environment, BODY_WIDTH, BODY_LEN)
        node2 = createRectangleNode(incarnation, environment, BODY_WIDTH, BODY_LEN)
        node4 = createRectangleNode(incarnation, environment, BODY_WIDTH, BODY_LEN)
        environment.addNode(node1, CENTER_POSITION)
        environment.addNode(node2, positionProvider.getNextExternalPosition())
        environment.addNode(node4, positionProvider.getNextExternalPosition())
    }

    @BeforeTest
    fun resetPositions() {
        val zoneShapeFactory = ZoneShapeFactoryImpl(environment.shapeFactory)
        val stressZoneShape = zoneShapeFactory.produceCircleZoneShape(STRESS_ZONE_RADIUS)

        stressZone1 = StressZone(
            stressZoneShape,
            node1,
            environment,
            getNewMovementProvider(node1.id),
            REPULSION_FACTOR,
        )
        stressZone2 = StressZone(
            stressZoneShape.makeCopy(),
            node2,
            environment,
            getNewMovementProvider(node2.id),
            REPULSION_FACTOR,
        )
        stressZone4 = StressZone(
            stressZoneShape.makeCopy(),
            node4,
            environment,
            getNewMovementProvider(node4.id),
            REPULSION_FACTOR,
        )

        environment.removeNode(node4)
        environment.removeNode(node2)
        setPositionAndVerifySetting(node1, CENTER_POSITION)
        setDefaultHeading(node1)
        assertFalse(stressZone1.areNodesInZone())
    }

    @Test
    fun testStressZoneDetection() {
        addNode(node2, positionProvider.getNorthEastInZonePosition())
        addNode(node4, positionProvider.getNextExternalPosition())
        assertTrue(stressZone1.areNodesInZone())
        assertTrue(stressZone2.areNodesInZone())
        assertFalse(stressZone4.areNodesInZone())
    }

    @Test
    fun testSlowDownMovement() {
        val randomMovement = getNewMovementProvider(node1.id).getRandomMovement()
        addNode(node2, positionProvider.getNorthInZonePosition())
        assertTrue(stressZone1.areNodesInZone())
        val movement = stressZone1.getNextMovement()

        assertEquals(
            randomMovement.addVelocityModifier(0.0, -REPULSION_FACTOR).y,
            movement.y,
        )
    }

    @Test
    fun testLeftAndRightMovementWithNorthHeading() {
        val randomMovement1 = getNewMovementProvider(node1.id).getRandomMovement()
        val randomMovement2 = getNewMovementProvider(node2.id).getRandomMovement()
        addNode(node2, LEFT_POSITION)

        assertTrue(stressZone1.areNodesInZone())
        assertTrue(stressZone2.areNodesInZone())

        val movement1 = stressZone1.getNextMovement()
        assertEquals(movementsProvider.toRight().x + randomMovement1.x, movement1.x)
        assertEquals(randomMovement1.y, movement1.y)

        val movement2 = stressZone2.getNextMovement()
        assertEquals(movementsProvider.toLeft().x + randomMovement2.x, movement2.x)
    }

    @Test
    fun testLeftAndRightMovementWithSouthHeading() {
        val randomMovement1 = getNewMovementProvider(node1.id).getRandomMovement()
        val randomMovement2 = getNewMovementProvider(node2.id).getRandomMovement()
        addNode(node2, LEFT_POSITION)
        val south = Euclidean2DPosition(0.0, -1.0)
        environment.setHeading(node1, south)
        environment.setHeading(node2, south)

        assertTrue(stressZone1.areNodesInZone())
        assertTrue(stressZone2.areNodesInZone())

        val movement1 = stressZone1.getNextMovement()
        assertEquals(movementsProvider.toLeft().x + randomMovement1.x, movement1.x)
        assertEquals(randomMovement1.y, movement1.y)

        val movement2 = stressZone2.getNextMovement()
        assertEquals(movementsProvider.toRight().x + randomMovement2.x, movement2.x)
//        assertEquals(randomMovement2.y, movement2.y)
    }

    @Test
    fun testLeftAndRightMovementWithEastHeading() {
        val randomMovement1 = getNewMovementProvider(node1.id).getRandomMovement()
        val randomMovement2 = getNewMovementProvider(node2.id).getRandomMovement()
        addNode(node2, positionProvider.getNorthInZonePosition())
        val east = Euclidean2DPosition(1.0, 0.0)
        environment.setHeading(node1, east)
        environment.setHeading(node2, east)

        assertTrue(stressZone1.areNodesInZone())
        assertTrue(stressZone2.areNodesInZone())

        val movement1 = stressZone1.getNextMovement()
        assertEquals(movementsProvider.toRight().x + randomMovement1.x, movement1.x)
        assertEquals(randomMovement1.y, movement1.y)

        val movement2 = stressZone2.getNextMovement()
        assertEquals(movementsProvider.toLeft().x + randomMovement2.x, movement2.x)
//        assertEquals(randomMovement2.y, movement2.y)
    }

    /**
     * Verify that the nodes positioned behind in the both sides force the node to move forward.
     */
    @Test
    fun testForwardMovement() {
        val randomMovement1 = getNewMovementProvider(node1.id).getRandomMovement()
        addNode(node2, positionProvider.getSouthEastInZonePosition())
        addNode(node4, positionProvider.getSouthWestInZonePosition())

        assertTrue(stressZone1.areNodesInZone())

        val movement = stressZone1.getNextMovement()
        assertEquals(
            (movementsProvider.forward() + randomMovement1)
                .addVelocityModifier(0.0, REPULSION_FACTOR).y,
            movement.y,
        )
        assertEquals(randomMovement1.x, movement.x)
    }

    @Test
    fun testInZoneCircular() {
        val points = positionProvider.getPointsInRadius(STRESS_ZONE_RADIUS)

        addNode(node2, points.first())
        for (p in points) {
            setPositionAndVerifySetting(node2, p)
            assertTrue(stressZone1.areNodesInZone())
            assertTrue(stressZone2.areNodesInZone())
        }
    }

    @Test
    fun testOutZoneCircular() {
        val points = positionProvider.getPointsInRadius(STRESS_ZONE_RADIUS + BODY_LEN + 0.01)
        addNode(node2, points.first())
        for (p in points) {
            setPositionAndVerifySetting(node2, p)
            assertFalse(stressZone1.areNodesInZone())
            assertFalse(stressZone2.areNodesInZone())
        }
    }

    @Test
    fun testCircumference() {
        println(positionProvider.generateEquidistantPointsInHalfCircle(1.0, 5, environment.getHeading(node1).asAngle))
    }
}
