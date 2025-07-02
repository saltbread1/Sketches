package mesh

import processing.core.PVector

class HalfEdgeMesh()
{
    private val vertices = mutableListOf<Vertex>()
    private val faces = mutableListOf<Face>()
    private val halfEdges = mutableListOf<HalfEdge>()

    // key: (origin, end)
    private val edgeToHalfEdge: MutableMap<Pair<Int, Int>, HalfEdge> = mutableMapOf()

    fun buildMesh(meshData: MeshData)
    {
        // initial vertices
        vertices.addAll(meshData.getVertices().asSequence().map { Vertex(it.copy(), null) })

        // create half-edges
        meshData.getFaces().forEachIndexed { face, (v0, v1, v2) ->
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

            // associate to the face
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
        edgeToHalfEdge.forEach { (key, edge) ->
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
    }

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
     * Get adjacent faces of the specified vertex.
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

        return result
    }

    /**
     * Get neighbor faces of the specified face.
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

        return result
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

        return result
    }

    fun getVertexCount() = vertices.size

    fun getFaceCount() = faces.size

    fun getEdgeCount() = halfEdges.count { it.face >= 0 } / 2

    fun getVertexPosition(vertex: Int): PVector?
    {
        return vertices.getOrNull(vertex)?.position
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
     * Insert a new vertex and split the specified edge.
     *
     * Before split:
     *     C
     *    / \
     *   /   \
     *  /     \
     * A-------B
     *  \     /
     *   \   /
     *    \ /
     *     D
     *
     * After split:
     *     C
     *    /|\
     *   / | \
     *  /  |  \
     * A---N---B
     *  \  |  /
     *   \ | /
     *    \|/
     *     D
     */
    fun splitEdge(edge: HalfEdge, position: PVector)
    {
        val edgeOpp = edge.opposite!!
        val edgeNext = edge.next!!
        val edgePrev = edge.prev!!
        val edgeOppNext = edgeOpp.next!!
        val edgeOppPrev = edgeOpp.prev!!

        // add a new vertex
        vertices.add(Vertex(position.copy(), null))
        val newVertex = vertices.lastIndex

        // create new edges (set only vertices)
        val newEdge0 = HalfEdge(edge.vertex, -1, null, null, null) // NB
        val newEdgeOpp0 = HalfEdge(edgeOpp.vertex, -1, null, null, null) // NA
        val newEdge1 = HalfEdge(newVertex, -1, null, null, null) // CN
        val newEdgeOpp1 = HalfEdge(edgeNext.vertex, -1, null, null, null) // NC
        val newEdge2 = HalfEdge(newVertex, -1, null, null, null) // DN
        val newEdgeOpp2 = HalfEdge(edgeOppNext.vertex, -1, null, null, null) // ND

        edge.vertex = newVertex

        // set outgoing
        vertices[newVertex].outgoing = newEdge0

        // add new faces and set to new edges
        val newFace0 = if (edge.face >= 0)
        {
            faces.add(Face(newEdge1))
            faces.lastIndex
        }
        else -1
        val newFace1 = if (edgeOpp.face >= 0)
        {
            faces.add(Face(newEdge2))
            faces.lastIndex
        }
        else -1
        newEdge0.face = newFace0
        newEdgeOpp0.face = newFace1
        newEdge1.face = newFace0
        newEdgeOpp1.face = edge.face
        newEdge2.face = newFace1
        newEdgeOpp2.face = edgeOpp.face

        // set connections of edges
        edge.next = newEdgeOpp1
        edge.opposite = newEdgeOpp0
        edgeOpp.next = newEdgeOpp2
        edgeOpp.opposite = newEdge0
        edgeNext.next = newEdge1
        edgeNext.prev = newEdge0
        edgePrev.prev = newEdgeOpp1
        edgeOppNext.next = newEdge2
        edgeOppNext.prev = newEdgeOpp0
        edgeOppPrev.prev = newEdgeOpp2

        newEdge0.next = edgeNext
        newEdge0.prev = edge
        newEdge0.opposite = edgeOpp
        newEdgeOpp0.next = edgeOppNext
        newEdgeOpp0.prev = edgeOpp
        newEdgeOpp0.opposite = edge

        newEdge1.next = newEdge0
        newEdge1.prev = edgeNext
        newEdge1.opposite = newEdgeOpp1
        newEdgeOpp1.next = edgePrev
        newEdgeOpp1.prev = edge
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
        }

        // check boundary loop
        val boundaryEdges = halfEdges.filter { it.face == -1 }
        if (boundaryEdges.any { it.next == null || it.prev == null })
        {
            errorMessages.append("Boundary edges not properly connected\n")
        }

        return errorMessages
    }
}
