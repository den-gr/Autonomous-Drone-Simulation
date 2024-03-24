package it.unibo.alchemist.model.linkingrules

import it.unibo.alchemist.model.* // ktlint-disable no-wildcard-imports
import it.unibo.alchemist.model.neighborhoods.Neighborhoods

/**
 * Allows limit connection of nodes to a specific group.
 * @param radius of connection.
 * @param groupName molecule that must have all group members.
 */
class ConnectToGroup<T, P : Position<P>>(
    radius: Double,
    private val groupName: Molecule,
) : ConnectWithinDistance<T, P>(radius) {

    override fun computeNeighborhood(center: Node<T>, environment: Environment<T, P>): Neighborhood<T> =
        super.computeNeighborhood(center, environment).run {
            if (center.contains(groupName)) {
                Neighborhoods.make(environment, center, this.neighbors.filter { it.contains(groupName) })
            } else {
                Neighborhoods.make(environment, center, emptyList())
            }
        }
}
