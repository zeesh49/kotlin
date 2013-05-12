//import java.text.NumberFormat
import js.Date
import java.util.LinkedList
import js.println

abstract class BenchmarkBase(val name: String) {
    // The benchmark code.
    // This function is not used, if both [warmup] and [exercise] are overwritten.
    abstract fun run()

    // Runs a short version of the benchmark. By default invokes [run] once.
    fun warmup() {
        run();
    }

    // Exercices the benchmark. By default invokes [run] 10 times.
    fun exercise() {
        for (i in 1..10) { //todo times
            run()
        }
    }

    // Measures the score for this benchmark by executing it repeately until
    // time minimum has been reached.
    fun measureFor(timeMinimum: Int, f: () -> Unit): Double {
        fun getMilliseconds() = Date().getTime()

        var iter = 0

        val startTime = getMilliseconds()
        while (true) {
            val elapsed = getMilliseconds() - startTime;
            if (elapsed >= timeMinimum)
                return elapsed * 1000.0 / iter.toDouble();
            f();
            iter++;
        }
    }

    // Measures the score for the benchmark and returns it.
    fun measure(): Double {
        // Warmup for at least 100ms. Discard result.
        measureFor(100) { this.warmup(); };

        // Run the benchmark for at least 2000ms.
        val result = measureFor(2000) { this.exercise(); };
        return result;
    }

//    fun Double.toString(format: String) = java.lang.String.format(format, this)

    fun report() {
        val score = measure();
//        println("$name: ${NumberFormat.getInstance().format(score)} us.");
//        println("$name: ${score.toLong()} us.");
//        println("$name: ${score.toString("%f")} us.");
        println("$name: $score us.");
    }
}

fun main(args: Array<String>) {
    DeltaBlue.report();
}

/// Benchmark class required to report results.
object DeltaBlue: BenchmarkBase("DeltaBlue") {
    override fun run() {
        chainTest(100)
        projectionTest(100)
    }
}

/**
 * Strengths are used to measure the relative importance of constraints.
 * New strengths may be inserted in the strength hierarchy without
 * disrupting current constraints.  Strengths cannot be created outside
 * this class, so == can be used for value comparison.
 */
class Strength(val value: Int, val name: String) {
    fun nextWeaker(): Strength = NEXT[value];
}

//fixme class object doesn't support
object Strengths {
    fun stronger(s1: Strength, s2: Strength) = s1.value < s2.value

    fun weaker(s1: Strength, s2: Strength) = s1.value > s2.value

    fun weakest(s1: Strength, s2: Strength) = if (weaker(s1, s2)) s1 else s2

    fun strongest(s1: Strength, s2: Strength) = if (stronger(s1, s2)) s1 else s2
}



// Compile time computed constants.
val REQUIRED        = Strength(0, "required")
val STRONG_REFERRED = Strength(1, "strongPreferred")
val PREFERRED       = Strength(2, "preferred")
val STRONG_DEFAULT  = Strength(3, "strongDefault")
val NORMAL          = Strength(4, "normal")
val WEAK_DEFAULT    = Strength(5, "weakDefault")
val WEAKEST         = Strength(6, "weakest")

val NEXT = array(WEAKEST, WEAK_DEFAULT, NORMAL, STRONG_DEFAULT, PREFERRED, STRONG_REFERRED)

abstract class Constraint(val strength: Strength) {
    abstract fun isSatisfied(): Boolean
    abstract fun markUnsatisfied(): Unit
    abstract fun addToGraph(): Unit
    abstract fun removeFromGraph(): Unit
    abstract fun chooseMethod(mark: Int): Unit
    abstract fun markInputs(mark: Int): Unit
    abstract fun inputsKnown(mark: Int): Boolean
    abstract fun output(): Variable
    abstract fun execute(): Unit
    abstract fun recalculate(): Unit

    /// Activate this constraint and attempt to satisfy it.
    fun addConstraint() {
        addToGraph();
        planner.incrementalAdd(this);
    }

    /**
    * Attempt to find a way to enforce this constraint. If successful,
    * record the solution, perhaps modifying the current dataflow
    * graph. Answer the constraint that this constraint overrides, if
    * there is one, or nil, if there isn't.
    * Assume: I am not already satisfied.
    */
    fun satisfy(mark: Int): Constraint? {
        chooseMethod(mark);
        if (!isSatisfied()) {
            if (strength == REQUIRED) {
                println("Could not satisfy a required constraint!");
            }
            return null;
        }
        markInputs(mark);
        val out = output();
        val overridden = out.determinedBy;
        if (overridden != null)
            overridden.markUnsatisfied();
        out.determinedBy = this;
        if (!planner.addPropagate(this, mark)) println("Cycle encountered");
        out.mark = mark;
        return overridden;
    }

    fun destroyConstraint() {
        if (isSatisfied())
            planner.incrementalRemove(this);
        removeFromGraph();
    }

    /**
     * Normal constraints are not input constraints.  An input constraint
     * is one that depends on external state, such as the mouse, the
     * keybord, a clock, or some arbitraty piece of imperative code.
     */
    open fun isInput() = false;
}

/**
 * Abstract superclass for constraints having a single possible output variable.
 */
abstract class UnaryConstraint(val myOutput: Variable, strength: Strength) : Constraint(strength) {
    var satisfied = false;

    fun init() {
        addConstraint();
    }

    /// Adds this constraint to the constraint graph
    override fun addToGraph() {
        myOutput.addConstraint(this);
        satisfied = false;
    }

    /// Decides if this constraint can be satisfied and records that decision.
    override fun chooseMethod(mark: Int) {
        satisfied = (myOutput.mark != mark) && Strengths.stronger(strength, myOutput.walkStrength);
    }

    /// Returns true if this constraint is satisfied in the current solution.
    override fun isSatisfied(): Boolean = satisfied;

    override fun markInputs(mark: Int) {
        // has no inputs.
    }

    /// Returns the current output variable.
    override fun output(): Variable = myOutput;

    /**
     * Calculate the walkabout strength, the stay flag, and, if it is
     * 'stay', the value for the current output of this constraint. Assume
     * this constraint is satisfied.
     */
    override fun  recalculate() {
        myOutput.walkStrength = strength;
        myOutput.stay = !isInput();
        if (myOutput.stay) execute(); // Stay optimization.
    }

    /// Records that this constraint is unsatisfied.
    override fun markUnsatisfied() {
        satisfied = false;
    }

    override fun inputsKnown(mark: Int) = true;

    override fun removeFromGraph() {
        //        if (myOutput != null)
        myOutput.removeConstraint(this);
        satisfied = false;
    }
}


/**
 * Variables that should, with some level of preference, stay the same.
 * Planners may exploit the fact that instances, if satisfied, will not
 * change their output during plan execution.  This is called "stay
 * optimization".
 */
class StayConstraint(v: Variable, str: Strength) : UnaryConstraint(v, str) {
    {
        init()
    }
    override fun execute() {
        // Stay constraints do nothing.
    }
}


/**
 * A unary input constraint used to mark a variable that the client
 * wishes to change.
 */
class EditConstraint(v: Variable, str: Strength) : UnaryConstraint(v, str) {
    {
        init()
    }
    /// Edits indicate that a variable is to be changed by imperative code.
    override fun isInput() = true;

    override fun execute() {
        // Edit constraints do nothing.
    }
}


// Directions.
val NONE = 1;
val FORWARD = 2;
val BACKWARD = 0;

/**
 * Abstract superclass for constraints having two possible output
 * variables.
 */
abstract class BinaryConstraint(val v1: Variable, val v2: Variable, strength: Strength) : Constraint(strength) {
    var direction = NONE;

    fun init() {
        addConstraint();
    }

    /**
     * Decides if this constraint can be satisfied and which way it
     * should flow based on the relative strength of the variables related,
     * and record that decision.
     */
    override fun chooseMethod(mark: Int) {
        if (v1.mark == mark) {
            direction = if (v2.mark != mark && Strengths.stronger(strength, v2.walkStrength)) FORWARD else NONE
        }
        if (v2.mark == mark) {
            direction = if (v1.mark != mark && Strengths.stronger(strength, v1.walkStrength)) BACKWARD else NONE
        }
        if (Strengths.weaker(v1.walkStrength, v2.walkStrength)) {
            direction = if (Strengths.stronger(strength, v1.walkStrength)) BACKWARD else NONE
        } else {
            direction = if (Strengths.stronger(strength, v2.walkStrength)) FORWARD else BACKWARD
        }
    }

    /// Add this constraint to the constraint graph.
    override fun addToGraph() {
        v1.addConstraint(this);
        v2.addConstraint(this);
        direction = NONE;
    }

    /// Answer true if this constraint is satisfied in the current solution.
    override fun isSatisfied() = direction != NONE

    /// Mark the input variable with the given mark.
    override fun markInputs(mark: Int) {
        input().mark = mark;
    }

    /// Returns the current input variable
    fun input(): Variable = if (direction == FORWARD) v1 else v2

    /// Returns the current output variable.
    override fun output(): Variable = if (direction == FORWARD) v2 else v1;

    /**
     * Calculate the walkabout strength, the stay flag, and, if it is
     * 'stay', the value for the current output of this
     * constraint. Assume this constraint is satisfied.
     */
    override fun recalculate() {
        val ihn = input()
        val out = output();
        out.walkStrength = Strengths.weakest(strength, ihn.walkStrength);
        out.stay = ihn.stay;
        if (out.stay) execute();
    }

    /// Record the fact that this constraint is unsatisfied.
    override fun markUnsatisfied() {
        direction = NONE;
    }

    override fun inputsKnown(mark: Int): Boolean {
        val i = input();
        return i.mark == mark || i.stay || i.determinedBy == null;
    }

    override fun removeFromGraph() {
        //        if (v1 != null)
        v1.removeConstraint(this);
        //        if (v2 != null)
        v2.removeConstraint(this);
        direction = NONE;
    }
}


/**
 * Relates two variables by the linear scaling relationship: "v2 =
 * (v1 * scale) + offset". Either v1 or v2 may be changed to maintain
 * this relationship but the scale factor and offset are considered
 * read-only.
 */

class ScaleConstraint(src: Variable,
                      val scale: Variable,
                      val offset: Variable,
                      dest: Variable,
                      strength: Strength) : BinaryConstraint(src, dest, strength) {
    {
        init()
    }

    //todo fix this workaround (super dosn't support in JS)
    fun superAddToGraph(that: BinaryConstraint) {
        that.v1.addConstraint(that);
        that.v2.addConstraint(that);
        that.direction = NONE;
    }
    fun superRemoveFromGraph(that: BinaryConstraint) {
        that.v1.removeConstraint(that);
        that.v2.removeConstraint(that);
        that.direction = NONE;
    }

    fun superMarkInputs(that: BinaryConstraint, mark: Int) {
        that.input().mark = mark;
    }

    /// Adds this constraint to the constraint graph.
    override fun addToGraph() {
        superAddToGraph(this);
        scale.addConstraint(this);
        offset.addConstraint(this);
    }

    override fun removeFromGraph() {
        superRemoveFromGraph(this);
        //        if (scale != null)
        scale.removeConstraint(this);
        //        if (offset != null)
        offset.removeConstraint(this);
    }

    override fun markInputs(mark: Int) {
        superMarkInputs(this, mark)
        scale.mark = mark
        offset.mark = mark
    }

    /// Enforce this constraint. Assume that it is satisfied.
    override fun execute() {
        if (direction == FORWARD) {
            v2.value = v1.value * scale.value + offset.value;
        } else {
            v1.value = (v2.value - offset.value) / scale.value;
        }
    }

    /**
     * Calculate the walkabout strength, the stay flag, and, if it is
     * 'stay', the value for the current output of this constraint. Assume
     * this constraint is satisfied.
     */
    override fun recalculate() {
        val ihn = input()
        val out = output()
        out.walkStrength = Strengths.weakest(strength, ihn.walkStrength);
        out.stay = ihn.stay && scale.stay && offset.stay;
        if (out.stay) execute();
    }
}


/**
 * Constrains two variables to have the same value.
 */
class EqualityConstraint(v1: Variable, v2: Variable, strength: Strength) : BinaryConstraint(v1, v2, strength) {
    {
        init()
    }

    /// Enforce this constraint. Assume that it is satisfied.
    override fun execute() {
        output().value = input().value;
    }
}


/**
* A constrained variable. In addition to its value, it maintain the
* structure of the constraint graph, the current dataflow graph, and
* various parameters of interest to the DeltaBlue incremental
* constraint solver.
**/
class Variable(val name: String, var value: Int) {
    val constraints = LinkedList<Constraint>();
    var determinedBy: Constraint? = null;
    var mark = 0;
    var walkStrength = WEAKEST;
    var stay = true;

    /**
     * Add the given constraint to the set of all constraints that refer
     * this variable.
     */
    fun addConstraint(c: Constraint) {
        constraints.add(c);
    }

    /// Removes all traces of c from this variable.
    fun removeConstraint(c: Constraint) {
        constraints.remove(c)
        if (determinedBy == c)
            determinedBy = null
    }
}


class Planner {

    var currentMark = 0;

    /**
     * Attempt to satisfy the given constraint and, if successful,
     * incrementally update the dataflow graph.  Details: If satifying
     * the constraint is successful, it may override a weaker constraint
     * on its output. The algorithm attempts to resatisfy that
     * constraint using some other method. This process is repeated
     * until either a) it reaches a variable that was not previously
     * determined by any constraint or b) it reaches a constraint that
     * is too weak to be satisfied using any of its methods. The
     * variables of constraints that have been processed are marked with
     * a unique mark value so that we know where we've been. This allows
     * the algorithm to avoid getting into an infinite loop even if the
     * constraint graph has an inadvertent cycle.
     */
    fun incrementalAdd(c: Constraint) {
        val mark = newMark();

        var overridden = c.satisfy(mark)
        while (overridden != null) {
            overridden = overridden?.satisfy(mark)
        }
    }

    /**
     * Entry point for retracting a constraint. Remove the given
     * constraint and incrementally update the dataflow graph.
     * Details: Retracting the given constraint may allow some currently
     * unsatisfiable downstream constraint to be satisfied. We therefore collect
     * a list of unsatisfied downstream constraints and attempt to
     * satisfy each one in turn. This list is traversed by constraint
     * strength, strongest first, as a heuristic for avoiding
     * unnecessarily adding and then overriding weak constraints.
     * Assume: [c] is satisfied.
     */
    fun incrementalRemove(c: Constraint) {
        val out = c.output();
        c.markUnsatisfied();
        c.removeFromGraph();
        val unsatisfied = removePropagateFrom(out);
        var strength = REQUIRED;
        do {
            for (u in unsatisfied) {
                if (u.strength == strength) incrementalAdd(u);
            }
            strength = strength.nextWeaker();
        } while (strength != WEAKEST);
    }

    /// Select a previously unused mark value.
    fun newMark() = ++currentMark;

    /**
     * Extract a plan for resatisfaction starting from the given source
     * constraints, usually a set of input constraints. This method
     * assumes that stay optimization is desired; the plan will contain
     * only constraints whose output variables are not stay. Constraints
     * that do no computation, such as stay and edit constraints, are
     * not included in the plan.
     * Details: The outputs of a constraint are marked when it is added
     * to the plan under construction. A constraint may be appended to
     * the plan when all its input variables are known. A variable is
     * known if either a) the variable is marked (indicating that has
     * been computed by a constraint appearing earlier in the plan), b)
     * the variable is 'stay' (i.e. it is a constant at plan execution
     * time), or c) the variable is not determined by any
     * constraint. The last provision is for past states of history
     * variables, which are not stay but which are also not computed by
     * any constraint.
     * Assume: [sources] are all satisfied.
     */
    fun makePlan(sources: LinkedList<Constraint>): Plan {
        val mark = newMark();
        val plan = Plan();
        val todo = sources;
        while (todo.size() > 0) {
            val c = todo.remove(todo.lastIndex)
            if (c.output().mark != mark && c.inputsKnown(mark)) {
                plan.addConstraint(c)
                c.output().mark = mark
                addConstraintsConsumingTo(c.output(), todo);
            }
        }
        return plan;
    }

    /**
     * Extract a plan for resatisfying starting from the output of the
     * given [constraints], usually a set of input constraints.
     */
    fun extractPlanFromConstraints(constraints: List<Constraint>): Plan {
        val sources = LinkedList<Constraint>();
        for (c in constraints) {
            // if not in plan already and eligible for inclusion.
            if (c.isInput() && c.isSatisfied())
                sources.add(c);
        }
        return makePlan(sources);
    }

    /**
     * Recompute the walkabout strengths and stay flags of all variables
     * downstream of the given constraint and recompute the actual
     * values of all variables whose stay flag is true. If a cycle is
     * detected, remove the given constraint and answer
     * false. Otherwise, answer true.
     * Details: Cycles are detected when a marked variable is
     * encountered downstream of the given constraint. The sender is
     * assumed to have marked the inputs of the given constraint with
     * the given mark. Thus, encountering a marked node downstream of
     * the output constraint means that there is a path from the
     * constraint's output to one of its inputs.
     */
    fun addPropagate(c: Constraint, mark: Int): Boolean {
        val todo = linkedListOf(c);
        while (todo.size() > 0) {
            val d = todo.remove(todo.lastIndex)
            if (d.output().mark == mark) {
                incrementalRemove(c);
                return false;
            }
            d.recalculate();
            addConstraintsConsumingTo(d.output(), todo);
        }
        return true;
    }

    /**
     * Update the walkabout strengths and stay flags of all variables
     * downstream of the given constraint. Answer a collection of
     * unsatisfied constraints sorted in order of decreasing strength.
     */
    fun removePropagateFrom(out: Variable): List<Constraint> {
        out.determinedBy = null;
        out.walkStrength = WEAKEST;
        out.stay = true;
        val unsatisfied = LinkedList<Constraint>();
        val todo = linkedListOf(out);
        while (todo.size() > 0) {
            val v = todo.remove(todo.lastIndex)
            for (c in v.constraints) {
                if (!c.isSatisfied()) unsatisfied.add(c);
            }
            val determining = v.determinedBy;
            for (next in v.constraints) {
                if (next != determining && next.isSatisfied()) {
                    next.recalculate();
                    todo.add(next.output());
                }
            }
        }
        return unsatisfied;
    }

    fun addConstraintsConsumingTo(v: Variable, coll: MutableList<Constraint>) {
        val determining = v.determinedBy
        for (c in v.constraints) {
            if (c != determining && c.isSatisfied()) coll.add(c);
        }
    }
}


/**
* A Plan is an ordered list of constraints to be executed in sequence
* to resatisfy all currently satisfiable constraints in the face of
* one or more changing inputs.
*/
class Plan {
    val list = LinkedList<Constraint>();

    fun addConstraint(c: Constraint) {
        list.add(c)
    }

    fun size() = list.size()

    fun execute() {
        for (c in list) {
            c.execute();
        }
    }
}


/**
* This is the standard DeltaBlue benchmark. A long chain of equality
* constraints is constructed with a stay constraint on one end. An
* edit constraint is then added to the opposite end and the time is
* measured for adding and removing this constraint, and extracting
* and executing a constraint satisfaction plan. There are two cases.
* In case 1, the added constraint is stronger than the stay
* constraint and values must propagate down the entire length of the
* chain. In case 2, the added constraint is weaker than the stay
* constraint so it cannot be accomodated. The cost in this case is,
* of course, very low. Typical situations lie somewhere between these
* two extremes.
*/
fun chainTest(n: Int) {
    planner = Planner();
    val first = Variable("v", 0);
    var prev = first

    // Build chain of n equality constraints.
    for (i in 1..n) {
        val v = Variable("v", 0);
        EqualityConstraint(prev, v, REQUIRED);
        prev = v;
    }
    val last = prev;

    StayConstraint(last, STRONG_DEFAULT);
    val edit = EditConstraint(first, PREFERRED);
    val plan = planner.extractPlanFromConstraints(linkedListOf(edit));
    for (i in 0..99) {
        first.value = i;
        plan.execute();
        if (last.value != i) {
            print("Chain test failed.\n${last.value}\n${i}");
        }
    }
}

/**
* This test constructs a two sets of variables related to each
* other by a simple linear transformation (scale and offset). The
* time is measured to change a variable on either side of the
* mapping and to change the scale and offset factors.
*/
fun projectionTest(n: Int) {
    planner = Planner();
    val scale = Variable("scale", 10)
    val offset = Variable("offset", 1000)
    var src = Variable("tmp", 0)
    var dst = src

    val dests = LinkedList<Variable>();
    for (i in 0..n - 1) {
        src = Variable("src", i);
        dst = Variable("dst", i);
        dests.add(dst);
        StayConstraint(src, NORMAL)
        ScaleConstraint(src, scale, offset, dst, REQUIRED)
    }

    change(src, 17);
    if (dst.value != 1170) print("Projection 1 failed");
    change(dst, 1050);
    if (src.value != 5) print("Projection 2 failed");
    change(scale, 5);
    for (i in 0..n - 2) {
        if (dests[i].value != i * 5 + 1000) print("Projection 3 failed");
    }
    change(offset, 2000);
    for (i in 0..n - 2) {
        if (dests[i].value != i * 5 + 2000) print("Projection 4 failed");
    }
}

fun change(v: Variable, newValue: Int) {
    val edit = EditConstraint(v, PREFERRED);
    val plan = planner.extractPlanFromConstraints(linkedListOf(edit));
    for (i in 0..9) {
        v.value = newValue
        plan.execute()
    }
    edit.destroyConstraint()
}

var planner: Planner = Planner();


inline fun linkedListOf<T>(d: T): LinkedList<T> {
    val a = LinkedList<T>()
    a.add(d)
    return a
}

val <T> List<T>.lastIndex : Int
    inline get() = this.size() - 1

fun box(): String = DeltaBlue.report().toString();
