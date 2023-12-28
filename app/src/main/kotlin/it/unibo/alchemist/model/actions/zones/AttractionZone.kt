package it.unibo.alchemist.model.actions.zones

import it.unibo.alchemist.model.actions.utils.Direction
import it.unibo.alchemist.model.actions.utils.Movement
import it.unibo.alchemist.model.geometry.Euclidean2DShape
import it.unibo.alchemist.model.physics.environments.Physics2DEnvironment

//class AttractionZone(
//    ownerNodeId: Int,
//    private val environment: Physics2DEnvironment<Any>,
//    private val movements: Map<Direction, Movement>,
//    private val attractionZoneWidth: Double,
//    private val attractionZoneHeight: Double,
//) : AbstractZone(ownerNodeId, environment, movements) {
//    override val shape: Euclidean2DShape =  environment.shapeFactory.rectangle(neutralZoneWidth * 2, neutralZoneHeight)
//
//}