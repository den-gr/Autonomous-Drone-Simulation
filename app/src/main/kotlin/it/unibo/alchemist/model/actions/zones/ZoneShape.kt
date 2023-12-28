package it.unibo.alchemist.model.actions.zones

interface ZoneShape<T> {
    val shape: T
    val gap: Double
}

class RectangularShape<T>(
    override val shape: T,
    val width: Double,
    val height: Double,
    override val gap: Double = 0.0
) : ZoneShape<T>
