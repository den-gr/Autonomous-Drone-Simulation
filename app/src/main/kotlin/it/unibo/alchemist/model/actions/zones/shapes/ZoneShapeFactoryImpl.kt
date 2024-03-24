package it.unibo.alchemist.model.actions.zones.shapes

import it.unibo.alchemist.model.geometry.Euclidean2DShape
import it.unibo.alchemist.model.geometry.Euclidean2DShapeFactory
import java.lang.Math.toRadians

/**
 * Helps to produce shapes.
 */
class ZoneShapeFactoryImpl(private val shapeFactory: Euclidean2DShapeFactory) : ZoneShapeFactory<Euclidean2DShape> {

    override fun produceCircleZoneShape(radius: Double): ZoneShape<Euclidean2DShape> {
        return EllipseZoneShape(
            shapeFactory.circle(radius),
            radius,
        )
    }

    override fun produceEllipseZoneShape(radius: Double, ratio: Double): ZoneShape<Euclidean2DShape> {
        return EllipseZoneShape(
            shapeFactory.ellipse(radius * 2 * ratio, radius * 2),
            radius,
            ratio,
        )
    }

    override fun produceCircularSectorZoneShape(
        radius: Double,
        angle: Double,
        inverseToHeading: Boolean,
    ): ZoneShape<Euclidean2DShape> {
        return CircularSegmentZoneShape(
            shapeFactory.circleSector(radius, toRadians(angle), 0.0),
            radius,
            angle,
            if (inverseToHeading) -1.0 else 1.0,
        )
    }
}
