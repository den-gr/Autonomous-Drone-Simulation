package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.positions.Euclidean2DPosition

class PositionProviderImpl(
    val bodyLen: Double,
    private val zoneWidth: Double,
    private val zoneHeight: Double,
) : PositionProvider<Euclidean2DPosition> {
    private val epsilon = 0.01
    private val offset = 9999.0
    private var count = 0

    override fun getNextExternalPosition(): Euclidean2DPosition {
        count++
        return Euclidean2DPosition(offset, offset + (count * bodyLen) + epsilon)
    }

    override fun getNorthInZonePosition(): Euclidean2DPosition {
        return Euclidean2DPosition(0.0, zoneHeight / 2)
    }

    override fun getNorthEastInZonePosition(): Euclidean2DPosition {
        return Euclidean2DPosition(zoneWidth / 2, zoneHeight / 4)
    }

    override fun getNorthEastOutZonePosition(): Euclidean2DPosition {
        return Euclidean2DPosition(zoneWidth + bodyLen, zoneHeight + epsilon)
    }

    override fun getNorthWestInZonePosition(): Euclidean2DPosition {
        return Euclidean2DPosition(-zoneWidth / 2, zoneHeight / 4)
    }

    override fun getNorthWestOutZonePosition(): Euclidean2DPosition {
        return Euclidean2DPosition(-zoneWidth - bodyLen, zoneHeight + epsilon)
    }

    override fun getSouthEastInZonePosition(): Euclidean2DPosition {
        return Euclidean2DPosition(zoneWidth / 2, -zoneHeight / 4)
    }

    override fun getSouthEastOutZonePosition(): Euclidean2DPosition {
        return Euclidean2DPosition(zoneWidth + bodyLen, -zoneHeight - epsilon)
    }

    override fun getSouthWestInZonePosition(): Euclidean2DPosition {
        return Euclidean2DPosition(-zoneWidth / 2, -zoneHeight / 4)
    }

    override fun getSouthWestOutZonePosition(): Euclidean2DPosition {
        return Euclidean2DPosition(-zoneWidth - bodyLen, -zoneHeight - epsilon)
    }
}
