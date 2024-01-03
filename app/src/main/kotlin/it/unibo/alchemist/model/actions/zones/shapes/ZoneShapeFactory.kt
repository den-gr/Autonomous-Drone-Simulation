package it.unibo.alchemist.model.actions.zones.shapes

interface ZoneShapeFactory<T, P> {
    /**
     * Produce a rectangular [ZoneShape]
     * @param width of rectangle
     * @param height of rectangle
     * @param zoneType defines if zone is circular or only rear/front
     * @return Rectangular zone shape
     */
    fun produceRectangularZoneShape(width: T, height: T, zoneType: ZoneType): ZoneShape<P>

    fun produceCircleZoneShape(radius: T): ZoneShape<P>

    fun produceCircularSectorZoneShape(radius: T, angle: T): ZoneShape<P>
}
