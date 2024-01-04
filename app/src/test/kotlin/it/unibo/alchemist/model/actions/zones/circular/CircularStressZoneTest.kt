package it.unibo.alchemist.model.actions.zones.circular

import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.SupportedIncarnations
import it.unibo.alchemist.model.actions.utils.Direction
import it.unibo.alchemist.model.actions.zones.AbstractZoneTest
import it.unibo.alchemist.model.actions.zones.StressZone
import it.unibo.alchemist.model.actions.zones.shapes.ZoneShapeFactoryImpl
import it.unibo.alchemist.model.linkingrules.NoLinks
import it.unibo.alchemist.model.physics.environments.ContinuousPhysics2DEnvironment
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.* // ktlint-disable no-wildcard-imports

class CircularStressZoneTest : AbstractZoneTest() {
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
        const val FORWARD_VELOCITY = 2.0
        const val LATERAL_VELOCITY = 1.0
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

        val zoneShapeFactory = ZoneShapeFactoryImpl(environment.shapeFactory)
        val stressZoneShape = zoneShapeFactory.produceCircleZoneShape(STRESS_ZONE_RADIUS)

        stressZone1 = StressZone(stressZoneShape, node1, environment, movements, REPULSION_FACTOR)
        stressZone2 = StressZone(stressZoneShape.makeCopy(), node2, environment, movements, REPULSION_FACTOR)
        stressZone4 = StressZone(stressZoneShape.makeCopy(), node4, environment, movements, REPULSION_FACTOR)
    }

    @BeforeTest
    fun resetPositions() {
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
        addNode(node2, positionProvider.getNorthInZonePosition())
        assertTrue(stressZone1.areNodesInZone())
        val movement = stressZone1.getNextMovement()
        assertEquals(movements.getValue(Direction.FORWARD).lateralVelocity, movement.lateralVelocity)
        assertEquals(movements.getValue(Direction.FORWARD).forwardVelocity, movement.forwardVelocity + REPULSION_FACTOR * FORWARD_VELOCITY)
    }

    @Test
    fun testLeftAndRightMovementWithNorthHeading() {
        addNode(node2, LEFT_POSITION)

        assertTrue(stressZone1.areNodesInZone())
        assertTrue(stressZone2.areNodesInZone())

        val movement1 = stressZone1.getNextMovement()
        assertEquals(movements.getValue(Direction.RIGHT).lateralVelocity, movement1.lateralVelocity)
        assertEquals(0.0, movement1.forwardVelocity)

        val movement2 = stressZone2.getNextMovement()
        assertEquals(movements.getValue(Direction.LEFT).lateralVelocity, movement2.lateralVelocity)
        assertEquals(0.0, movement2.forwardVelocity)
    }

    @Test
    fun testLeftAndRightMovementWithSouthHeading() {
        addNode(node2, LEFT_POSITION)
        val south = Euclidean2DPosition(0.0, -1.0)
        environment.setHeading(node1, south)
        environment.setHeading(node2, south)

        assertTrue(stressZone1.areNodesInZone())
        assertTrue(stressZone2.areNodesInZone())

        val movement1 = stressZone1.getNextMovement()
        assertEquals(movements.getValue(Direction.LEFT).lateralVelocity, movement1.lateralVelocity)
        assertEquals(0.0, movement1.forwardVelocity)

        val movement2 = stressZone2.getNextMovement()
        assertEquals(movements.getValue(Direction.RIGHT).lateralVelocity, movement2.lateralVelocity)
        assertEquals(0.0, movement2.forwardVelocity)
    }

    @Test
    fun testLeftAndRightMovementWithEastHeading() {
        addNode(node2, positionProvider.getNorthInZonePosition())
        val east = Euclidean2DPosition(1.0, 0.0)
        environment.setHeading(node1, east)
        environment.setHeading(node2, east)

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
        addNode(node2, positionProvider.getSouthEastInZonePosition())
        addNode(node4, positionProvider.getSouthWestInZonePosition())

        assertTrue(stressZone1.areNodesInZone())

        val movement = stressZone1.getNextMovement()
        assertEquals(movements.getValue(Direction.FORWARD).forwardVelocity, movement.forwardVelocity)
        assertEquals(0.0, movement.lateralVelocity)
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
    fun testAngles() {
        val position = Euclidean2DPosition(0.0, -1.0)
        val heading = atan2(position.y, position.x)
        val headingPosition = Euclidean2DPosition(cos(heading), sin(heading))

        val west = Euclidean2DPosition(-0.0, 1.0)
        println("target angle ${west.asAngle}")
        val updatedAngle = (west.asAngle - heading)
        println("Heading angle $heading")
        println("updated angle $updatedAngle")

        println((PI % (2 * PI)) - PI)
        val rotatedVector = rotateVector(headingPosition, updatedAngle)
        assertEquals(west.x, rotatedVector.x, EPSILON)
        assertEquals(west.y, rotatedVector.y, EPSILON)
//        assertEquals(west, rotatedVector)
    }

    @Test
    fun testCirconference() {
        println(positionProvider.generateEquidistantPointsInHalfCircle(1.0, 5, environment.getHeading(node1).asAngle))
    }

    private fun rotateVector(vector: Euclidean2DPosition, angle: Double): Euclidean2DPosition {
        val newX = vector.x * cos(angle) - vector.y * sin(angle)
        val newY = vector.x * sin(angle) + vector.y * cos(angle)
        return environment.makePosition(newX, newY)
    }
}
