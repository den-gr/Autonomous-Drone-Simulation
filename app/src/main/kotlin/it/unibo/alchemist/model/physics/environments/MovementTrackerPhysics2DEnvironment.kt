package it.unibo.alchemist.model.physics.environments

import it.unibo.alchemist.model.Incarnation
import it.unibo.alchemist.model.Molecule
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.positions.Euclidean2DPosition

interface NodeMovementTracker {
    fun queryCameraMovementsSinceLastQuery(): Double
    fun queryObjectMovementsSinceLastQuery(): Double
}

@Suppress("unused")
class MovementTrackerPhysics2DEnvironment<T>(
    incarnation: Incarnation<T, Euclidean2DPosition>,
    private val filterCamera: Molecule,
) : ContinuousPhysics2DEnvironment<T>(incarnation), NodeMovementTracker {
    private var cameraMovements = 0.0
    private var objectMovements = 0.0

    override fun moveNodeToPosition(node: Node<T>, newPosition: Euclidean2DPosition) {
        val realLastPos = getPosition(node)
        super.moveNodeToPosition(node, newPosition)
        val realNewPos = getPosition(node)
        realLastPos.also { lastp ->
            realNewPos.also { newp ->
                val distance = lastp.distanceTo(newp)
                if (node.contains(filterCamera)) {
                    cameraMovements += distance
                } else {
                    objectMovements += distance
                }
            }
        }
    }

    override fun queryCameraMovementsSinceLastQuery(): Double {
        val result = cameraMovements
        cameraMovements = 0.0
        return result
    }

    override fun queryObjectMovementsSinceLastQuery(): Double {
        val result = objectMovements
        objectMovements = 0.0
        return result
    }
}
