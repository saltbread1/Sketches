package sketches

import processing.core.PGraphics
import processing.core.PVector
import processing.opengl.PGraphicsOpenGL


@Suppress("unused")
class TestShadowMapping : ExtendedPApplet(P3D)
{
    private val fov = PI * 0.4f
    private val near = 0.1f
    private val far = 20.0f
    private val depthShader by lazy { loadShader(
        resourcePath("shaders/depth.frag")!!,
        resourcePath("shaders/depth.vert")!!,
    ) }
    private val shadowShader by lazy { loadShader(
        resourcePath("shaders/shadow.frag")!!,
        resourcePath("shaders/shadow.vert")!!,
    ) }
    private val depthChecker by lazy { loadShader(
        resourcePath("shaders/depth_checker.frag")!!
    ) }
    private val bgColor = NIGHT_BLACK
    private val lightPosition = PVector(-4.0f, -1.0f, -1.0f) // view space
    private val depthPG by lazy { createGraphics(width * 2, height * 2, P3D).apply { noSmooth() } }

    override fun setup()
    {
        textureMode(NORMAL)

        perspective(fov, aspect, near, far)
        camera(
            0.0f, -4.0f, -6.0f,
            0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f)

        val viewMat = (g as PGraphicsOpenGL).cameraInv.get()
        val worldLightPos = viewMat.mult(lightPosition, null)

        depthPG.beginDraw()
        depthPG.textureMode(NORMAL)
        depthPG.blendMode(REPLACE)
        depthPG.perspective(fov, aspect, near, far)
        depthPG.camera(
            worldLightPos.x, worldLightPos.y, worldLightPos.z,
            0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f)
        depthPG.endDraw()

        depthShader.set("near", near)
        depthShader.set("far", far)

        shadowShader.set("lgProjViewMatrix", (depthPG as PGraphicsOpenGL).projmodelview.get().apply { transpose() }) // この時点ではmodel行列は単位行列
        shadowShader.set("invViewMatrix", (g as PGraphicsOpenGL).cameraInv.get().apply { transpose() })
        shadowShader.set("texProjMatrix", texProjMat.get().apply { transpose() })
        shadowShader.set("lightPosition", lightPosition.x, lightPosition.y, lightPosition.z, 1.0f)
        shadowShader.set("depthBuffer", depthPG)
        shadowShader.set("texOffset", 1.0f / depthPG.width, 1.0f / depthPG.height)
        shadowShader.set("shadowColor", 0.2f, 0.2f, 0.2f)
        shadowShader.set("near", near)
        shadowShader.set("far", far)

        depthChecker.set("depthBuffer", depthPG)

        noLoop()
    }

    override fun draw()
    {
        // front面を無視し, 厚みのあるオブジェクトのみ影を作るようにする
        // これはシャドウアクネを防止するが, 影を落とすback面では解消されない
        // そのため, そのような面のみフラグメントシェーダで深度比較時にバイアスを追加し, 2重で防止する
        depthPG.beginDraw()
        enableCullFaceFront(depthPG) {
            depthPG.background(0x003F8000)
            depthPG.shader(depthShader)
            render(depthPG)
            depthPG.resetShader()
        }
        depthPG.endDraw()

        enableCullFaceBack(g) {
            background(bgColor)
            shader(shadowShader)
            render(g)
            resetShader()
        }

//        filter(depthChecker)
    }

    private fun render(pg: PGraphics)
    {
        val cube = createCubeFlat().apply {
            setStrokeWeight(0.0f)
            setFill(color(255))
        }
        val plane = createRectFlat().apply {
            setStrokeWeight(0.0f)
            setFill(color(249, 222, 204))
        }

        pg.pushMatrix()
        pg.translate(2.0f, 0.0f, 0.0f)
        cube.setFill(color(141, 232, 155))
        cube.draw(pg)
        pg.popMatrix()

        pg.pushMatrix()
        pg.translate(-2.0f, -1.0f, 0.0f)
        pg.scale(1.0f, 2.0f, 1.0f)
        cube.setFill(color(131, 132, 252))
        cube.draw(pg)
        pg.popMatrix()

        pg.pushMatrix()
        pg.translate(0.0f, 1.0f, far * 0.5f)
        pg.rotateX(-HALF_PI)
        pg.scale(far, far, 1.0f)
        plane.draw(pg)
        pg.popMatrix()
    }
}
