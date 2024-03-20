package it.unibo.alchemist.model.actions

import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.molecules.SimpleMolecule
import it.unibo.alchemist.model.nodes.VisibleNodeImpl
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment
import it.unibo.alchemist.model.positions.Euclidean2DPosition
import java.lang.Math.toRadians

/**
 * Checks nodes in the [environment] and writes in [outputMolecule]
 * the list of [it.unibo.alchemist.model.VisibleNode],
 * containing [filterByMolecule].
 * [distance] and [angle] define the field of view.
 * [blindSpotDistance] define the radius of a zone inside node's FoV where neighbors are not visible.
 */
class CameraSeeWithBlindSpot @JvmOverloads constructor(
    node: Node<Any>,
    private val environment: Physics2DEnvironment<Any>,
    /**
     * Distance of the blind spot of field of view.
     */
    val blindSpotDistance: Double,
    /**
     * Distance of the field of view.
     */
    val distance: Double,
    /**
     * Angle in degrees of the field of view.
     */
    val angle: Double,
    private val outputMolecule: Molecule = SimpleMolecule("vision"),
    private val filterByMolecule: Molecule? = null,
) : AbstractAction<Any>(node) {

    private val fieldOfView =
        FieldOfView2DWithBlindSpot(
            environment,
            node,
            blindSpotDistance,
            distance,
            toRadians(angle),
        )

    var seenTargets: List<Node<Any>> = emptyList()

    init {
        require(blindSpotDistance in 0.0..distance)
        node.setConcentration(outputMolecule, emptyList<Any>())
    }

    fun isVisible(point: Euclidean2DPosition): Boolean {
        return fieldOfView.contains(point)
    }

    override fun cloneAction(node: Node<Any>, reaction: Reaction<Any>) =
        CameraSeeWithBlindSpot(node, environment, blindSpotDistance, distance, angle, outputMolecule, filterByMolecule)

    override fun execute() {
        var seen = fieldOfView.influentialNodes()
        filterByMolecule?.run {
            seen = seen.filter { it.contains(filterByMolecule) }
        }
        seenTargets = seen
        node.setConcentration(outputMolecule, seen.map { VisibleNodeImpl(it, environment.getPosition(it)) })
    }

    override fun getContext() = Context.LOCAL

    class FieldOfView2DWithBlindSpot<T>(
        private val environment: Physics2DEnvironment<T>,
        private val owner: Node<T>,
        blindSpotDistance: Double,
        fovDistance: Double,
        aperture: Double,
    ) {
        private val fovShape = environment.shapeFactory.circleSector(fovDistance, aperture, 0.0)
        private val blindSpotShape = environment.shapeFactory.circleSector(blindSpotDistance, aperture, 0.0)

        fun influentialNodes(): List<Node<T>> = environment.getNodesWithin(
            fovShape.transformed {
                origin(environment.getPosition(owner))
                rotate(environment.getHeading(owner))
            },
        ).minus(nodesInBlindSpot().toSet())

        private fun nodesInBlindSpot(): List<Node<T>> = environment.getNodesWithin(
            blindSpotShape.transformed {
                origin(environment.getPosition(owner))
                rotate(environment.getHeading(owner))
            },
        )

        fun contains(point: Euclidean2DPosition): Boolean {
            val pointShape = environment.shapeFactory.circle(1.0).transformed {
                origin(point)
            }
            val intersectFoV = fovShape.transformed {
                origin(environment.getPosition(owner))
                rotate(environment.getHeading(owner))
            }.intersects(pointShape)
            val intersectBlindSpot = blindSpotShape.transformed {
                origin(environment.getPosition(owner))
                rotate(environment.getHeading(owner))
            }.intersects(pointShape)
            return intersectFoV && !intersectBlindSpot
        }
    }
}
