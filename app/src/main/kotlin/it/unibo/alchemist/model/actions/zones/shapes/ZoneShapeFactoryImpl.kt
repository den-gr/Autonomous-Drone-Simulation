package it.unibo.alchemist.model.actions.zones.shapes

import it.unibo.alchemist.model.geometry.Euclidean2DShape
import it.unibo.alchemist.model.geometry.Euclidean2DShapeFactory

class ZoneShapeFactoryImpl(private val shapeFactory: Euclidean2DShapeFactory) : ZoneShapeFactory<Double, Euclidean2DShape> {

    override fun produceRectangularZoneShape(
        width: Double,
        height: Double,
        zoneType: ZoneType,
    ): ZoneShape<Euclidean2DShape> {
        val gap = when(zoneType){
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
}
