package it.unibo.alchemist.boundary.swingui.effect.impl

import it.unibo.alchemist.boundary.ui.api.Wormhole2D
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Position2D
import it.unibo.alchemist.model.actions.CameraSeeWithBlindSpot
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import org.jooq.lambda.function.Consumer2
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import smile.clustering.hclust
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import kotlin.math.ceil

@Suppress("DEPRECATION")
class DrawCluster : it.unibo.alchemist.boundary.swingui.effect.api.Effect {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(DrawCluster::class.java)
    }

    private val ks = 1.0
    private val sizex = 12
    private val listOfColors = listOf(
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

    private var alreadyLogged: Boolean = false

    override fun getColorSummary(): Color {
        return Color.YELLOW
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T, P : Position2D<P>?> apply(
        graphics: Graphics2D,
        node: Node<T>,
        environment: Environment<T, P>,
        wormhole: Wormhole2D<P>,
    ) {
        if (environment is Physics2DEnvironment && node.contains(SimpleMolecule("drone"))) {
            draw(graphics, environment, wormhole as Wormhole2D<Euclidean2DPosition>)
        } else {
            logOnce("DrawZones only works with Physics2DEnvironment") { logger, message ->
                logger.warn(message)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> draw(
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
            val c = hclust(data, "ward")
            val limit = 350.0
            val labels = if (c.height().last() < limit) {
                IntArray(data.size) { 0 }
            } else {
                c.partition(limit)
            }
            val groupedData = data.zip(labels.toTypedArray()).groupBy { it.second }

            val result = mutableListOf<List<DoubleArray>>()

            groupedData.forEach { (_, pairs) ->
                result.add(pairs.map { it.first })
            }

            result.forEachIndexed { idx, cluster ->
                cluster.forEach {
                    val viewPoint: Point = wormhole.getViewPoint(environment.makePosition(it[0], it[1]))
                    fillVisibleNodeWithColor(graphics, viewPoint, listOfColors[idx % listOfColors.size])
                }
            }
        }
    }

    private fun fillVisibleNodeWithColor(graphics: Graphics2D, point: Point, color: Color) {
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
