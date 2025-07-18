package mesh.data

import processing.core.PVector
import java.io.File

class ObjMesh(filepath: String) : MeshData
{
    override val vertices: MutableList<PVector> = mutableListOf()
    override val faces: MutableList<Triple<Int, Int, Int>> = mutableListOf()
    private val objFile: File = File(filepath)

    init
    {
        if (!objFile.exists())
        {
            throw IllegalArgumentException("File not found: $filepath")
        }
    }

    fun parse()
    {
        objFile.bufferedReader().use { reader ->
            reader.forEachLine { line ->
                parseLine(line.trim())
            }
        }
    }

    private fun parseLine(line: String)
    {
        if (line.isEmpty() || line.startsWith("#"))
        {
            return
        }

        val parts = line.split("\\s+".toRegex())
        when (parts[0])
        {
            "v" -> parseVertex(parts)
            "f" -> parseFace(parts)
        }
    }

    private fun parseVertex(parts: List<String>)
    {
        if (parts.size >= 4)
        {
            try
            {
                val x = parts[1].toFloat()
                val y = parts[2].toFloat()
                val z = parts[3].toFloat()
                vertices.add(PVector(x, y, z))
            }
            catch (_: NumberFormatException)
            {
                println("Warning: Invalid vertex format in line: ${parts.joinToString(" ")}")
            }
        }
    }

    private fun parseFace(parts: List<String>)
    {
        if (parts.size >= 4)
        {
            try
            {
                val indices = parts.drop(1).take(3).map { part ->
                    val index = part.split("/")[0].toInt()
                    index - 1
                }

                if (indices.size == 3)
                {
                    faces.add(Triple(indices[0], indices[1], indices[2]))
                }
            }
            catch (_: NumberFormatException)
            {
                println("Warning: Invalid face format in line: ${parts.joinToString(" ")}")
            }
            catch (_: IndexOutOfBoundsException)
            {
                println("Warning: Invalid face indices in line: ${parts.joinToString(" ")}")
            }
        }
    }
}
