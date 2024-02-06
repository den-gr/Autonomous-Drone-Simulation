package it.unibo.alchemist.boundary.swingui.effect.impl

import it.unibo.alchemist.boundary.ui.api.Wormhole2D
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Position2D
import it.unibo.alchemist.model.actions.CameraSeeWithBlindSpot
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import it.unibo.experiment.clustering.Cluster
import org.jooq.lambda.function.Consumer2
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import kotlin.math.ceil

open class DrawVisibleClusters : AbstractDrawOnce() {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(DrawVisibleClusters::class.java)
        protected val ks = 1.0
        protected val sizex = 12

        @JvmStatic
        protected val listOfColors = listOf(
            Color.YELLOW,
            Color.BLUE,
            Color.GREEN,
            Color.CYAN,
            Color.MAGENTA,
            Color.DARK_GRAY,
            Color.ORANGE,
            Color.LIGHT_GRAY,
            Color.PINK,
            Color.RED,
        )
    }

    private var alreadyLogged: Boolean = false

    override fun getColorSummary(): Color {
        return Color.YELLOW
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?, P : Position2D<P>?> draw(
        graphics2D: Graphics2D,
        node: Node<T>?,
        environment: Environment<T, P>?,
        wormhole: Wormhole2D<P>?,
    ) {
        if (environment is Physics2DEnvironment) {
            draw(graphics2D, environment, wormhole as Wormhole2D<Euclidean2DPosition>)
        } else {
            logOnce("DrawZones only works with Physics2DEnvironment") { logger, message ->
                logger.warn(message)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    open fun <T> draw(
        graphics: Graphics2D,
        environment: Physics2DEnvironment<T>,
        wormhole: Wormhole2D<Euclidean2DPosition>,
    ) {
        graphics.color = colorSummary

        val allVisibleNodesPositions = environment.nodes
            .flatMap { n ->
                n.reactions
                    .flatMap { it.actions }
                    .filterIsInstance<CameraSeeWithBlindSpot>()
                    .flatMap { it.seenTargets }
            }
            .toSet()
            .map { it as Node<T> }
            .map { environment.getPosition(it) }

        val data = allVisibleNodesPositions.map { it.coordinates }.toTypedArray()
        if (data.size < 2) {
            allVisibleNodesPositions.forEach {
                fillVisibleNodeWithColor(graphics, wormhole.getViewPoint(it), listOfColors[0])
            }
        } else {
            val result = environment.nodes
                .first { it.contains(SimpleMolecule("drone")) }
                .getConcentration(SimpleMolecule("Clusters"))

            if (result != null) {
                val sortedPairs = (result as List<Cluster>).sortedBy { it.centroid.asAngle }

                sortedPairs.forEachIndexed { idx, cluster ->

                    cluster.nodes.forEach {
                        val viewPoint: Point = wormhole.getViewPoint(it.position)
                        fillVisibleNodeWithColor(graphics, viewPoint, listOfColors[idx % listOfColors.size])
                    }
                }
            }
        }
    }

    protected fun fillVisibleNodeWithColor(graphics: Graphics2D, point: Point, color: Color) {
        graphics.color = color
        val startx: Int = point.x - sizex / 2
        val sizey = ceil(sizex * ks).toInt()
        val starty: Int = point.y - sizey / 2
        graphics.fillOval(startx, starty, sizex, sizey)
    }

    private fun logOnce(message: String, logger: Consumer2<Logger, String>) {
        if (!alreadyLogged) {
            logger.accept(LOGGER, message)
            alreadyLogged = true
        }
    }
}
