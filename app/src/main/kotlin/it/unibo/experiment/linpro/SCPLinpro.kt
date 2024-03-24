package it.unibo.experiment.linpro

import scpsolver.constraints.LinearBiggerThanEqualsConstraint
import scpsolver.constraints.LinearConstraint
import scpsolver.constraints.LinearEqualsConstraint
import scpsolver.constraints.LinearSmallerThanEqualsConstraint
import scpsolver.lpsolver.LinearProgramSolver
import scpsolver.lpsolver.SolverFactory
import scpsolver.problems.LinearProgram

private class ConstrainNameFactory(private val prefix: String) {
    private var idx = 1

    fun next() = prefix + idx++
}

/**
 * Linpro implementation with SCPSolver.
 * Faster than Apache's but seems to be unstable. Expect occasional crashes and huge amount of console output.
 */
class SCPLinpro<S, D> : AbstractLinpro<S, D>() {
    companion object {
        fun isAvailable() = SolverFactory.newDefault() != null
    }
    private val solver: LinearProgramSolver = SolverFactory.newDefault()
        ?: throw IllegalAccessException("SCPLinpro is not available, try ApacheLinpro instead")
    init {
        if (solver.javaClass.simpleName.equals("GLPKSolver", ignoreCase = true)) {
            System.out.close() // avoid the HUGE console output of GPLK
        }
    }

    override fun solveLPProblem(builder: LPProblemBuilder.() -> Unit): DoubleArray {
        val constraintNameFactory = ConstrainNameFactory("c")
        val settings = object :
            LPProblemBuilder {
            var objectiveFunctionCoefficients = DoubleArray(0)
            var objective =
                LPProblemBuilder.GOAL.MINIMIZE
            val constraints = mutableListOf<LinearConstraint>()
            var nonNegative = false
            override fun setObjectiveFunction(coefficients: DoubleArray, goal: LPProblemBuilder.GOAL) {
                objectiveFunctionCoefficients = coefficients
                objective = goal
            }
            override fun addSmallerThanEqualsConstraint(coefficients: DoubleArray, value: Double) {
                constraints.add(LinearSmallerThanEqualsConstraint(coefficients, value, constraintNameFactory.next()))
            }
            override fun addBiggerThanEqualsConstraint(coefficients: DoubleArray, value: Double) {
                constraints.add(LinearBiggerThanEqualsConstraint(coefficients, value, constraintNameFactory.next()))
            }
            override fun addEqualsConstraint(coefficients: DoubleArray, value: Double) {
                constraints.add(LinearEqualsConstraint(coefficients, value, constraintNameFactory.next()))
            }
            override fun addNonNegativityConstraint() {
                nonNegative = true
            }
        }.apply(builder)
        val problem = LinearProgram(settings.objectiveFunctionCoefficients)
        problem.isMinProblem = settings.objective == LPProblemBuilder.GOAL.MINIMIZE
        settings.constraints.forEach {
            problem.addConstraint(it)
        }
        if (settings.nonNegative) {
            problem.lowerbound = DoubleArray(problem.dimension)
        }
        return solver.solve(problem)
    }
}
