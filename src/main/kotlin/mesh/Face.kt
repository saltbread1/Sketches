package mesh

/**
 * Represents a face in a half-edge mesh structure.
 *
 * @property halfEdge A reference to one of the directed half-edges defining this face.
 */
data class Face(val halfEdge: HalfEdge)
