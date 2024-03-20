package it.unibo.alchemist.model.actions.zones.shapes

/**
 * Factory that create shapes of zones.
 */
interface ZoneShapeFactory<P> {

    /**
     * @param radius of the circle
     * @return complete circle shape
     */
    fun produceCircleZoneShape(radius: Double): ZoneShape<P>

    /**
     * @param radius
     * @param angle
     * @param inverseToHeading
     * @return Segment of circle shape.
     */
    fun produceCircularSectorZoneShape(radius: Double, angle: Double, inverseToHeading: Boolean = false): ZoneShape<P>

    /**
     * @param radius base width radius
     * @param ratio how many times the length of ellipse should be bigger than radius (width)
     * @return Ellipse zone shape
     */
    fun produceEllipseZoneShape(radius: Double, ratio: Double): ZoneShape<P>
}
