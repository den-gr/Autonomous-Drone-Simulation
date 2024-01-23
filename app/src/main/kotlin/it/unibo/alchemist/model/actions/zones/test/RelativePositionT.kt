package it.unibo.alchemist.model.actions.zones.test

import kotlin.math.PI

enum class RelativePositionT(val startAngle: Double, val endAngle: Double) {
    LEFT(0.0, PI),
    BEHIND_LEFT(PI * (1.0 / 2.0), PI),
    BEHIND_RIGHT(PI, PI + PI * (1.0 / 2.0)),
    RIGHT(PI, 2 * PI),
    FORWARD(PI * (1.0 / 2.0), PI + PI * (1.0 / 2.0)),
}
