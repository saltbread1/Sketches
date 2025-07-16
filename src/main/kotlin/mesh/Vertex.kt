package mesh

import processing.core.PVector

/**
 * Represents a vertex in a half-edge mesh structure.
 *
 * @property position The position of the vertex in 3D space.
 * @property outgoing A reference to one of the outgoing half-edges originating from this vertex.
 */
data class Vertex(var position: PVector, var outgoing: HalfEdge?)
