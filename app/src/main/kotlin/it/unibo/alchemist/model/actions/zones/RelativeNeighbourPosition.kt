package it.unibo.alchemist.model.actions.zones

enum class RelativeNeighbourPosition(val startAngle: Double, val endAngle: Double) {
    FORWARD(Math.PI * (1.0 / 4.0), Math.PI * (3.0 / 4.0)),
    RIGHT(-Math.PI * (1.0 / 4.0), Math.PI * (1.0 / 4.0)),
    BEHIND_RIGHT(-Math.PI * (2.0 / 4.0), -Math.PI * (1.0 / 4.0)),
    BEHIND_LEFT(-Math.PI * (3.0 / 4.0), -Math.PI * (2.0 / 4.0)),
    LEFT(Math.PI * (3.0 / 4.0), -Math.PI * (3.0 / 4.0)),
}
