import js.Date
import java.util.ArrayList
import java.util.HashMap

// Copyright 2012 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//======================================================
// Scaffold Code
//======================================================

// BasicBlock's static members
//
var numBasicBlocks = 0

//
// class BasicBlock
//
// BasicBlock only maintains a vector of in-edges and
// a vector of out-edges.
//
class BasicBlock(val name: Int) //: Hashable
{
//    public override fun equals(other: Any?): Boolean {
//        throw UnsupportedOperationException()
//    }

    val inEdges = ArrayList<BasicBlock>()
    val outEdges = ArrayList<BasicBlock>()

    fun hashCode() = name

    {
        numBasicBlocks = numBasicBlocks + 1;
    }

    fun toString() = "BB$name"
    fun getNumPred() = inEdges.size()
    fun getNumSucc() = outEdges.size()
    fun addInEdge(bb: BasicBlock) = inEdges.add(bb);
    fun addOutEdge(bb: BasicBlock) = outEdges.add(bb);

    fun dump() {
        var res = "  BB#$name"
        if (!inEdges.isEmpty()) {
            res += "\tin :";
            for (e in inEdges) {
                res += " " + e.toString();
            }
        }

        if (!outEdges.isEmpty()) {
            res += "\tout:";
            for (e in outEdges) {
                res += " " + e.toString();
            }
        }

        println(res);
    }
}

//
// class BasicBlockEdge
//
// These data structures are stubbed out to make the code below easier
// to review.
//
// BasicBlockEdge only maintains two pointers to BasicBlocks.
// Note: from is apparently a keyword in python. Changed to uppercase
//
class BasicBlockEdge(cfg: CFG, fromName: Int, toName: Int) {
    val From = cfg.createNode(fromName)
    val To = cfg.createNode(toName)

    ;{
        From.addOutEdge(To)
        To.addInEdge(From)
        cfg.addEdge(this)
    }
}


//
// class CFG
//
// CFG maintains a list of nodes, plus a start node.
// That's it.
//
class CFG {
    val basicBlockMap = HashMap<Int, BasicBlock>()
    val edgeList = ArrayList<BasicBlockEdge>()
    var startNode: BasicBlock? = null;

    fun createNode(name: Int): BasicBlock {
        fun createAndRegisterNode(that: CFG, name: Int) : BasicBlock {
            val newNode = BasicBlock(name)
            that.basicBlockMap.put(name, newNode)
            return newNode
        }

        val node = basicBlockMap[name] ?: createAndRegisterNode(this, name)

        if (startNode == null) {
            startNode = node;
        }
        return node;
    }

    void dump() {
        for (k in basicBlockMap.values()) {
            k.dump();
        }
    }

    fun addEdge(edge: BasicBlockEdge) = edgeList.add(edge)

    fun getNumNodes() = basicBlockMap.size();

    fun getDst(edge: BasicBlockEdge) = edge.To;
    fun getSrc(edge: BasicBlockEdge) = edge.From;
}

//
// class SimpleLoop
//
// Basic representation of loops, a loop has an entry point,
// one or more exit edges, a set of basic blocks, and potentially
// an outer loop - a "parent" loop.
//
// Furthermore, it can have any set of properties, e.g.,
// it can be an irreducible loop, have control flow, be
// a candidate for transformations, and what not.
//
class SimpleLoop // : Hashable
{

//    public override fun equals(other: Any?): Boolean {
//        throw UnsupportedOperationException()
//    }

    val basicBlocks = ArrayList<BasicBlock>();
    val children = ArrayList<SimpleLoop>()
    var parent: SimpleLoop? = null
        set(p: SimpleLoop?) {
            $parent = p;
            p?.addChildLoop(this);
        }
    var header: BasicBlock? = null
        set(bb: BasicBlock?) {
            basicBlocks.add(bb!!)
            $header = bb
        }

    var isRoot = false
    var isReducible = true
    var counter = 0
    var nestingLevel = 0
        set(level: Int) {
            $nestingLevel = level;
            if (level == 0) {
                isRoot = true;
            }
        }
    var depthLevel = 0

    fun hashCode() = counter

    fun addNode(bb: BasicBlock) = basicBlocks.add(bb)
    fun addChildLoop(loop: SimpleLoop) = children.add(loop)

    fun dump(indent: Int) {
        var res = ""
        for (i in 0..indent - 1) {
            res += "  ";
        }

        print("$res loop-$counter, nest: $nestingLevel, depth $depthLevel");
        if (isReducible == false) {
            print("  irreducible");
        }
    }
}


//
// LoopStructureGraph
//
// Maintain loop structure for a given CFG.
//
// Two values are maintained for this loop graph, depth, and nesting level.
// For example:
//
// loop        nesting level    depth
//----------------------------------------
// loop-0      2                0
//   loop-1    1                1
//   loop-3    1                1
//     loop-2  0                2
//
class LSG {
    var loopCounter = 0;
    val loops = ArrayList<SimpleLoop>()
    val root = SimpleLoop();

    {
        root.nestingLevel = 0
        root.counter = loopCounter
        loopCounter++
        loops.add(this.root)
    }

    fun createNewLoop(): SimpleLoop {
        val loop = SimpleLoop()
        loop.counter = loopCounter;
        loopCounter++
        return loop
    }

    fun addLoop(loop: SimpleLoop) = loops.add(loop)

    fun dumpRec(loop: SimpleLoop, indent: Int) {
        loop.dump(indent);

        for (c in loop.children) {
            dumpRec(c, indent + 1);
        }
    }

    fun dump() = dumpRec(root,0)

    fun max(a: Int, b: Int) = if (a>b) a else b

    fun calculateNestingLevelRec(loop: SimpleLoop, depth: Int) {
        loop.depthLevel = depth;
        for (c in loop.children) {
            calculateNestingLevelRec(c, depth + 1);

            loop.nestingLevel = max(loop.nestingLevel, 1 + c.nestingLevel)
        }
    }

    fun calculateNestingLevel() {
        for (l in this.loops) {
            if (!l.isRoot) {
                if (l.parent == null) {
                    l.parent = this.root
                }
            }
        }

        this.calculateNestingLevelRec(this.root, 0);
    }

    fun getNumLoops() = loops.size();
}


//======================================================
// Main Algorithm
//======================================================

//
// class UnionFindNode
//
// The algorithm uses the Union/Find algorithm to collapse
// complete loops into a single node. These nodes and the
// corresponding functionality are implemented with this class
//
class UnionFindNode {
    var dfsNumber = 0
    var parent: UnionFindNode? = null
    var bb: BasicBlock? = null
    var loop: SimpleLoop? = null

    // Initialize this node.
    //
    fun initNode(bb_arg: BasicBlock, dfsNumber_arg: Int) {
        parent = this
        bb = bb_arg
        dfsNumber = dfsNumber_arg
        loop = null
    }

    // Union/Find Algorithm - The find routine.
    //
    // Implemented with Path Compression (inner loops are only
    // visited and collapsed once, however, deep nests would still
    // result in significant traversals).
    //
    fun findSet(): UnionFindNode {
        val nodeList = ArrayList<UnionFindNode>();

        var node = this;
        while (node != node.parent) {
            if (node.parent != node.parent?.parent)
                nodeList.add(node);

            node = node.parent!!;
        }

        // Path Compression, all nodes' parents point to the 1st level parent.
        for (n in nodeList) {
            n.parent = node.parent;
        }

        return node;
    }

    // Union/Find Algorithm - The union routine.
    //
    // Trivial. Assigning parent pointer is enough,
    // we rely on path compression.
    //
    fun union(unionFindNode: UnionFindNode) {
        parent = unionFindNode;
    }
    fun setLoop(l: SimpleLoop): SimpleLoop {
        loop = l
        return l
    }
}

class HavlakLoopFinder(val cfg: CFG, val lsg: LSG) {

    //
    // enum BasicBlockClass
    //
    // Basic Blocks and Loops are being classified as regular, irreducible,
    // and so on. This enum contains a symbolic name for all these
    // classifications. Python doesn't have enums, so we just create values.
    //
    val BB_TOP = 0
    val BB_NONHEADER = 1
    val BB_REDUCIBLE = 2
    val BB_SELF = 3
    val BB_IRREDUCIBLE = 4
    val BB_DEAD = 5
    val BB_LAST = 6

    //
    // Constants
    //
    // Marker for uninitialized nodes.
    val UNVISITED = -1

    // Safeguard against pathologic algorithm behavior.
    val MAXNONBACKPREDS = 32 * 1024

    //
    // IsAncestor
    //
    // As described in the paper, determine whether a node 'w' is a
    // "true" ancestor for node 'v'.
    //
    // Dominance can be tested quickly using a pre-order trick
    // for depth-first spanning trees. This is why DFS is the first
    // thing we run below.
    //
    fun isAncestor(w: Int, v: Int, last: List<Int>): Boolean {
        return (w <= v) && (v <= last[w]);
    }

    //
    // DFS - Depth-First-Search
    //
    // DESCRIPTION:
    // Simple depth first traversal along out edges with node numbering.
    //
    fun DFS(currentNode: BasicBlock,
            nodes: List<UnionFindNode>,
            number: MutableMap<BasicBlock, Int>,
            last: MutableList<Int>,
            current: Int): Int {
        nodes[current].initNode(currentNode, current)
        number.put(currentNode, current)

        var lastid = current
        for (target in currentNode.outEdges) {
            if (number[target] == UNVISITED)
                lastid = DFS(target, nodes, number, last, lastid + 1);
        }

        last[number[currentNode]!!] = lastid;
        return lastid;
    }

    //
    // findLoops
    //
    // Find loops and build loop forest using Havlak's algorithm, which
    // is derived from Tarjan. Variable names and step numbering has
    // been chosen to be identical to the nomenclature in Havlak's
    // paper (which, in turn, is similar to the one used by Tarjan).
    //
    fun findLoops(): Int {
        if (cfg.startNode == null) {
            return 0;
        }

        val size = cfg.getNumNodes();

        val nonBackPreds = ArrayList<ArrayList<Int>>(size)
        val backPreds = ArrayList<ArrayList<Int>>(size)
        val number = HashMap<BasicBlock, Int>()
        val header = ArrayList<Int>(size)
        val types = ArrayList<Int>(size)
        val last = ArrayList<Int>(size)
        val nodes   = ArrayList<UnionFindNode>(size)

        for (i in 1..size) {
            nonBackPreds.add(ArrayList<Int>())
            backPreds.add(ArrayList<Int>())
            header.add(0)
            types.add(0)
            last.add(0)
            nodes.add(UnionFindNode())
        }

        // Step a:
        //   - initialize all nodes as unvisited.
        //   - depth-first traversal and numbering.
        //   - unreached BB's are marked as dead.
        //
        for (bb in cfg.basicBlockMap.values()) {
            number.put(bb, UNVISITED)
        }

        DFS(cfg.startNode!!, nodes, number, last, 0);

        // Step b:
        //   - iterate over all nodes.
        //
        //   A backedge comes from a descendant in the DFS tree, and non-backedges
        //   from non-descendants (following Tarjan).
        //
        //   - check incoming edges 'v' and add them to either
        //     - the list of backedges (backPreds) or
        //     - the list of non-backedges (nonBackPreds)
        //
        for (w in 0..size - 1) {
            header[w] = 0;
            types[w]  = BB_NONHEADER

            val nodeW = nodes[w].bb
            if (nodeW == null) {
                types[w] = BB_DEAD;
            } else {
                if (nodeW.getNumPred() > 0) {
                    for (nodeV in nodeW.inEdges) {
                        val v = number[nodeV]!!
                        if (v != UNVISITED) {
                            if (isAncestor(w, v, last)) {
                                backPreds[w].add(v);
                            } else {
                                nonBackPreds[w].add(v);
                            }
                        }
                    }
                }
            }
        }

        // Start node is root of all other loops.
        header[0] = 0;

        // Step c:
        //
        // The outer loop, unchanged from Tarjan. It does nothing except
        // for those nodes which are the destinations of backedges.
        // For a header node w, we chase backward from the sources of the
        // backedges adding nodes to the set P, representing the body of
        // the loop headed by w.
        //
        // By running through the nodes in reverse of the DFST preorder,
        // we ensure that inner loop headers will be processed before the
        // headers for surrounding loops.
        //
        for (w in size - 1 downTo 0) {
            // this is 'P' in Havlak's paper
            val nodePool = ArrayList<UnionFindNode>();

            val nodeW = nodes[w].bb
            if (nodeW == null) {
                continue;
            }

            // Step d:
            for (v in backPreds[w]) {
                if (v != w) {
                    nodePool.add(nodes[v].findSet());
                } else {
                    types[w] = BB_SELF;
                }
            }

            // Copy nodePool to workList.
            //
            var workList: jet.MutableList<UnionFindNode> = ArrayList_<UnionFindNode>(nodePool) //todo ?
            //            for (int n = 0; n < nodePool.length; ++n) {
            //                  workList.add(nodePool[n]);
            //            }

            if (!nodePool.isEmpty()) {
                types[w] = BB_REDUCIBLE;
            }
            // work the list...
            //
            while (!workList.isEmpty()) {
                val x = workList[0];
                workList = workList.subList(0, workList.size() - 1);

                // Step e:
                //
                // Step e represents the main difference from Tarjan's method.
                // Chasing upwards from the sources of a node w's backedges. If
                // there is a node y' that is not a descendant of w, w is marked
                // the header of an irreducible loop, there is another entry
                // into this loop that avoids w.
                //

                // The algorithm has degenerated. Break and
                // return in this case.
                //
                val nonBackSize = nonBackPreds[x.dfsNumber].size()
                if (nonBackSize > MAXNONBACKPREDS) {
                    return 0;
                }

                for (iter in nonBackPreds[x.dfsNumber]) {
                    val y = nodes[iter]
                    val ydash = y.findSet()

                    if (!isAncestor(w, ydash.dfsNumber, last)) {
                        types[w] = BB_IRREDUCIBLE;
                        nonBackPreds[w].add(ydash.dfsNumber);
                    } else {
                        if (ydash.dfsNumber != w) {
                            if (nodePool.indexOf(ydash) == -1) {
                                workList.add(ydash);
                                nodePool.add(ydash);
                            }
                        }
                    }
                }
            }

            // Collapse/Unionize nodes in a SCC to a single node
            // For every SCC found, create a loop descriptor and link it in.
            //
            if ((nodePool.size() > 0) || (types[w] == BB_SELF)) {
                val loop = lsg.createNewLoop();

                loop.header = nodeW
                if (types[w] == BB_IRREDUCIBLE) {
                    loop.isReducible = true;
                } else {
                    loop.isReducible = false;
                }

                // At this point, one can set attributes to the loop, such as:
                //
                // the bottom node:
                //    iter  = backPreds(w).begin();
                //    loop bottom is: nodes(iter).node;
                //
                // the number of backedges:
                //    backPreds(w).size()
                //
                // whether this loop is reducible:
                //    types(w) != BB_IRREDUCIBLE
                //
                nodes[w].loop = loop;

                for (node in nodePool) {
                    // Add nodes to loop descriptor.
                    header[node.dfsNumber] = w;
                    node.union(nodes[w]);

                    // Nested loops are not added, but linked together.
                    val l = node.loop
                    if (l != null) {
                        l.parent = loop
                    } else {
                        loop.addNode(node.bb!!);
                    }
                }
                lsg.addLoop(loop);
            } // nodePool.length
        } // Step c

        return lsg.getNumLoops();
    } // findLoops
} // HavlakLoopFinder


//======================================================
// Testing Code
//======================================================

fun buildDiamond(cfg: CFG, start: Int): Int {
    val bb0 = start
    BasicBlockEdge(cfg, bb0, bb0 + 1)
    BasicBlockEdge(cfg, bb0, bb0 + 2)
    BasicBlockEdge(cfg, bb0 + 1, bb0 + 3)
    BasicBlockEdge(cfg, bb0 + 2, bb0 + 3)
    return bb0 + 3
}


fun buildConnect(cfg: CFG, start: Int, end: Int) {
    BasicBlockEdge(cfg, start, end);
}

fun buildStraight(cfg: CFG, start: Int, n: Int): Int {
    for (i in 0..n - 1) {
        buildConnect(cfg, start + i, start + i + 1);
    }
    return start + n;
}

fun buildBaseLoop(cfg: CFG, from: Int): Int {
    val header   = buildStraight(cfg, from, 1);
    val diamond1 = buildDiamond(cfg, header);
    val d11      = buildStraight(cfg, diamond1, 1);
    val diamond2 = buildDiamond(cfg, d11);
    val footer   = buildStraight(cfg, diamond2, 1);
    buildConnect(cfg, diamond2, d11);
    buildConnect(cfg, diamond1, header);
    buildConnect(cfg, footer, from);

    return buildStraight(cfg, footer, 1)
}

fun test() {
    println("Welcome to LoopTesterApp, Kotlin edition")

    val cfg = CFG()
    val lsg = LSG()

    println("Constructing Simple CFG...");

    cfg.createNode(0);  // top
    buildBaseLoop(cfg, 0);
    cfg.createNode(1);  //s bottom
    buildConnect(cfg, 0, 2);

    // execute loop recognition 15000 times to force compilation
    println("15000 dummy loops");

    for (i in 1..15000) {
        val finder = HavlakLoopFinder(cfg, lsg)
        finder.findLoops()
    }

    println("Constructing CFG...");
    var n = 2;

    for (i in 1..10) {
        cfg.createNode(n + 1);
        buildConnect(cfg, 2, n + 1);
        n++
        for (i in 1..100) {
            val top = n;
            n = buildStraight(cfg, n, 1);
            for (i in 1..25) {
                n = buildBaseLoop(cfg, n);
            }

            val bottom = buildStraight(cfg, n, 1);
            buildConnect(cfg, n, top);
            n = bottom;
        }
        buildConnect(cfg, n, 1);
    }

    println("Performing Loop Recognition\n1 Iteration");

    val finder = HavlakLoopFinder(cfg, lsg)
    val num_loops = finder.findLoops()
//    lsg.calculateNestingLevel()

    println("Found: $num_loops.")
    assertEquals(121002, num_loops)

    println("Another 50 iterations...")

    val start = Date().getTime()
    for (i in 1..50) {
        print(".")
        val finder2 = HavlakLoopFinder(cfg, LSG());
        finder2.findLoops();
    }
    println("\nTime: ${Date().getTime() - start} ms")
    println()

    println("lsg loops: " + (lsg.getNumLoops()) + " (including 1 artificial root node)")

    println("# of BBs  : " + numBasicBlocks)

    assertEquals(121002, lsg.getNumLoops())
    assertEquals(252013, numBasicBlocks)

//    lsg.calculateNestingLevel();
}

fun assertEquals(a: Int, b: Int) {
    if (a != b) {
        val msg = "expected: $a; actual: $b"
        println(msg)
        throw RuntimeException(msg)
    }
}

fun ArrayList_<T>(c: Collection<T>): ArrayList<T> {
    val al = ArrayList<T>(c.size())
    al.addAll(c)
    return al
}

fun box(): String {
    test()
    return "OK"
}
