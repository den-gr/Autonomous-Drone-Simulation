package it.unibo

import smile.clustering.*

fun main() {
    // Sample data
    val data = arrayOf(
        doubleArrayOf(1.0, 2.0),
        doubleArrayOf(1.5, 1.8),
        doubleArrayOf(5.0, 8.0),
        doubleArrayOf(8.0, 8.0),
        doubleArrayOf(1.0, 0.6),
        doubleArrayOf(9.0, 11.0),
        doubleArrayOf(-8.0, -2.0),
        doubleArrayOf(-10.0, -2.0),
        doubleArrayOf(-9.0, -3.0),
    )

    val arr = IntArray(data.size) { 0 }
    println(arr.contentToString())

    // GMeans clustering
    val k = 2 // Number of clusters

    val c = hclust(data, "single")

    val h = c.height()
    val hsplit = c.partition(7.0)
    val labels = c.partition(2)

    val groupedData = data.zip(labels.toTypedArray()).groupBy { it.second }

    val result = mutableListOf<List<DoubleArray>>()

    groupedData.forEach { (label, pairs) ->
        result.add(pairs.map { it.first })
    }

    val centroids = mutableListOf<DoubleArray>()
    for (cluster in result) {
        val c = cluster.foldRight(doubleArrayOf(0.0, 0.0)) { v, acc ->
            doubleArrayOf(v[0] + acc[0], v[1] + acc[1])
        }
        val size = cluster.size
        centroids.add(doubleArrayOf(c[0] / size, c[1] / size))
    }

    centroids.forEach { (e) ->
        println("Data for label $e")
    }
    println(c)
}
