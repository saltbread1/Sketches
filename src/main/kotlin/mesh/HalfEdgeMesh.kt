package mesh

import mesh.data.MeshData
import processing.core.PVector
import kotlin.math.max
import kotlin.math.min

/**
 * Represents a half-edge mesh structure, offering efficient traversal and manipulation
 * of vertices, edges, and faces in a mesh.
 * This implementation allows **only triangle meshes**.
 *
 * @property vertices  A list of all vertices in the mesh.
 * @property faces     A list of all faces in the mesh.
 * @property halfEdges A list of all half-edges in the mesh.
 */
class HalfEdgeMesh
{
    private val vertices = mutableListOf<Vertex>()
    private val faces = mutableListOf<Face>()
    private val halfEdges = mutableListOf<HalfEdge>()

    val faceNormals = mutableListOf<PVector>()
    val vertexNormals = mutableListOf<PVector>()

    fun buildMesh(meshData: MeshData)
    {
        vertices.clear()
        faces.clear()
        halfEdges.clear()

        // key: (origin, end)
        val edgeToHalfEdge: MutableMap<Pair<Int, Int>, HalfEdge> = mutableMapOf()

        // initial vertices
        vertices.addAll(meshData.vertices.asSequence().map { Vertex(it.copy(), null) })

        // create half-edges
        meshData.faces.forEachIndexed { face, (v0, v1, v2) ->
            // create half-edges
            val e0 = HalfEdge(v1, face, null, null, null)
            val e1 = HalfEdge(v2, face, null, null, null)
            val e2 = HalfEdge(v0, face, null, null, null)

            // set next and prev edges
            e0.next = e1
            e1.next = e2
            e2.next = e0
            e0.prev = e2
            e1.prev = e0
            e2.prev = e1

            // associate with the face
            faces.add(Face(e0))

            // add to the edge list
            halfEdges.add(e0)
            halfEdges.add(e1)
            halfEdges.add(e2)

            // add to the edge map
            edgeToHalfEdge[Pair(v0, v1)] = e0
            edgeToHalfEdge[Pair(v1, v2)] = e1
            edgeToHalfEdge[Pair(v2, v0)] = e2

            // set outgoing edges to vertices
            if (vertices[v0].outgoing == null) vertices[v0].outgoing = e0
            if (vertices[v1].outgoing == null) vertices[v1].outgoing = e1
            if (vertices[v2].outgoing == null) vertices[v2].outgoing = e2
        }

        // set opposite half-edges
        val edgeList = edgeToHalfEdge.toList()
        edgeList.forEach { (key, edge) ->
            val reverseKey = Pair(key.second, key.first)
            edge.opposite = edgeToHalfEdge[reverseKey]

            if (edge.opposite == null)
            {
                // create and add boundary edge
                val boundaryEdge = HalfEdge(key.first, -1, null, null, edge)
                halfEdges.add(boundaryEdge)
                edgeToHalfEdge[reverseKey] = boundaryEdge

                // set the new edge as an opposite edge
                edge.opposite = boundaryEdge

                // update outgoing edges of vertices to its directions toward the boundary
                vertices[key.second].outgoing = boundaryEdge
            }
        }

        // configure boundary edges
        val boundaryEdges = halfEdges.filter { it.face == -1 }.toList()
        boundaryEdges.forEach { e ->
            e.next = boundaryEdges.asSequence().find { it != e && it.opposite?.vertex == e.vertex }
            e.prev = boundaryEdges.asSequence().find { it != e && it.vertex == e.opposite?.vertex }
        }

        // calculate normals
        calculateNormals()
    }

    fun getVertexCount() = vertices.size

    fun getFaceCount() = faces.size

    /**
     * Get the number of half-edges **INCLUDING BOUNDARY**.
     */
    fun getAllHalfEdgeCount() = halfEdges.size

    /**
     * Get the number of half-edges **EXCLUDING BOUNDARY**.
     */
    fun getHalfEdgeCount() = halfEdges.count { it.face >= 0 }

    /**
     * Get the number of unique edges.
     */
    fun getEdgeCount() = halfEdges.size / 2

    fun getVertexPosition(vertex: Int): PVector?
    {
        return vertices.getOrNull(vertex)?.position?.copy()
    }

    fun setVertexPosition(vertex: Int, position: PVector): Boolean
    {
        return if (vertex in vertices.indices)
        {
            vertices[vertex].position = position
            true
        }
        else false
    }

    fun isBoundaryVertex(vertex: Int): Boolean
    {
        val start = vertices.getOrNull(vertex)?.outgoing ?: return false
        var current: HalfEdge? = start

        do
        {
            if (current?.face == -1) return true
            current = current?.opposite?.next
        }
        while (current != null && current != start)

        return false
    }

    fun isBoundaryEdge(edge: HalfEdge): Boolean = edge.face == -1

    /**
     * Get adjacent vertices of the specified vertex.
     */
    fun getAdjacentVertices(vertex: Int): List<Int>
    {
        if (vertex !in vertices.indices) return emptyList()

        val result = mutableListOf<Int>()
        val start = vertices[vertex].outgoing ?: return emptyList()
        var current: HalfEdge? = start

        do
        {
            current?.let { result.add(it.vertex) }
            current = current?.opposite?.next
        }
        while (current != null && current != start)

        return result.toList()
    }

    /**
     * Get the adjacent faces of the specified vertex.
     */
    fun getAdjacentFaces(vertex: Int): List<Int>
    {
        if (vertex !in vertices.indices) return emptyList()

        val result = mutableListOf<Int>()
        val start = vertices[vertex].outgoing ?: return emptyList()
        var current: HalfEdge? = start

        do
        {
            current?.let { if (it.face >= 0) result.add(it.face) }
            current = current?.opposite?.next
        }
        while (current != null && current != start)

        return result.toList()
    }

    /**
     * Get the neighbor faces of the specified face.
     */
    fun getFaceNeighbors(face: Int): List<Int>
    {
        if (face !in faces.indices) return emptyList()

        val result = mutableListOf<Int>()
        val start = faces[face].halfEdge
        var current: HalfEdge? = start

        do
        {
            current?.opposite?.let { opp ->
                if (opp.face >= 0) result.add(opp.face)
            }
            current = current?.next
        }
        while (current != null && current != start)

        return result.toList()
    }

    /**
     * Get vertices of the specified face.
     */
    fun getFaceVertices(face: Int): List<Int>
    {
        if (face !in faces.indices) return emptyList()

        val result = mutableListOf<Int>()
        val start = faces[face].halfEdge
        var current: HalfEdge? = start

        do
        {
            current?.opposite?.vertex?.let { result.add(it) }
            current = current?.next
        }
        while (current != null && current != start)

        return result.toList()
    }

    fun getSpecifiedHalfEdge(sourceVertex: Int, targetVertex: Int): HalfEdge?
    {
        return halfEdges.find { it.vertex == targetVertex && it.opposite?.vertex == sourceVertex }
    }

    fun getUniqueHalfEdges(): List<HalfEdge>
    {
        val uniqueEdges = mutableListOf<HalfEdge>()
        val processedEdges = mutableSetOf<Pair<Int, Int>>()

        for (halfEdge in halfEdges)
        {
            val opposite = halfEdge.opposite ?: continue
            val sourceVertex = opposite.vertex
            val targetVertex = halfEdge.vertex

            // get a normalized-edge key
            val edgeKey = Pair(min(sourceVertex, targetVertex), max(sourceVertex, targetVertex))

            // already have not processed
            if (edgeKey !in processedEdges)
            {
                // choose the half-edge begun to smaller vertex index
                val canonicalHalfEdge = if (sourceVertex < targetVertex) halfEdge else opposite

                uniqueEdges.add(canonicalHalfEdge)
                processedEdges.add(edgeKey)
            }
        }

        return uniqueEdges.toList()
    }

    fun forEachVertex(action: (vertex: Int, position: PVector) -> Unit)
    {
        vertices.forEachIndexed { index, vertex ->
            action(index, vertex.position.copy())
        }
    }

    fun forEachFace(action: (face: Int, vertices: List<Int>) -> Unit)
    {
        faces.forEachIndexed { index, _ ->
            action(index, getFaceVertices(index))
        }
    }

    fun forEachEdge(action: (sourceVertex: Int, targetVertex: Int) -> Unit)
    {
        val edges = getUniqueHalfEdges()
        edges.forEach { edge ->
            action(edge.opposite?.vertex ?: return@forEach, edge.vertex)
        }
    }

    /**
     * Insert a new vertex to the specified edge.
     * Three edges and two faces will be created and added to the mesh.
     *
     * Ex. A-->B is the specified edge and each-edge constructs CCW faces:
     *
     * Before split:
     * ```
     *     C
     *    / \
     *   /   \
     *  /     \
     * A-------B
     *  \     /
     *   \   /
     *    \ /
     *     D
     *```
     *
     * After split:
     * ```
     *     C
     *    /|\
     *   / | \
     *  /  |  \
     * A---N---B
     *  \  |  /
     *   \ | /
     *    \|/
     *     D
     * ```
     */
    fun splitEdge(edge: HalfEdge, position: PVector): Boolean
    {
        val edgeIn = if (edge.face == -1) edge.opposite ?: return false else edge

        val edgeOpp = edgeIn.opposite ?: return false// B -> A
        val edgeNext = edgeIn.next ?: return false // B -> C
        val edgePrev = edgeIn.prev ?: return false // C -> A
        val edgeOppNext = edgeOpp.next ?: return false // A -> D
        val edgeOppPrev = edgeOpp.prev ?: return false // D -> B

        val va = edgeOpp.vertex
        val vb = edgeIn.vertex
        val vc = edgeNext.vertex
        val vd = edgeOppNext.vertex

        // add a new vertex
        vertices.add(Vertex(position.copy(), null))
        val vn = vertices.lastIndex

        // create new edges (set only vertices)
        val newEdge0 = HalfEdge(vb, -1, null, null, null) // NB
        val newEdgeOpp0 = HalfEdge(va, -1, null, null, null) // NA
        val newEdge1 = HalfEdge(vn, -1, null, null, null) // CN
        val newEdgeOpp1 = HalfEdge(vc, -1, null, null, null) // NC
        val newEdge2 = HalfEdge(vn, -1, null, null, null) // DN
        val newEdgeOpp2 = HalfEdge(vd, -1, null, null, null) // ND

        edgeIn.vertex = vn
        edgeOpp.vertex = vn

        // set outgoing
        vertices[vn].outgoing = newEdge0

        // add new faces and set to new edges
        val newFace0 = if (edgeIn.face >= 0)
        {
            faces[edgeIn.face].halfEdge = edgeIn
            faces.add(Face(newEdge0))
            faces.lastIndex
        }
        else -1
        val newFace1 = if (edgeOpp.face >= 0)
        {
            faces[edgeOpp.face].halfEdge = edgeOpp
            faces.add(Face(newEdgeOpp0))
            faces.lastIndex
        }
        else -1
        edgeNext.face = newFace0
        edgeOppNext.face = newFace1
        newEdge0.face = newFace0
        newEdgeOpp0.face = newFace1
        newEdge1.face = newFace0
        newEdgeOpp1.face = edgeIn.face
        newEdge2.face = newFace1
        newEdgeOpp2.face = edgeOpp.face

        // set connections of edges
        edgeIn.next = newEdgeOpp1
        edgeIn.opposite = newEdgeOpp0
        edgeOpp.next = newEdgeOpp2
        edgeOpp.opposite = newEdge0
        edgeNext.next = newEdge1
        edgeNext.prev = newEdge0
        edgePrev.prev = newEdgeOpp1
        edgeOppNext.next = newEdge2
        edgeOppNext.prev = newEdgeOpp0
        edgeOppPrev.prev = newEdgeOpp2

        newEdge0.next = edgeNext
        newEdge0.prev = newEdge1
        newEdge0.opposite = edgeOpp
        newEdgeOpp0.next = edgeOppNext
        newEdgeOpp0.prev = newEdge2
        newEdgeOpp0.opposite = edgeIn

        newEdge1.next = newEdge0
        newEdge1.prev = edgeNext
        newEdge1.opposite = newEdgeOpp1
        newEdgeOpp1.next = edgePrev
        newEdgeOpp1.prev = edgeIn
        newEdgeOpp1.opposite = newEdge1

        newEdge2.next = newEdgeOpp0
        newEdge2.prev = edgeOppNext
        newEdge2.opposite = newEdgeOpp2
        newEdgeOpp2.next = edgeOppPrev
        newEdgeOpp2.prev = edgeOpp
        newEdgeOpp2.opposite = newEdge2

        // add edges
        halfEdges.add(newEdge0)
        halfEdges.add(newEdgeOpp0)
        halfEdges.add(newEdge1)
        halfEdges.add(newEdgeOpp1)
        halfEdges.add(newEdge2)
        halfEdges.add(newEdgeOpp2)

        // calculate normals
        calculateNormals()

        return true
    }

    /**
     * Split the edge specified by its vertices.
     */
    fun splitEdge(sourceVertex: Int, targetVertex: Int, position: PVector): Boolean
    {
        val edge = getSpecifiedHalfEdge(sourceVertex, targetVertex) ?: return false
        return splitEdge(edge, position)
    }

    /**
     * Subdivide the mesh by splitting each triangle into 4 triangles.
     * Each edge is split at its midpoint, creating new vertices.
     *
     * @param midPointStrategy a strategy to calculate the coordinate of the middle point vertex
     */
    fun subdivide(midPointStrategy: (PVector, PVector) -> PVector = { v0, v1 -> PVector.lerp(v0, v1, 0.5f) })
    {
        val newMesh = object : MeshData
        {
            override val vertices = mutableListOf<PVector>()
            override val faces = mutableListOf<Triple<Int, Int, Int>>()
            val edgeMidpoints = mutableMapOf<Pair<Int, Int>, Int>()

            init
            {
                val edges = getUniqueHalfEdges()

                vertices.addAll(this@HalfEdgeMesh.vertices.map { it.position.copy() })

                edges.forEach { edge ->
                    val sourceVertex = edge.opposite?.vertex ?: return@forEach
                    val targetVertex = edge.vertex

                    val v0 = this@HalfEdgeMesh.vertices[sourceVertex].position
                    val v1 = this@HalfEdgeMesh.vertices[targetVertex].position
                    val mid = midPointStrategy.invoke(v0, v1)
                    vertices.add(mid)
                    edgeMidpoints[Pair(min(sourceVertex, targetVertex), max(sourceVertex, targetVertex))] =
                        vertices.lastIndex
                }

                this@HalfEdgeMesh.faces.forEach { face ->
                    val edge = face.halfEdge
                    val next = edge.next ?: return@forEach
                    val prev = edge.prev ?: return@forEach

                    val v0 = edge.vertex
                    val v1 = next.vertex
                    val v2 = prev.vertex
                    val m01 = edgeMidpoints[Pair(min(v0, v1), max(v0, v1))] ?: return@forEach
                    val m12 = edgeMidpoints[Pair(min(v1, v2), max(v1, v2))] ?: return@forEach
                    val m20 = edgeMidpoints[Pair(min(v2, v0), max(v2, v0))] ?: return@forEach

                    faces.add(Triple(v0, m01, m20))
                    faces.add(Triple(m01, v1, m12))
                    faces.add(Triple(m20, m12, v2))
                    faces.add(Triple(m01, m12, m20))
                }
            }
        }

        buildMesh(newMesh)
    }

    /**
     * Mesh validation.
     */
    fun validate(): StringBuilder
    {
        val errorMessages = StringBuilder()

        // check structure
        halfEdges.forEachIndexed { index, edge ->
            if (edge.next == null)
            {
                errorMessages.append("HalfEdge $index: null next\n")
            }
            if (edge.prev == null)
            {
                errorMessages.append("HalfEdge $index: null prev\n")
            }
            if (edge.opposite == null)
            {
                errorMessages.append("HalfEdge $index: null opposite\n")
            }

            if (edge.next?.prev != edge || edge.prev?.next != edge || edge.opposite?.opposite != edge)
            {
                errorMessages.append("HalfEdge $index: connection inconsistency\n")
            }

            if (edge.face != -1 && (edge.next?.next?.next != edge || edge.prev?.prev?.prev != edge))
            {
                errorMessages.append("HalfEdge $index: interior loop inconsistency\n")
            }

            if (edge.face != edge.next?.face || edge.face != edge.prev?.face)
            {
                errorMessages.append("HalfEdge $index: edge face inconsistency\n")
            }
        }

        // check boundary loop
        val numBoundaryEdges = halfEdges.count { it.face == -1 }
        val first = halfEdges.find { it.face == -1 }
        var boundary0 = first
        repeat(numBoundaryEdges) { boundary0 = boundary0?.next }
        var boundary1 = first
        repeat(numBoundaryEdges) { boundary1 = boundary1?.prev }
        if (boundary0 != first || boundary1 != first)
        {
            errorMessages.append("Boundary loop inconsistency\n")
        }

        return errorMessages
    }

    private fun calculateNormals()
    {
        val faceAreas = mutableListOf<Float>()
        faceNormals.clear()
        vertexNormals.clear()

        faces.indices.forEach { idx ->
            val vertices = getFaceVertices(idx)
            val p0 = getVertexPosition(vertices[0])
            val p1 = getVertexPosition(vertices[1])
            val p2 = getVertexPosition(vertices[2])
            val v0 = PVector.sub(p1, p0)
            val v1 = PVector.sub(p2, p0)
            val cross = PVector.cross(v0, v1, null)
            val crossMag = cross.mag()
            faceAreas.add(crossMag * 0.5f)
            faceNormals.add(cross.div(crossMag))
        }
        vertexNormals.addAll(vertices.mapIndexed { idx, _ ->
            getAdjacentFaces(idx).map { PVector.mult(faceNormals[it], faceAreas[it]) }
                .reduce { acc, n -> PVector.add(acc, n) }.normalize()
        })
    }
}
