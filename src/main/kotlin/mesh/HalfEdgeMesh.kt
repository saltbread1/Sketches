package mesh

class HalfEdgeMesh(meshData: MeshData)
{
    private val vertices = mutableListOf<Vertex>()
    private val faces = mutableListOf<Face>()
    private val halfEdges = mutableListOf<HalfEdge>()

    private val edgeToHalfEdge: MutableMap<Pair<Int, Int>, HalfEdge> = mutableMapOf()

    init
    {
        // initial vertices
        vertices.addAll(meshData.getVertices().asSequence().map { Vertex(it.copy(), null) })

        // create half-edges
        meshData.getFaces().forEachIndexed { face, (v0, v1, v2) ->
            // create half-edges
            val e0 = HalfEdge(v0, face, null, null, null)
            val e1 = HalfEdge(v1, face, null, null, null)
            val e2 = HalfEdge(v2, face, null, null, null)

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
            edgeToHalfEdge[Pair<Int, Int>(v0, v1)] = e0
            edgeToHalfEdge[Pair<Int, Int>(v1, v2)] = e1
            edgeToHalfEdge[Pair<Int, Int>(v2, v0)] = e2

            // set outgoing edges to vertices
            if (vertices[v0].outgoing == null) { vertices[v0].outgoing = e0 }
            if (vertices[v1].outgoing == null) { vertices[v1].outgoing = e1 }
            if (vertices[v2].outgoing == null) { vertices[v2].outgoing = e2 }
        }

        // set opposite half-edges
        edgeToHalfEdge.forEach { (key, edge) ->
            edge.opposite = edgeToHalfEdge[Pair(key.second, key.first)]
            if (edge.opposite == null)
            {
                // create and add boundary edge
                val e = HalfEdge(key.second, -1, null, null, edge)
                halfEdges.add(e)
                edgeToHalfEdge[Pair(key.second, key.first)] = e

                // set the new edge as an opposite edge
                edge.opposite = e

                // update outgoing edges of vertices to its directions toward the boundary
                vertices[key.second].outgoing = e
            }
        }

        // configure boundary edges
        val boundaryEdges = halfEdges.asSequence().filter { it.face == -1 }.toList()
        boundaryEdges.forEach { e ->
            e.next = boundaryEdges.asSequence().find { it != e && it.vertex == e.opposite?.vertex }
            e.prev = boundaryEdges.asSequence().find { it != e && it.opposite?.vertex == e.vertex }
        }
    }
}
