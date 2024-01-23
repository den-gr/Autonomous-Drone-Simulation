package it.unibo.alchemist.model.linkingrules

import it.unibo.alchemist.model.*
import it.unibo.alchemist.model.neighborhoods.Neighborhoods

class ConnectToGroup<T, P : Position<P>>(
    radius: Double,
    private val groupName: Molecule,
) : ConnectWithinDistance<T, P>(radius){

    override fun computeNeighborhood(center: Node<T>, environment: Environment<T, P>): Neighborhood<T> =
        super.computeNeighborhood(center, environment).run {
            if (center.contains(groupName)) Neighborhoods.make(environment, center, this.neighbors.filter { it.contains(groupName) }) else Neighborhoods.make(environment, center, emptyList())
        }

//    override fun computeNeighborhood(center: Node<T>, environment: Environment<T, P>): Neighborhood<T> {
//        val list = super.computeNeighborhood(center, environment).neighbors.filter { it.contains(groupName) }
//        return Neighborhoods.make(environment, center, list)
//    }
}
