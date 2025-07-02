package mesh

/**
 * Represents a directed edge in a half-edge mesh structure.
 *
 * @property vertex   Index of the vertex at the end of this half-edge.
 * @property face     Index of the face associated with this half-edge.
 * @property next     Reference to the next half-edge in the loop of the face.
 * @property prev     Reference to the previous half-edge in the loop of the face.
 * @property opposite Reference to the opposite half-edge sharing the same edge.
 */
data class HalfEdge(val vertex: Int, val face: Int, var next: HalfEdge?, var prev: HalfEdge?, var opposite: HalfEdge?)
