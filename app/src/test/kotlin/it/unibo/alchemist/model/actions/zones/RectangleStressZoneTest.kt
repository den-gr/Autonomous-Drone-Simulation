package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.SupportedIncarnations
import it.unibo.alchemist.model.actions.utils.Direction
import it.unibo.alchemist.model.actions.zones.shapes.ZoneShapeFactoryImpl
import it.unibo.alchemist.model.actions.zones.shapes.ZoneType
import it.unibo.alchemist.model.linkingrules.NoLinks
import it.unibo.alchemist.model.physics.environments.ContinuousPhysics2DEnvironment
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import kotlin.test.* // ktlint-disable no-wildcard-imports

class RectangleStressZoneTest : AbstractZoneTest() {
    override lateinit var environment: Physics2DEnvironment<Any>
    private lateinit var node1: Node<Any>
    private lateinit var node2: Node<Any>
    private lateinit var node4: Node<Any>
    private lateinit var stressZone1: StressZone
    private lateinit var stressZone2: StressZone
    private lateinit var stressZone4: StressZone
    private val positionProvider: PositionProvider<Euclidean2DPosition> = RectangularPositionProviderImpl(
        BODY_LEN,
        STRESS_ZONE_WIDTH,
        STRESS_ZONE_HEIGHT,
    )

    companion object {
        const val STRESS_ZONE_WIDTH = 10.0 * BODY_LEN
        const val STRESS_ZONE_HEIGHT = 10.0 * BODY_LEN
        const val REPULSION_FACTOR = 0.5
        const val FORWARD_VELOCITY = 2.0
        const val LATERAL_VELOCITY = 1.0
        val LEFT_POSITION = Euclidean2DPosition(-2.0, 0.0)
    }

    @BeforeTest
    fun beforeTest() {
        val incarnation = SupportedIncarnations.get<Any, Euclidean2DPosition>("protelis").orElseThrow()
        environment = ContinuousPhysics2DEnvironment(incarnation)
        environment.linkingRule = NoLinks()
        node1 = createRectangleNode(incarnation, environment, BODY_WIDTH, BODY_LEN)
        node2 = createRectangleNode(incarnation, environment, BODY_WIDTH, BODY_LEN)
        node4 = createRectangleNode(incarnation, environment, BODY_WIDTH, BODY_LEN)
        environment.addNode(node1, positionProvider.getNextExternalPosition())
        environment.addNode(node2, positionProvider.getNextExternalPosition())
        environment.addNode(node4, positionProvider.getNextExternalPosition())

        val zoneShapeFactory = ZoneShapeFactoryImpl(environment.shapeFactory)
        val stressZoneShape = zoneShapeFactory.produceRectangularZoneShape(
            STRESS_ZONE_WIDTH,
            STRESS_ZONE_HEIGHT,
            ZoneType.FRONT_AND_REAR,
        )

        stressZone1 = StressZone(stressZoneShape, node1, environment, movements, REPULSION_FACTOR)
        stressZone2 = StressZone(stressZoneShape.makeCopy(), node2, environment, movements, REPULSION_FACTOR)
        stressZone4 = StressZone(stressZoneShape.makeCopy(), node4, environment, movements, REPULSION_FACTOR)
    }

    @BeforeTest
    fun resetPositions() {
        setPositionAndVerifySetting(node1, CENTER_POSITION)
        environment.moveNodeToPosition(node2, positionProvider.getNextExternalPosition())
        environment.moveNodeToPosition(node4, positionProvider.getNextExternalPosition())
        assertFalse(stressZone1.areNodesInZone())
    }

    @Test
    fun testStressZoneDetection() {
        setPositionAndVerifySetting(node2, positionProvider.getNorthEastInZonePosition())
        assertTrue(stressZone1.areNodesInZone())
        assertTrue(stressZone2.areNodesInZone())
        assertFalse(stressZone4.areNodesInZone())
    }

    @Test
    fun testSlowDownMovement() {
        setPositionAndVerifySetting(node2, positionProvider.getNorthInZonePosition())
        assertTrue(stressZone1.areNodesInZone())
        val movement = stressZone1.getNextMovement()
        assertEquals(movements.getValue(Direction.FORWARD).lateralVelocity, movement.lateralVelocity)
        assertEquals(movements.getValue(Direction.FORWARD).forwardVelocity, movement.forwardVelocity + REPULSION_FACTOR * FORWARD_VELOCITY)
    }

    @Test
    fun testLeftAndRightMovement() {
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

    /**
     * Verify that the nodes positioned behind in the both sides force the node to move forward
     */
    @Test
    fun testForwardMovement() {
        environment.removeNode(node2)
        environment.removeNode(node4)
        addNode(node2, positionProvider.getSouthEastInZonePosition())
        addNode(node4, positionProvider.getSouthWestInZonePosition())

        assertTrue(stressZone1.areNodesInZone())

        val movement = stressZone1.getNextMovement()
        assertEquals(movements.getValue(Direction.FORWARD).forwardVelocity, movement.forwardVelocity)
        assertEquals(0.0, movement.lateralVelocity)
    }
}
