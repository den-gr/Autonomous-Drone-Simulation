package it.unibo.alchemist.model.actions.zones.shapes

import it.unibo.alchemist.model.geometry.Euclidean2DShape
import it.unibo.alchemist.model.geometry.Euclidean2DShapeFactory
import java.lang.Math.toRadians

class ZoneShapeFactoryImpl(private val shapeFactory: Euclidean2DShapeFactory) : ZoneShapeFactory<Double, Euclidean2DShape> {

    override fun produceRectangularZoneShape(
        width: Double,
        height: Double,
        zoneType: ZoneType,
    ): ZoneShape<Euclidean2DShape> {
        val gap = when (zoneType) {
            ZoneType.FRONT -> height / 2
            ZoneType.REAR -> -height / 2
            ZoneType.FRONT_AND_REAR -> 0.0
        }
        return RectangularZoneShape(
            shapeFactory.rectangle(width, height),
            width,
            height,
            gap,
        )
    }

    override fun produceCircleZoneShape(radius: Double): ZoneShape<Euclidean2DShape> {
        return CircleZoneShape(
            shapeFactory.circle(radius),
            radius,
        )
    }

    override fun produceCircularSectorZoneShape(radius: Double, angle: Double): ZoneShape<Euclidean2DShape> {
        return CircularSegmentZoneShape(
            shapeFactory.circleSector(radius, toRadians(angle), 0.0),
            radius,
            angle,
        )
    }
}
