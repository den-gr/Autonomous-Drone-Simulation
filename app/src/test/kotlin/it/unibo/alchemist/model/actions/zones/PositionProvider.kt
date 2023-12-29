package it.unibo.alchemist.model.actions.zones

/**
 * Provide positions useful for testing [Zone] implementations.
 */
interface PositionProvider<P> {
    /**
     * Return a position distant from center where can be placed a node.
     */
    fun getNextExternalPosition(): P

    fun getNorthInZonePosition(): P

    fun getNorthEastInZonePosition(): P

    fun getNorthEastOutZonePosition(): P

    fun getNorthWestInZonePosition(): P

    fun getNorthWestOutZonePosition(): P

    fun getSouthEastInZonePosition(): P

    fun getSouthEastOutZonePosition(): P

    fun getSouthWestInZonePosition(): P

    fun getSouthWestOutZonePosition(): P
}
