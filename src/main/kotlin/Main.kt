import processing.core.PApplet

fun main(args: Array<String>)
{
    if (args.isEmpty())
    {
        System.err.println("Usage: <class-to-run>")
        return
    }

    val className = args[0]

    try
    {
        val clazz = Class.forName(className).kotlin
        PApplet.main(clazz.java)
    }
    catch (e: ClassNotFoundException)
    {
        System.err.println("Class '$className' is not found")
    }
    catch (e: Exception)
    {
        System.err.println("Error: ${e.message}")
    }
}
