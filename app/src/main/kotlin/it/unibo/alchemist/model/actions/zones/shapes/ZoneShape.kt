package it.unibo.alchemist.model.actions.zones.shapes

interface ZoneShape<T> {
    val shape: T

    fun makeCopy(): ZoneShape<T>
}

class RectangularZoneShape<T>(
    override val shape: T,
    private val width: Double,
    private val height: Double,
) : ZoneShape<T> {

    override fun makeCopy(): ZoneShape<T> {
        return RectangularZoneShape(shape, width, height)
    }
}

class EllipseZoneShape<T>(
    override val shape: T,
    val radius: Double,
    val ratio: Double = 1.0,
) : ZoneShape<T> {

    override fun makeCopy(): ZoneShape<T> {
        return EllipseZoneShape(shape, radius)
    }
}

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
