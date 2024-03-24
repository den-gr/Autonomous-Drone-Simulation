package it.unibo.alchemist.boundary.swingui.effect.impl

import it.unibo.alchemist.boundary.ui.api.Wormhole2D
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Position2D
import it.unibo.alchemist.model.actions.HerdBehavior
import it.unibo.alchemist.model.actions.zones.StressZone
import it.unibo.alchemist.model.actions.zones.Zone
import it.unibo.alchemist.model.actions.zones.shapes.CircularSegmentZoneShape
import it.unibo.alchemist.model.actions.zones.shapes.EllipseZoneShape
import it.unibo.alchemist.model.geometry.Euclidean2DShape
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import jdk.jshell.spi.ExecutionControl.NotImplementedException
import org.jooq.lambda.function.Consumer2
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import java.awt.geom.AffineTransform
import java.awt.geom.Arc2D
import java.awt.geom.Ellipse2D

/**
 * Draws individuals stress, neutral, attraction and rear zones.
 */
@Suppress("DEPRECATION")
class DrawZones : it.unibo.alchemist.boundary.swingui.effect.api.Effect {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(DrawZones::class.java)
    }

    private var alreadyLogged: Boolean = false
    private val transparentRed = Color(1.0f, 1.0f, 0.0f, 0.05f)

    override fun <T, P : Position2D<P>?> apply(
        graphics: Graphics2D,
        node: Node<T>,
        environment: Environment<T, P>,
        wormhole: Wormhole2D<P>,
    ) {
        val zoom: Double = wormhole.zoom
        val viewPoint: Point = wormhole.getViewPoint(environment.getPosition(node))
        val x: Int = viewPoint.x
        val y: Int = viewPoint.y
        if (environment is Physics2DEnvironment) {
            draw(graphics, node, environment, zoom, x, y)
        } else {
            logOnce("DrawZones only works with Physics2DEnvironment") { logger, message ->
                logger.warn(message)
            }
        }
    }

    override fun getColorSummary(): Color {
        return Color.DARK_GRAY
    }

    private fun <T> draw(
        graphics: Graphics2D,
        node: Node<T>,
        environment: Physics2DEnvironment<T>,
        zoom: Double,
        x: Int,
        y: Int,
    ) {
        val transform: AffineTransform = getTransform(x, y, zoom, getRotation(node, environment))
        graphics.color = colorSummary
        node.reactions
            .flatMap { r -> r.actions }
            .filterIsInstance<HerdBehavior>()
            .forEach { a ->
                val zones: List<Zone> = a.zones
                zones.forEach { z ->
                    when (z.zoneShape) {
                        is EllipseZoneShape<Euclidean2DShape> -> {
                            val shape = z.zoneShape as EllipseZoneShape<Euclidean2DShape>
                            val fov: java.awt.Shape = Ellipse2D.Double(
                                -shape.radius * shape.ratio,
                                -shape.radius,
                                shape.radius * 2 * shape.ratio,
                                shape.radius * 2,
                            )
                            if (z is StressZone) {
                                graphics.color = transparentRed
                                graphics.fill(transform.createTransformedShape(fov))
                                graphics.color = colorSummary
                            }
                            graphics.draw(transform.createTransformedShape(fov))
                        }
                        is CircularSegmentZoneShape<Euclidean2DShape> -> {
                            val shape = z.zoneShape as CircularSegmentZoneShape<Euclidean2DShape>
                            val startAngle = shape.direction * -shape.angle / 2
                            val fov: java.awt.Shape = Arc2D.Double(
                                -shape.radius,
                                -shape.radius,
                                shape.radius * 2,
                                shape.radius * 2,
                                startAngle,
                                shape.angle,
                                Arc2D.PIE,
                            )
                            graphics.draw(transform.createTransformedShape(fov))
                        }
                        else -> {
                            throw NotImplementedException("This zone shape is not implemented")
                        }
                    }
                }
            }
    }

    private fun getTransform(x: Int, y: Int, zoom: Double, rotation: Double): AffineTransform =
        AffineTransform().apply {
            translate(x.toDouble(), y.toDouble())
            scale(zoom, zoom)
            rotate(-rotation)
        }

    private fun <T> getRotation(node: Node<T>, environment: Physics2DEnvironment<T>): Double {
        return environment.getHeading(node).asAngle
    }

    private fun logOnce(message: String, logger: Consumer2<Logger, String>) {
        if (!alreadyLogged) {
            logger.accept(LOGGER, message)
            alreadyLogged = true
        }
    }
}
