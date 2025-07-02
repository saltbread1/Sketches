package mesh

import processing.core.PVector

data class Vertex(val position: PVector, var outgoing: HalfEdge?)
