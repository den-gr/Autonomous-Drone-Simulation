package it.unibo.alchemist.model.actions.zones.shapes

import it.unibo.alchemist.model.geometry.Euclidean2DShape

interface ZoneShapeFactory<P> {
    /**
     * Produce a rectangular [ZoneShape]
     * @param width of rectangle
     * @param height of rectangle
     * @param zoneType defines if zone is circular or only rear/front
     * @return Rectangular zone shape
     */
    fun produceRectangularZoneShape(width: Double, height: Double, zoneType: ZoneType): ZoneShape<P>

    fun produceCircleZoneShape(radius: Double): ZoneShape<P>

    fun produceCircularSectorZoneShape(radius: Double, angle: Double, inverseToHeading: Boolean = false): ZoneShape<P>
    fun produceEllipseZoneShape(radius: Double, ratio: Double): ZoneShape<Euclidean2DShape>
}
