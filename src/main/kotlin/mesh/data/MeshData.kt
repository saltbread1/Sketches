package mesh.data

import processing.core.PVector

interface MeshData
{
    /**
     * A collection of all vertices.
     */
    val vertices: Collection<PVector>

    /**
     * An indices collection of CCW vertices which construct the mesh polygons.
     */
    val faces: Collection<Triple<Int, Int, Int>>
}
