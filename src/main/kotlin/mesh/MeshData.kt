package mesh

import processing.core.PVector

interface MeshData
{
    /**
     * Get a collection of all vertices.
     */
    fun getVertices(): Collection<PVector>

    /**
     * Get an indices collection of CCW vertices which construct the mesh polygons.
     */
    fun getFaces(): Collection<Triple<Int, Int, Int>>
}
