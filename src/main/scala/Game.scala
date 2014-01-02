import scala.annotation.tailrec
import org.lwjgl.BufferUtils
import org.lwjgl.LWJGLException
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.DisplayMode
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL32
import org.lwjgl.opengl.GLContext
import org.lwjgl.Sys

object Game extends App {
  new Game().start()
}

class Game {

  val strVertexShader = """
                        #version 330

                        layout(location = 0) in vec4 position;

                        uniform float loopDuration;
                        uniform float time;

                        uniform mat4 perspectiveMatrix;

                        void main()
                        {
                            float timeScale = 3.14159f * 2.0f / loopDuration;

                            float currTime = mod(time, loopDuration);
                            vec4 totalOffset = vec4(
                                    cos(currTime * timeScale) * 0.5f,
                                    sin(currTime * timeScale) * 0.5f,
                                    0.0f,
                                    0.0f);
                            vec4 cameraPos = position + totalOffset;

                            gl_Position = perspectiveMatrix * cameraPos;
                        }
                        """

  val strFragmentShader = """
                          #version 330

                          out vec4 outputColor;
                          uniform float fragLoopDuration;
                          uniform float time;
                          const vec4 firstColor = vec4(1.0f, 1.0f, 1.0f, 1.0f);
                          const vec4 secondColor = vec4(0.0f, 1.0f, 0.0f, 1.0f);

                          void main()
                          {
                              float currTime = mod(time, fragLoopDuration);
                              float currLerp = currTime / fragLoopDuration;

                              outputColor = mix(firstColor, secondColor, currLerp);
                          }
                          """

  val vertexData = Array(
    0.25f, 0.25f, -1.25f, 1.0f,
    0.25f, -0.25f, -1.25f, 1.0f,
    -0.25f, 0.25f, -1.25f, 1.0f,

    0.25f, -0.25f, -1.25f, 1.0f,
    -0.25f, -0.25f, -1.25f, 1.0f,
    -0.25f, 0.25f, -1.25f, 1.0f,

    0.25f, 0.25f, -2.75f, 1.0f,
    -0.25f, 0.25f, -2.75f, 1.0f,
    0.25f, -0.25f, -2.75f, 1.0f,

    0.25f, -0.25f, -2.75f, 1.0f,
    -0.25f, 0.25f, -2.75f, 1.0f,
    -0.25f, -0.25f, -2.75f, 1.0f,

    -0.25f, 0.25f, -1.25f, 1.0f,
    -0.25f, -0.25f, -1.25f, 1.0f,
    -0.25f, -0.25f, -2.75f, 1.0f,

    -0.25f, 0.25f, -1.25f, 1.0f,
    -0.25f, -0.25f, -2.75f, 1.0f,
    -0.25f, 0.25f, -2.75f, 1.0f,

    0.25f, 0.25f, -1.25f, 1.0f,
    0.25f, -0.25f, -2.75f, 1.0f,
    0.25f, -0.25f, -1.25f, 1.0f,

    0.25f, 0.25f, -1.25f, 1.0f,
    0.25f, 0.25f, -2.75f, 1.0f,
    0.25f, -0.25f, -2.75f, 1.0f,

    0.25f, 0.25f, -2.75f, 1.0f,
    0.25f, 0.25f, -1.25f, 1.0f,
    -0.25f, 0.25f, -1.25f, 1.0f,

    0.25f, 0.25f, -2.75f, 1.0f,
    -0.25f, 0.25f, -1.25f, 1.0f,
    -0.25f, 0.25f, -2.75f, 1.0f,

    0.25f, -0.25f, -2.75f, 1.0f,
    -0.25f, -0.25f, -1.25f, 1.0f,
    0.25f, -0.25f, -1.25f, 1.0f,

    0.25f, -0.25f, -2.75f, 1.0f,
    -0.25f, -0.25f, -2.75f, 1.0f,
    -0.25f, -0.25f, -1.25f, 1.0f
    )

  lazy val positionBufferObject = initVertexBuffer()
  lazy val theProgram = initProgram()
  lazy val startTime = System.nanoTime()

  def start() {
    initWindow(800, 600)
    initGL()
    loop(getTime(), 0)
    Display.destroy()
  }

  @tailrec final def loop(lastFPS: Long, fps: Int) {
      val (newLastFPS, newFPS) = updateFPS(lastFPS, fps)
      display()
      Display.update()
      Display.sync(100)
      if (Display.wasResized)
        reshape(Display.getWidth, Display.getHeight)
      if (!Display.isCloseRequested)
        loop(newLastFPS, newFPS)
  }

  def initWindow(width: Int, height: Int) {
    try {
      Display.setTitle("Triangle!")
      Display.setDisplayMode(new DisplayMode(width, height))
      Display.setResizable(true)
      Display.create()

      if (!GLContext.getCapabilities().OpenGL33) {
        System.err.println("You must have at least OpenGL 3.3 to run this program.")
      }
    } catch {
      case e: LWJGLException ⇒
        e.printStackTrace()
        System.exit(-1)
    }
  }

  def initGL() {
    val vao = GL30.glGenVertexArrays()
    GL30.glBindVertexArray(vao)
    GL11.glEnable(GL11.GL_CULL_FACE)
    GL11.glCullFace(GL11.GL_BACK)
    GL11.glFrontFace(GL11.GL_CW)
  }

  def initProgram(): Int = {
    val shaderList = Array[Int](
      createShader(GL20.GL_VERTEX_SHADER, strVertexShader),
      createShader(GL20.GL_FRAGMENT_SHADER, strFragmentShader)
    )

    val theProgram = createProgram(shaderList)
    val loopDurationUnf = GL20.glGetUniformLocation(theProgram, "loopDuration")
    val fragLoopDurUnf = GL20.glGetUniformLocation(theProgram, "fragLoopDuration")
    val perspectiveMatrixUnif = GL20.glGetUniformLocation(theProgram, "perspectiveMatrix")

    val frustumScale = 1.0f
    val zNear = 0.5f
    val zFar = 3.0f

    val theMatrix = Array.ofDim[Float](16)
    theMatrix(0) = frustumScale
    theMatrix(5) = frustumScale
    theMatrix(10) = (zFar + zNear) / (zNear - zFar)
    theMatrix(14) = (2 * zFar * zNear) / (zNear - zFar)
    theMatrix(11) = -1.0f

    val theMatrixBuffer = BufferUtils.createFloatBuffer(theMatrix.length)
    theMatrixBuffer.put(theMatrix)
    theMatrixBuffer.flip()

    GL20.glUseProgram(theProgram)
    GL20.glUniform1f(loopDurationUnf, 5.0f)
    GL20.glUniform1f(fragLoopDurUnf, 10.0f)
    GL20.glUniformMatrix4(perspectiveMatrixUnif, false, theMatrixBuffer)
    GL20.glUseProgram(0)
    shaderList.foreach(GL20.glDeleteShader(_))
    theProgram
  }

  def createProgram(shaderList: Array[Int]): Int = {
    val program = GL20.glCreateProgram()
    shaderList.foreach(GL20.glAttachShader(program, _))
    GL20.glLinkProgram(program)
    val status = GL20.glGetProgrami(program, GL20.GL_LINK_STATUS)
    if (status == GL11.GL_FALSE) {
      val infoLogLength = GL20.glGetProgrami(program, GL20.GL_INFO_LOG_LENGTH)
      val strInfoLog = GL20.glGetProgramInfoLog(program, infoLogLength)
      println("Linker failure: %s".format(strInfoLog))
    }
    shaderList.foreach(GL20.glDetachShader(program, _))
    program
  }

  def initVertexBuffer(): Int = {
    val vertexPositionsBuffer = BufferUtils.createFloatBuffer(vertexData.length)
    vertexPositionsBuffer.put(vertexData)
    vertexPositionsBuffer.flip()

    val positionBufferObject = GL15.glGenBuffers()
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBufferObject)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexPositionsBuffer, GL15.GL_STATIC_DRAW)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
    positionBufferObject
  }

  def createShader(shaderType: Int, shaderFile: String): Int = {
    val shader = GL20.glCreateShader(shaderType)
    GL20.glShaderSource(shader, shaderFile)
    GL20.glCompileShader(shader)
    val status = GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS)
    if (status == GL11.GL_FALSE) {
      val infoLogLength = GL20.glGetShaderi(shader, GL20.GL_INFO_LOG_LENGTH)
      val infoLog = GL20.glGetShaderInfoLog(shader, infoLogLength)
      val strShaderType = shaderType match {
        case GL20.GL_VERTEX_SHADER   ⇒ "vertex"
        case GL32.GL_GEOMETRY_SHADER ⇒ "geometry"
        case GL20.GL_FRAGMENT_SHADER ⇒ "fragment"
      }
      println("Compile failure in %s shader:\n%s".format(strShaderType, infoLog))
    }
    shader
  }

  def display() {
    GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)

    GL20.glUseProgram(theProgram)

    val elapsedTimeUnf = GL20.glGetUniformLocation(theProgram, "time")

    GL20.glUniform1f(elapsedTimeUnf, getElapsedTime() / 1000.0f)

    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBufferObject)
    GL20.glEnableVertexAttribArray(0)
    GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, 0, 0)
    GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 36)

    GL20.glDisableVertexAttribArray(0)
    GL20.glUseProgram(0)
  }

  def reshape(width: Int, height: Int) {
    GL11.glViewport(0, 0, width, height)
  }

  def getTime(): Long =
    Sys.getTime() * 1000 / Sys.getTimerResolution()

  def getElapsedTime(): Float =
    (System.nanoTime() - startTime) / 1000000.0f

  def updateFPS(lastFPS: Long, fps: Int): (Long, Int) = {
    val isNext = (getTime() - lastFPS) > 1000.0
    val newFps = if (isNext) 0 else fps + 1
    val newLastFPS = if (isNext) lastFPS + 1000 else lastFPS
    if (isNext) Display.setTitle("FPS: " + fps)
    (newLastFPS, newFps)
  }
}
