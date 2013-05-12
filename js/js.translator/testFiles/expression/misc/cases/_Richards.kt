import js.Date
import js.println

class Richards() {
    val name: String = "Richards"

    // The benchmark code.
    // This function is not used, if both [warmup] and [exercise] are overwritten.
    //abstract fun run()

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

    fun report(): Double {
        val score = measure();
//        println("$name: ${NumberFormat.getInstance().format(score)} us.")
//        println("$name: ${score.toLong()} us.")
//        println("$name: ${score.toString("%f")} us.")
        println("$name: $score us.")
        return score
    }

        fun run() {
            val scheduler = Scheduler()
            scheduler.addIdleTask(ID_IDLE, 0, null, COUNT)

            var queue = Packet(null, ID_WORKER, KIND_WORK)
            queue = Packet(queue, ID_WORKER, KIND_WORK)
            scheduler.addWorkerTask(ID_WORKER, 1000, queue)

            queue = Packet(null, ID_DEVICE_A, KIND_DEVICE)
            queue = Packet(queue, ID_DEVICE_A, KIND_DEVICE)
            queue = Packet(queue, ID_DEVICE_A, KIND_DEVICE)
            scheduler.addHandlerTask(ID_HANDLER_A, 2000, queue)

            queue = Packet(null, ID_DEVICE_B, KIND_DEVICE)
            queue = Packet(queue, ID_DEVICE_B, KIND_DEVICE)
            queue = Packet(queue, ID_DEVICE_B, KIND_DEVICE)
            scheduler.addHandlerTask(ID_HANDLER_B, 3000, queue)

            scheduler.addDeviceTask(ID_DEVICE_A, 4000, null)

            scheduler.addDeviceTask(ID_DEVICE_B, 5000, null)

            scheduler.schedule()

            if (scheduler.queueCount != EXPECTED_QUEUE_COUNT || scheduler.holdCount != EXPECTED_HOLD_COUNT) {
                println("Error during execution: queueCount = ${scheduler.queueCount}, holdCount = ${scheduler.holdCount}.")
            }

    //        assertEquals(EXPECTED_QUEUE_COUNT, scheduler.queueCount)
    //        assertEquals(EXPECTED_HOLD_COUNT, scheduler.holdCount)
        }
}

    val DATA_SIZE = 4
    val COUNT = 1000

    /**
     * These two constants specify how many times a packet is queued and
     * how many times a task is put on hold in a correct run of richards.
     * They don't have any meaning a such but are characteristic of a
     * correct run so if the actual queue or hold count is different from
     * the expected there must be a bug in the implementation.
     **/
    val EXPECTED_QUEUE_COUNT = 2322
    val EXPECTED_HOLD_COUNT = 928

    val ID_IDLE = 0
    val ID_WORKER = 1
    val ID_HANDLER_A = 2
    val ID_HANDLER_B = 3
    val ID_DEVICE_A = 4
    val ID_DEVICE_B = 5

    val NUMBER_OF_IDS = 6

    val KIND_DEVICE = 0
    val KIND_WORK = 1


/**
 * A scheduler can be used to schedule a set of tasks based on their relative
 * priorities.  Scheduling is done by maintaining a list of task control blocks
 * which holds tasks and the data queue they are processing.
 */
class Scheduler {
    var queueCount = 0
    var holdCount = 0
    var currentTcb: TaskControlBlock? = null
    var currentId = 0
    var list: TaskControlBlock? = null
    val blocks = arrayOfNulls<TaskControlBlock>(NUMBER_OF_IDS)

    /// Add the specified task to this scheduler.
    fun addTask(id: Int, priority: Int, queue: Packet?, task: Task) {
        currentTcb = TaskControlBlock(list, id, priority, queue, task)
        list = currentTcb
        blocks[id] = currentTcb
    }

    /// Add the specified task and mark it as running.
    fun addRunningTask(id: Int, priority: Int, queue: Packet?, task: Task) {
        addTask(id, priority, queue, task)
        currentTcb?.setRunning()
    }

    /// Add an idle task to this scheduler.
    fun addIdleTask(id: Int, priority: Int, queue: Packet?, count: Int) {
        addRunningTask(id, priority, queue, IdleTask(this, 1, count))
    }

    /// Add a work task to this scheduler.
    fun addWorkerTask(id: Int, priority: Int, queue: Packet) {
        addTask(id, priority, queue, WorkerTask(this, ID_HANDLER_A, 0))
    }

    /// Add a handler task to this scheduler.
    fun addHandlerTask(id: Int, priority: Int, queue: Packet) {
        addTask(id, priority, queue, HandlerTask(this))
    }

    /// Add a handler task to this scheduler.
    fun addDeviceTask(id: Int, priority: Int, queue: Packet?) {
        addTask(id, priority, queue, DeviceTask(this))
    }

    /// Execute the tasks managed by this scheduler.
    fun schedule() {
        currentTcb = list
        while (currentTcb != null) {
            if (currentTcb!!.isHeldOrSuspended()) {
                currentTcb = currentTcb?.link
            } else {
                currentId = currentTcb!!.id
                currentTcb = currentTcb?.run()
            }
        }
    }

    /// Release a task that is currently blocked and return the next block to run.
    fun release(id: Int): TaskControlBlock? {
        val tcb = blocks[id]
        if (tcb == null)
            return tcb

        tcb.markAsNotHeld()

        if (tcb.priority > currentTcb!!.priority)
            return tcb

        return currentTcb
    }

    /**
     * Block the currently executing task and return the next task control block
     * to run.  The blocked task will not be made runnable until it is explicitly
     * released, even if new work is added to it.
     */
    fun holdCurrent(): TaskControlBlock? {
        holdCount++
        currentTcb?.markAsHeld()
        return currentTcb?.link
    }

    /**
     * Suspend the currently executing task and return the next task
     * control block to run.
     * If new work is added to the suspended task it will be made runnable.
     */
    fun suspendCurrent(): TaskControlBlock? {
        currentTcb?.markAsSuspended()
        return currentTcb
    }

    /**
     * Add the specified packet to the end of the worklist used by the task
     * associated with the packet and make the task runnable if it is currently
     * suspended.
     */
    fun queue(packet: Packet): TaskControlBlock? {
        val t = blocks[packet.id]
        if (t == null)
            return t

        queueCount++
        packet.link = null
        packet.id = currentId

        return t.checkPriorityAdd(currentTcb!!, packet)
    }
}

/// The task is running and is currently scheduled.
val STATE_RUNNING = 0

/// The task has packets left to process.
val STATE_RUNNABLE = 1

/**
 * The task is not currently running. The task is not blocked as such and may
 * be started by the scheduler.
 */
val STATE_SUSPENDED = 2

/// The task is blocked and cannot be run until it is explicitly released.
val STATE_HELD = 4

val STATE_SUSPENDED_RUNNABLE = STATE_SUSPENDED or STATE_RUNNABLE
val STATE_NOT_HELD = STATE_HELD.inv()

/**
 * A task control block manages a task and the queue of work packages associated
 * with it.
 */
class TaskControlBlock(
        var link: TaskControlBlock?,
        val id: Int, // The id of this block.
        val priority: Int, // The priority of this block.
        var queue: Packet?, // The queue of packages to be processed by the task.
        val task: Task
) {
    var state = if (queue == null) STATE_SUSPENDED else STATE_SUSPENDED_RUNNABLE

    fun setRunning() {
        state = STATE_RUNNING
    }

    fun markAsNotHeld() {
        state = state and STATE_NOT_HELD
    }

    fun markAsHeld() {
        state = state or STATE_HELD
    }

    fun isHeldOrSuspended(): Boolean =
            (state and STATE_HELD) != 0 || (state == STATE_SUSPENDED)

    fun markAsSuspended() {
        state = state or STATE_SUSPENDED
    }

    fun markAsRunnable() {
        state = state or STATE_RUNNABLE
    }

    /// Runs this task, if it is ready to be run, and returns the next task to run.
    fun run(): TaskControlBlock? {
        if (state == STATE_SUSPENDED_RUNNABLE) {
            val packet = queue
            queue = queue?.link
            state = if (queue == null) STATE_RUNNING else STATE_RUNNABLE
            return task.run(packet)
        }

        return task.run(null)
    }

    /**
     * Adds a packet to the worklist of this block's task, marks this as
     * runnable if necessary, and returns the next runnable object to run
     * (the one with the highest priority).
     */
    fun checkPriorityAdd(task: TaskControlBlock, packet: Packet): TaskControlBlock {
        if (queue == null) {
            queue = packet
            markAsRunnable()
            if (priority > task.priority)
                return this
        } else {
            queue = packet.addTo(queue)
        }
        return task
    }

    fun toString() = "tcb { ${task}@${state} }"
}

/**
 *  Abstract task that manipulates work packets.
 */
trait Task {
    val scheduler: Scheduler // The scheduler that manages this task.
    fun run(packet: Packet?): TaskControlBlock?
}

/**
 * An idle task doesn't do any work itself but cycles control between the two
 * device tasks.
 */
class IdleTask(override val scheduler: Scheduler,
               var v1: Int,   // A seed value that controls how the device tasks are scheduled
               var count: Int // The number of times this task should be scheduled.
) : Task {

    override fun run(packet: Packet?): TaskControlBlock? {
        count--
        if (count == 0)
            return scheduler.holdCurrent()
        if (v1 and 1 == 0) {
            v1 = v1 shr 1
            return scheduler.release(ID_DEVICE_A)
        }
        v1 = (v1 shr 1) xor 0xD008
        return scheduler.release(ID_DEVICE_B)
    }

    fun toString() = "IdleTask"
}


/**
* A task that suspends itself after each time it has been run to simulate
* waiting for data from an external device.
*/
class DeviceTask(override val scheduler: Scheduler) : Task {
    var v1: Packet? = null

    override fun run(packet: Packet?): TaskControlBlock? {
        if (packet == null) {
            if (v1 == null)
                return scheduler.suspendCurrent()
            val v = v1!!
            v1 = null
            return scheduler.queue(v)
        }
        v1 = packet
        return scheduler.holdCurrent()
    }

    fun toString() = "DeviceTask"
}


/**
* A task that manipulates work packets.
*/
class WorkerTask(override val scheduler: Scheduler,
                 var v1: Int, // A seed used to specify how work packets are manipulated.
                 var v2: Int  // Another seed used to specify how work packets are manipulated.
) : Task {
    override fun run(packet: Packet?): TaskControlBlock? {
        if (packet == null) {
            return scheduler.suspendCurrent()
        }

        if (v1 == ID_HANDLER_A) {
            v1 = ID_HANDLER_B
        } else {
            v1 = ID_HANDLER_A
        }

        packet.id = v1
        packet.a1 = 0
        for (i in 0..DATA_SIZE - 1) {
            v2++
            if (v2 > 26) v2 = 1
            packet.a2[i] = v2
        }

        return scheduler.queue(packet)
    }

    fun toString() = "WorkerTask"
}

/**
* A task that manipulates work packets and then suspends itself.
*/
class HandlerTask(override val scheduler: Scheduler) : Task {
    var v1: Packet? = null
    var v2: Packet? = null

    override fun run(packet: Packet?): TaskControlBlock? {
        if (packet != null) {
            if (packet.kind == KIND_WORK) {
                v1 = packet.addTo(v1)
            } else {
                v2 = packet.addTo(v2)
            }
        }
        val tV1 = v1
        if (tV1 != null) {
            val count = tV1.a1
            if (count < DATA_SIZE) {
                val tV2 = v2
                if (tV2 != null) {
                    v2 = tV2.link
                    tV2.a1 = tV1.a2[count]
                    tV1.a1 = count + 1
                    return scheduler.queue(tV2)
                }
            } else {
                v1 = tV1.link
                return scheduler.queue(tV1)
            }
        }
        return scheduler.suspendCurrent()
    }

    fun toString() = "HandlerTask"
}


/**
 * A simple package of data that is manipulated by the tasks.  The exact layout
 * of the payload data carried by a packet is not importaint, and neither is the
 * nature of the work performed on packets by the tasks.
 * Besides carrying data, packets form linked lists and are hence used both as
 * data and worklists.
 */
class Packet(var link: Packet?, var id: Int, val kind: Int) {
    var a1 = 0
    val a2 = IntArray(DATA_SIZE)

    /// Add this packet to the end of a worklist, and return the worklist.
    fun addTo(queue: Packet?): Packet {
        link = null
        if (queue == null)
            return this

        var p: Packet = queue
        while (p.link != null) {
            p = p.link!!
        }
        p.link = this

        return queue
    }
}

fun main(args: Array<String>) {
    Richards().report();
}

fun box(): String = Richards().report().toString();