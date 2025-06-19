import processing.core.PApplet

class Main() : PApplet()
{
    override fun settings()
    {
        size(512, 512, P3D)
    }

    override fun setup()
    {
        background(0)
        sphereDetail(32)
    }

    override fun draw()
    {
        background(0)
        ambientLight(32f, 32f, 32f)
        lightSpecular(255f, 255f, 255f)
        pointLight(16f, 32f, 230f, mouseX.toFloat(), mouseY.toFloat(), 128f)
        pushMatrix()
        pushStyle()
        translate((width / 2).toFloat(), (height / 2).toFloat(), 0f)
        noStroke()
        fill(250f, 8f, 32f)
        specular(250f, 250f, 250f)
        shininess(5f)
        sphere(64f)
        popStyle()
        popMatrix()
    }
}

fun main()
{
    PApplet.main(Main::class.java)
}
