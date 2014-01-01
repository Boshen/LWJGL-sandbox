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

                        layout (location = 0) in vec4 position;
                        layout (location = 1) in vec4 color;

                        smooth out vec4 theColor;

                        void main()
                        {
                            gl_Position = position;
                            theColor = color;
                        }
                        """

  val strFragmentShader = """
                          #version 330

                          smooth in vec4 theColor;
                          out vec4 outputColor;
                          void main()
                          {
                              outputColor = theColor;
                          }
                          """

  val vertexData = Array(
    0.0f, 0.5f, 0.0f, 1.0f, // top
    0.5f, -0.366f, 0.0f, 1.0f, // right
    -0.5f, -0.366f, 0.0f, 1.0f, // left
    1.0f, 0.0f, 0.0f, 1.0f, // red
    0.0f, 1.0f, 0.0f, 1.0f, // green
    0.0f, 0.0f, 1.0f, 1.0f // blue
  )

  lazy val positionBufferObject = initVertexBuffer()
  lazy val theProgram = initProgram()

  def start() {
    initWindow(800, 600)
    initGL()
    loop(getTime(), 0)
    Display.destroy()
  }

  def loop(lastFPS: Long, fps: Int) {
    if (!Display.isCloseRequested) {
      display()
      val (newLastFPS, newFPS) = updateFPS(lastFPS, fps)
      Display.update()
      Display.sync(100)
      if (Display.wasResized)
        reshape(Display.getWidth, Display.getHeight)
      loop(newLastFPS, newFPS)
    }
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
  }

  def initProgram(): Int = {
    val shaderList = Array[Int](
      createShader(GL20.GL_VERTEX_SHADER, strVertexShader),
      createShader(GL20.GL_FRAGMENT_SHADER, strFragmentShader)
    )

    val theProgram = createProgram(shaderList)
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

    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBufferObject)

    GL20.glEnableVertexAttribArray(0)
    GL20.glEnableVertexAttribArray(1)
    GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, 0, 0)
    GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, 0, 48)

    GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3)

    GL20.glDisableVertexAttribArray(0)
    GL20.glDisableVertexAttribArray(1)
    GL20.glUseProgram(0)
  }

  def reshape(width: Int, height: Int) {
    GL11.glViewport(0, 0, width, height)
  }

  def getTime(): Long =
    Sys.getTime() * 1000 / Sys.getTimerResolution()

  def updateFPS(lastFPS: Long, fps: Int): (Long, Int) = {
    val isNext = (getTime() - lastFPS) > 1000.0
    val newFps = if (isNext) 0 else fps + 1
    val newLastFPS = if (isNext) lastFPS + 1000 else lastFPS
    if (isNext) Display.setTitle("FPS: " + fps)
    (newLastFPS, newFps)
  }
}
