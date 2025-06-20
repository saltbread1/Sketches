package s20250620a

import processing.core.PApplet

class S20250620a : PApplet()
{
    override fun settings()
    {
        size(1280, 720, P3D)
    }

    override fun setup()
    {
        frameRate(30.0f)
        ortho(-1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f)
        camera(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
    }

    override fun draw()
    {
        background(0.1f, 0.2f, 0.4f)
    }
}
