package it.unibo.alchemist.model.deployments

import it.unibo.alchemist.boundary.gps.loaders.TraceLoader
import it.unibo.alchemist.model.Deployment
import it.unibo.alchemist.model.GeoPosition
import it.unibo.alchemist.model.maps.GPSTrace
import java.io.IOException
import java.util.stream.Stream
import java.util.stream.StreamSupport

/**
 * @param nodeCount
 * number of node request
 * @param path
 * path with the gps tracks
 * @param cycle
 * true if, in case there are more nodes to deploy than available GPS traces,
 * the traces should be reused cyclically. E.g., if 10 nodes must be deployed
 * but only 9 GPS traces are available, the first one is reused for the 10th
 * node.
 * @param normalizer
 * class to use to normalize time
 * @param args
 * args to use to create GPSTimeNormalizer
 * @throws IOException if there are errors accessing the file system
 */
class FromGPSTracesDynamic(
    path: String,
    cycle: Boolean,
    normalizer: String,
    vararg args: Any?,
) : Deployment<GeoPosition> {
    private var traces: TraceLoader
    private var numNode = 0

    init {
        traces = TraceLoader(path, cycle, normalizer, *args)
        require(traces.size().isPresent)
        numNode = traces.size().get()
    }

    override fun stream(): Stream<GeoPosition>? {
        return StreamSupport.stream(traces.spliterator(), false)
            .limit(numNode.toLong())
            .map { obj: GPSTrace -> obj.initialPosition }
    }
}
