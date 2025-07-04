package mesh

import processing.core.PVector

data class Vertex(var position: PVector, var outgoing: HalfEdge?)
