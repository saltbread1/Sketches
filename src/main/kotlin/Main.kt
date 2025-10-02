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
        System.err.println("\u001B[31mClass '$sketchClassName' is not found in 'sketches' package\u001B[0m")
    }
    catch (e: Exception)
    {
        System.err.println("\u001B[31mError: ${e.message}\u001B[0m")
    }
}
