package it.unibo.alchemist.model.actions.utils

import java.util.Objects

class TraceRef(
    val path: String,
    val cycle: Boolean,
    val normalizer: String,
    vararg args: Any,
) {
    val args: Array<out Any> = args
    private var hash: Int = 0

    init {
        // NOPMD: array is stored directly by purpose.
    }

    override fun hashCode(): Int {
        if (hash == 0) {
            hash = Objects.hash(path, normalizer, cycle, args.contentHashCode())
        }
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TraceRef) return false
        return path == other.path &&
            normalizer == other.normalizer &&
            cycle == other.cycle &&
            args.contentDeepEquals(other.args)
    }

    override fun toString(): String {
        return (if (cycle) "Cyclic" else "") +
            "Trace[path=$path, normalizer=$normalizer(${args.contentToString()})]"
    }
}
