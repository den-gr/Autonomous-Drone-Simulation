package it.unibo.alchemist.model.actions.zones

import kotlin.math.PI

enum class RelativePosition(val startAngle: Double, val endAngle: Double) {
    LEFT(PI * (1.0 / 4.0), PI * (3.0 / 4.0)),
    BEHIND_LEFT(PI * (3.0 / 4.0), PI),
    BEHIND_RIGHT(PI, PI + PI * (1.0 / 4.0)),
    RIGHT(PI + PI * (1.0 / 4.0), PI + PI * (3.0 / 4.0)),
    FORWARD(Math.PI * (1.0 / 4.0), PI + PI * (3.0 / 4.0)),
}
//enum class RelativePosition(val startAngle: Double, val endAngle: Double) {
//    FORWARD(Math.PI * (1.0 / 4.0), Math.PI * (3.0 / 4.0)),
//    RIGHT(-Math.PI * (1.0 / 4.0), Math.PI * (1.0 / 4.0)),
//    BEHIND_RIGHT(-Math.PI * (2.0 / 4.0), -Math.PI * (1.0 / 4.0)),
//    BEHIND_LEFT(-Math.PI * (3.0 / 4.0), -Math.PI * (2.0 / 4.0)),
//    LEFT(Math.PI * (3.0 / 4.0), -Math.PI * (3.0 / 4.0)),
//}
