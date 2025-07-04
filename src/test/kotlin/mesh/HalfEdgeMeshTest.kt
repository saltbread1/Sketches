package mesh

import processing.core.PVector
import kotlin.test.*

class HalfEdgeMeshTest
{

    class SimpleMeshData(
        private val vertices: List<PVector>,
        private val faces: List<Triple<Int, Int, Int>>
    ) : MeshData
    {
        override fun getVertices(): Collection<PVector> = vertices
        override fun getFaces(): Collection<Triple<Int, Int, Int>> = faces
    }

    private fun createPlaneMesh(): SimpleMeshData
    {
        val vertices = listOf(
            PVector(0f, 0f, 0f),    // 0
            PVector(1f, 0f, 0f),    // 1
            PVector(1f, 1f, 0f),    // 2
            PVector(0f, 1f, 0f)     // 3
        )
        val faces = listOf(
            Triple(0, 1, 2),
            Triple(0, 2, 3)
        )
        return SimpleMeshData(vertices, faces)
    }

    private fun createTetrahedronMesh(): SimpleMeshData
    {
        val vertices = listOf(
            PVector(0f, 0f, 0f),         // 0
            PVector(1f, 0f, 0f),         // 1
            PVector(0.5f, 1f, 0f),       // 2
            PVector(0.5f, 0.5f, 1f)      // 3
        )
        val faces = listOf(
            Triple(0, 1, 2),
            Triple(0, 2, 3),
            Triple(0, 3, 1),
            Triple(1, 3, 2)
        )
        return SimpleMeshData(vertices, faces)
    }

    @Test
    fun testPlaneBasicProperties()
    {
        val mesh = HalfEdgeMesh()
        val planeMeshData = createPlaneMesh()
        mesh.buildMesh(planeMeshData)

        assertEquals(4, mesh.getVertexCount())
        assertEquals(2, mesh.getFaceCount())
        assertEquals(6, mesh.getHalfEdgeCount())
        assertEquals(10, mesh.getAllHalfEdgeCount())
        assertEquals(5, mesh.getEdgeCount())

        assertTrue(mesh.isBoundaryVertex(0))
        assertTrue(mesh.isBoundaryVertex(1))
        assertTrue(mesh.isBoundaryVertex(2))
        assertTrue(mesh.isBoundaryVertex(3))
    }

    @Test
    fun testTetrahedronBasicProperties()
    {
        val mesh = HalfEdgeMesh()
        val tetrahedronMeshData = createTetrahedronMesh()
        mesh.buildMesh(tetrahedronMeshData)

        assertEquals(4, mesh.getVertexCount())
        assertEquals(4, mesh.getFaceCount())
        assertEquals(12, mesh.getHalfEdgeCount())
        assertEquals(12, mesh.getAllHalfEdgeCount())
        assertEquals(6, mesh.getEdgeCount())

        assertFalse(mesh.isBoundaryVertex(0))
        assertFalse(mesh.isBoundaryVertex(1))
        assertFalse(mesh.isBoundaryVertex(2))
        assertFalse(mesh.isBoundaryVertex(3))
    }

    @Test
    fun testVertexPositionOperations()
    {
        val mesh = HalfEdgeMesh()
        val planeMeshData = createPlaneMesh()
        mesh.buildMesh(planeMeshData)

        val pos = mesh.getVertexPosition(0)
        assertNotNull(pos)
        assertEquals(0f, pos.x)
        assertEquals(0f, pos.y)
        assertEquals(0f, pos.z)

        val newPos = PVector(2f, 2f, 2f)
        assertTrue(mesh.setVertexPosition(0, newPos))

        val updatedPos = mesh.getVertexPosition(0)
        assertNotNull(updatedPos)
        assertEquals(2f, updatedPos.x)
        assertEquals(2f, updatedPos.y)
        assertEquals(2f, updatedPos.z)

        assertNull(mesh.getVertexPosition(10))
        assertFalse(mesh.setVertexPosition(10, PVector(0f, 0f, 0f)))
    }

    @Test
    fun testAdjacentVertices()
    {
        val mesh = HalfEdgeMesh()
        val planeMeshData = createPlaneMesh()
        mesh.buildMesh(planeMeshData)

        val adjacentTo0 = mesh.getAdjacentVertices(0)
        assertTrue(adjacentTo0.contains(1))
        assertTrue(adjacentTo0.contains(2))
        assertTrue(adjacentTo0.contains(3))
        assertEquals(3, adjacentTo0.size, "Vertex 0 should have exactly 3 adjacent vertices but got: $adjacentTo0")
        
        val adjacentTo2 = mesh.getAdjacentVertices(2)
        assertTrue(adjacentTo2.contains(0))
        assertTrue(adjacentTo2.contains(1))
        assertTrue(adjacentTo2.contains(3))
        assertEquals(3, adjacentTo2.size, "Vertex 2 should have exactly 3 adjacent vertices but got: $adjacentTo2")
    }

    @Test
    fun testAdjacentFaces()
    {
        val mesh = HalfEdgeMesh()
        val planeMeshData = createPlaneMesh()
        mesh.buildMesh(planeMeshData)

        val facesOf0 = mesh.getAdjacentFaces(0)
        assertTrue(facesOf0.contains(0))
        assertTrue(facesOf0.contains(1))
        assertEquals(2, facesOf0.size)

        val facesOf2 = mesh.getAdjacentFaces(2)
        assertTrue(facesOf2.contains(0))
        assertTrue(facesOf2.contains(1))
        assertEquals(2, facesOf2.size)
    }

    @Test
    fun testFaceNeighbors()
    {
        val mesh = HalfEdgeMesh()
        val planeMeshData = createPlaneMesh()
        mesh.buildMesh(planeMeshData)

        val neighborsOf0 = mesh.getFaceNeighbors(0)
        assertTrue(neighborsOf0.contains(1))
        assertEquals(1, neighborsOf0.size)

        val neighborsOf1 = mesh.getFaceNeighbors(1)
        assertTrue(neighborsOf1.contains(0))
        assertEquals(1, neighborsOf1.size)
    }

    @Test
    fun testFaceVertices()
    {
        val mesh = HalfEdgeMesh()
        val planeMeshData = createPlaneMesh()
        mesh.buildMesh(planeMeshData)

        val verticesOf0 = mesh.getFaceVertices(0)
        assertTrue(verticesOf0.contains(0))
        assertTrue(verticesOf0.contains(1))
        assertTrue(verticesOf0.contains(2))
        assertEquals(3, verticesOf0.size)

        val verticesOf1 = mesh.getFaceVertices(1)
        assertTrue(verticesOf1.contains(0))
        assertTrue(verticesOf1.contains(2))
        assertTrue(verticesOf1.contains(3))
        assertEquals(3, verticesOf1.size)
    }

    @Test
    fun testHalfEdgeRetrieval()
    {
        val mesh = HalfEdgeMesh()
        val planeMeshData = createPlaneMesh()
        mesh.buildMesh(planeMeshData)

        val edge01 = mesh.getSpecifiedHalfEdge(0, 1)
        assertNotNull(edge01)

        val edge10 = mesh.getSpecifiedHalfEdge(1, 0)
        assertNotNull(edge10)

        val nonExistentEdge = mesh.getSpecifiedHalfEdge(0, 10)
        assertNull(nonExistentEdge)
    }

    @Test
    fun testUniqueHalfEdges()
    {
        val mesh = HalfEdgeMesh()
        val planeMeshData = createPlaneMesh()
        mesh.buildMesh(planeMeshData)

        val uniqueEdges = mesh.getUniqueHalfEdges()
        assertEquals(5, uniqueEdges.size)

        val tetrahedronMesh = HalfEdgeMesh()
        val tetrahedronMeshData = createTetrahedronMesh()
        tetrahedronMesh.buildMesh(tetrahedronMeshData)

        val uniqueTetrahedronEdges = tetrahedronMesh.getUniqueHalfEdges()
        assertEquals(6, uniqueTetrahedronEdges.size)
    }

    @Test
    fun testEdgeSplitting()
    {
        val mesh = HalfEdgeMesh()
        val planeMeshData = createPlaneMesh()
        mesh.buildMesh(planeMeshData)

        val originalVertexCount = mesh.getVertexCount()
        val originalFaceCount = mesh.getFaceCount()
        val originalEdgeCount = mesh.getEdgeCount()

        val midpoint = PVector(0.5f, 0f, 0f)
        val success = mesh.splitEdge(0, 1, midpoint)
        assertTrue(success)

        assertEquals(originalVertexCount + 1, mesh.getVertexCount())
        assertEquals(originalFaceCount + 1, mesh.getFaceCount())
        assertEquals(originalEdgeCount + 3, mesh.getEdgeCount())

        val newVertexPos = mesh.getVertexPosition(originalVertexCount)
        assertNotNull(newVertexPos)
        assertEquals(0.5f, newVertexPos.x)
        assertEquals(0f, newVertexPos.y)
        assertEquals(0f, newVertexPos.z)
    }

    @Test
    fun testMeshValidation()
    {
        val mesh = HalfEdgeMesh()
        val planeMeshData = createPlaneMesh()
        mesh.buildMesh(planeMeshData)

        val validationResult = mesh.validate()
        assertTrue(validationResult.isEmpty())

        val tetrahedronMesh = HalfEdgeMesh()
        val tetrahedronMeshData = createTetrahedronMesh()
        tetrahedronMesh.buildMesh(tetrahedronMeshData)

        val tetrahedronValidationResult = tetrahedronMesh.validate()
        assertTrue(tetrahedronValidationResult.isEmpty())
    }

    @Test
    fun testIteratorMethods()
    {
        val mesh = HalfEdgeMesh()
        val planeMeshData = createPlaneMesh()
        mesh.buildMesh(planeMeshData)

        var vertexCount = 0
        mesh.forEachVertex { _, position ->
            vertexCount++
            assertNotNull(position)
        }
        assertEquals(4, vertexCount)

        var faceCount = 0
        mesh.forEachFace { _, vertices ->
            faceCount++
            assertEquals(3, vertices.size)
        }
        assertEquals(2, faceCount)

        var edgeCount = 0
        mesh.forEachEdge { source, target ->
            edgeCount++
            assertTrue(source >= 0 && source < mesh.getVertexCount())
            assertTrue(target >= 0 && target < mesh.getVertexCount())
        }
        assertEquals(5, edgeCount)
    }

}
