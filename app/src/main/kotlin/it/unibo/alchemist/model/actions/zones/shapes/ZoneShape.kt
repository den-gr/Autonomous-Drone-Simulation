package it.unibo.alchemist.model.actions.zones.shapes

interface ZoneShape<T> {
    val shape: T
    val offset: Double

    fun makeCopy(): ZoneShape<T>
}

class RectangularZoneShape<T>(
    override val shape: T,
    val width: Double,
    val height: Double,
    override val offset: Double = 0.0
) : ZoneShape<T> {

    override fun makeCopy(): ZoneShape<T> {
        return RectangularZoneShape(shape, width, height, offset)
    }
}
