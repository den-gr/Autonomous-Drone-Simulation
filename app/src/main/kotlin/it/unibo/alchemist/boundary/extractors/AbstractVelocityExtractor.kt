package it.unibo.alchemist.boundary.extractors

abstract class AbstractVelocityExtractor : AbstractDoubleExporter() {
    override val columnNames: List<String> = listOf("0-1", "1-2", "2-3", "3-4", "4-5", "5-6", "6-10", ">10")

    /**
     * @param velocity in meters per second
     * @return the velocity aggregation group name
     */
    fun getRange(velocity: Double): String {
        return when {
            velocity in 0.0..1000.0 -> "0-1"
            velocity in 1000.0..2000.0 -> "1-2"
            velocity in 2000.0..3000.0 -> "2-3"
            velocity in 3000.0..4000.0 -> "3-4"
            velocity in 4000.0..5000.0 -> "4-5"
            velocity in 5000.0..6000.0 -> "5-6"
            velocity in 6000.0..10000.0 -> "6-10"
            velocity >= 10000.0 -> ">10"
            else -> {
                print(velocity)
                throw IllegalStateException("Some problem with velocity value.")
            }
        }
    }
}
