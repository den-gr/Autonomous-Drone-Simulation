package it.unibo.alchemist.model.actions.zones.shapes

import it.unibo.alchemist.model.geometry.Euclidean2DShape

interface ZoneShapeFactory<P> {

    fun produceCircleZoneShape(radius: Double): ZoneShape<P>
    fun produceCircularSectorZoneShape(radius: Double, angle: Double, inverseToHeading: Boolean = false): ZoneShape<P>
    fun produceEllipseZoneShape(radius: Double, ratio: Double): ZoneShape<Euclidean2DShape>
}
