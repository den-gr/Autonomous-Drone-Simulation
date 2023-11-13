@file:Suppress("DEPRECATION")

package it.unibo.alchemist.boundary.swingui.impl

import it.unibo.alchemist.boundary.Loader
import it.unibo.alchemist.boundary.launchers.SimulationLauncher
import javax.swing.JFrame

/**
 * Launches a Swing GUI meant to be used for a single simulation run.
 */
class SingleRunSwingUI2(
    private val graphics: String? = null,
) : SimulationLauncher() {

    override fun launch(loader: Loader) {
        val simulation = prepareSimulation<Any, Nothing>(loader, emptyMap<String, Any>())
        when (graphics) {
            null -> SingleRunGUI2.make(simulation, JFrame.EXIT_ON_CLOSE)
            else -> SingleRunGUI2.make(simulation, graphics, JFrame.EXIT_ON_CLOSE)
        }
        simulation.run()
    }
}
