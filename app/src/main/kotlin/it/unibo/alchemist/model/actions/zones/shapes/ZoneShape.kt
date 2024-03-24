package it.unibo.alchemist.model.actions.zones.shapes

/**
 * Shape of a node's zone.
 */
interface ZoneShape<T> {
    /**
     * Shape of the zone.
     */
    val shape: T

    /**
     * Allows create a new copy of the zone.
     */
    fun makeCopy(): ZoneShape<T>
}

/**
 * Zone in the shape of an ellipse.
 * @param shape
 * @param radius of the ellipse width
 * @param ratio how many times length of ellipse is bigger and width.
 */
class EllipseZoneShape<T>(
    override val shape: T,
    val radius: Double,
    val ratio: Double = 1.0,
) : ZoneShape<T> {

    override fun makeCopy(): ZoneShape<T> {
        return EllipseZoneShape(shape, radius)
    }
}

/**
 * Zone shaped like a circular segment.
 * @param shape
 * @param radius
 * @param angle of the circular segment.
 * @param direction 1.0 if is same to the heading,
 *                 -1.0 if is inverse (rotated by 180 degrees).
 */
class CircularSegmentZoneShape<T>(
    override val shape: T,
    val radius: Double,
    val angle: Double,
    val direction: Double = 1.0,
) : ZoneShape<T> {

    override fun makeCopy(): ZoneShape<T> {
        return CircularSegmentZoneShape(shape, radius, angle)
    }
}

/**
 * Original rectangle zone shape. Not used anymore. Can be removed from the codebase.
 * @param shape
 * @param width of rectangle.
 * @param height of rectangle.
 */
class RectangularZoneShape<T>(
    override val shape: T,
    private val width: Double,
    private val height: Double,
) : ZoneShape<T> {

    override fun makeCopy(): ZoneShape<T> {
        return RectangularZoneShape(shape, width, height)
    }
}
