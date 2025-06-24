import processing.core.PApplet

fun main(args: Array<String>)
{
    if (args.isEmpty())
    {
        System.err.println("Usage: <class-to-run>")
        return
    }

    val sketchClassName = args[0]

    try
    {
        val clazz = Class.forName("sketches.$sketchClassName").kotlin
        PApplet.main(clazz.java)
    }
    catch (_: ClassNotFoundException)
    {
        System.err.println("Class '$sketchClassName' is not found in 'sketches' package")
    }
    catch (e: Exception)
    {
        System.err.println("Error: ${e.message}")
    }
}
