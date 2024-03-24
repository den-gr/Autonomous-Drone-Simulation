package it.unibo.alchemist.model.actions.zones

import kotlin.math.PI

/**
 * Map the angles with different directions. Helps to determinate the direction of node's neighbors.
 * These position are used for Stress zone.
 * @param startAngle initial angle of direction circular sector.
 * @param endAngle end angle of direction circular sector.
 */
enum class RelativePosition(val startAngle: Double, val endAngle: Double) {
    LEFT(0.0, PI),
    BEHIND_LEFT(PI * (1.0 / 2.0), PI),
    BEHIND_RIGHT(PI, PI + PI * (1.0 / 2.0)),
    RIGHT(PI, 2 * PI),
    FORWARD(PI * (1.0 / 2.0), PI + PI * (1.0 / 2.0)),
}

/**
 * Map the angles with directions.
 * These positions are used for Attraction and Neutral zones.
 * @param startAngle initial angle of direction circular sector.
 * @param endAngle end angle of direction circular sector.
 */
enum class RelativeLateralPosition(val startAngle: Double, val endAngle: Double) {
    LEFT(0.0, Math.PI),
    RIGHT(Math.PI, 2 * Math.PI),
}
