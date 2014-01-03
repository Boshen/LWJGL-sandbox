import scala.annotation.tailrec

import org.lwjgl.BufferUtils
import org.lwjgl.input.Keyboard
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
                        layout(location = 1) in vec4 color;

                        smooth out vec4 theColor;

                        uniform vec3 offset;
                        uniform mat4 perspectiveMatrix;

                        void main()
                        {
                            vec4 cameraPos = position + vec4(offset.x, offset.y, offset.z, 0.0);

                            gl_Position = perspectiveMatrix * cameraPos;
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

  val right_extent = 0.8f
  val left_extent = -right_extent
  val top_extent = 0.20f
  val middle_extent = 0.0f
  val bottom_extent = -top_extent
  val front_extent = -1.25f
  val rear_extent = -1.75f

  val numberOfVertices = 36

  val vertexData = Array[Float](
    // Object 1 positions
    left_extent, top_extent, rear_extent,
    left_extent, middle_extent, front_extent,
    right_extent, middle_extent, front_extent,
    right_extent, top_extent, rear_extent,

    left_extent, bottom_extent, rear_extent,
    left_extent, middle_extent, front_extent,
    right_extent, middle_extent, front_extent,
    right_extent, bottom_extent, rear_extent,

    left_extent, top_extent, rear_extent,
    left_extent, middle_extent, front_extent,
    left_extent, bottom_extent, rear_extent,

    right_extent, top_extent, rear_extent,
    right_extent, middle_extent, front_extent,
    right_extent, bottom_extent, rear_extent,

    left_extent, bottom_extent, rear_extent,
    left_extent, top_extent, rear_extent,
    right_extent, top_extent, rear_extent,
    right_extent, bottom_extent, rear_extent,

    // Object 2 positions
    top_extent, right_extent, rear_extent,
    middle_extent, right_extent, front_extent,
    middle_extent, left_extent, front_extent,
    top_extent, left_extent, rear_extent,

    bottom_extent, right_extent, rear_extent,
    middle_extent, right_extent, front_extent,
    middle_extent, left_extent, front_extent,
    bottom_extent, left_extent, rear_extent,

    top_extent, right_extent, rear_extent,
    middle_extent, right_extent, front_extent,
    bottom_extent, right_extent, rear_extent,

    top_extent, left_extent, rear_extent,
    middle_extent, left_extent, front_extent,
    bottom_extent, left_extent, rear_extent,

    bottom_extent, right_extent, rear_extent,
    top_extent, right_extent, rear_extent,
    top_extent, left_extent, rear_extent,
    bottom_extent, left_extent, rear_extent,

    // Object 1 colors
    0.75f, 0.75f, 1.0f, 1.0f, // GREEN
    0.75f, 0.75f, 1.0f, 1.0f,
    0.75f, 0.75f, 1.0f, 1.0f,
    0.75f, 0.75f, 1.0f, 1.0f,

    0.0f, 0.5f, 0.0f, 1.0f, // BLUE
    0.0f, 0.5f, 0.0f, 1.0f,
    0.0f, 0.5f, 0.0f, 1.0f,
    0.0f, 0.5f, 0.0f, 1.0f,

    1.0f, 0.0f, 0.0f, 1.0f, // RED
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,

    0.8f, 0.8f, 0.8f, 1.0f, // GREY
    0.8f, 0.8f, 0.8f, 1.0f,
    0.8f, 0.8f, 0.8f, 1.0f,

    0.5f, 0.5f, 0.0f, 1.0f, // BROWN
    0.5f, 0.5f, 0.0f, 1.0f,
    0.5f, 0.5f, 0.0f, 1.0f,
    0.5f, 0.5f, 0.0f, 1.0f,

    // Object 2 colors
    1.0f, 0.0f, 0.0f, 1.0f, // RED
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,
    1.0f, 0.0f, 0.0f, 1.0f,

    0.5f, 0.5f, 0.0f, 1.0f, // BROWN
    0.5f, 0.5f, 0.0f, 1.0f,
    0.5f, 0.5f, 0.0f, 1.0f,
    0.5f, 0.5f, 0.0f, 1.0f,

    0.0f, 0.5f, 0.0f, 1.0f, // BLUE
    0.0f, 0.5f, 0.0f, 1.0f,
    0.0f, 0.5f, 0.0f, 1.0f,

    0.75f, 0.75f, 1.0f, 1.0f, // GREEN
    0.75f, 0.75f, 1.0f, 1.0f,
    0.75f, 0.75f, 1.0f, 1.0f,

    0.8f, 0.8f, 0.8f, 1.0f, // GREY
    0.8f, 0.8f, 0.8f, 1.0f,
    0.8f, 0.8f, 0.8f, 1.0f,
    0.8f, 0.8f, 0.8f, 1.0f)

  val indexData = Array[Short](
    0, 2, 1,
    3, 2, 0,

    4, 5, 6,
    6, 7, 4,

    8, 9, 10,
    11, 13, 12,

    14, 16, 15,
    17, 16, 14)

  lazy val theProgram = initProgram()
  lazy val perspectiveMatrix = initMatrix()
  lazy val vertexBufferObject = initVertexBuffer()
  lazy val indexBufferObject = initIndexBuffer()
  lazy val vao = GL30.glGenVertexArrays()
  lazy val startTime = System.nanoTime()
  val frustumScale = 1.0f

  def start() {
    initWindow(800, 600)
    initGL()
    loop(getTime(), 0)
    if (!Display.isCreated)
      Display.destroy()
  }

  var bDepthClampingActive = false
  @tailrec final def loop(lastFPS: Long, fps: Int) {
    val (newLastFPS, newFPS) = updateFPS(lastFPS, fps)
    val continueLoop = update()
    display()
    Display.update()
    Display.sync(100)
    if (Display.wasResized)
      reshape(Display.getWidth, Display.getHeight)
    if (continueLoop && !Display.isCloseRequested)
      loop(newLastFPS, newFPS)
  }

  var offset1 = 1.0f
  var offset2 = -1.0f
  def update(): Boolean = {
    var continueLoop = true
    while (Keyboard.next()) {
      if (Keyboard.getEventKeyState) {
        val eventKey = Keyboard.getEventKey
        eventKey match {
          case Keyboard.KEY_SPACE ⇒
            if (bDepthClampingActive)
              GL11.glDisable(GL32.GL_DEPTH_CLAMP)
            else
              GL11.glEnable(GL32.GL_DEPTH_CLAMP)
            bDepthClampingActive = !bDepthClampingActive
          case Keyboard.KEY_ESCAPE ⇒
            continueLoop = false
          case Keyboard.KEY_Z ⇒
            offset1 -= 0.1f
          case Keyboard.KEY_X ⇒
            offset1 += 0.1f
          case Keyboard.KEY_C ⇒
            offset2 -= 0.1f
          case Keyboard.KEY_V ⇒
            offset2 += 0.1f
          case other ⇒
        }
      }
    }
    continueLoop
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
    GL30.glBindVertexArray(vao)

    val float_size = 4
    val colorDataOffset = float_size * 3 * numberOfVertices
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBufferObject)
    GL20.glEnableVertexAttribArray(0)
    GL20.glEnableVertexAttribArray(1)
    GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0)
    GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, 0, colorDataOffset)
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBufferObject)

    GL30.glBindVertexArray(0)

    GL11.glEnable(GL11.GL_CULL_FACE)
    GL11.glCullFace(GL11.GL_BACK)
    GL11.glFrontFace(GL11.GL_CW)

    GL11.glEnable(GL11.GL_DEPTH_TEST)
    GL11.glDepthMask(true)
    GL11.glDepthFunc(GL11.GL_LESS)
    GL11.glDepthRange(0.0f, 1.0f)
  }

  def initProgram(): Int = {
    val shaderList = Array[Int](
      createShader(GL20.GL_VERTEX_SHADER, strVertexShader),
      createShader(GL20.GL_FRAGMENT_SHADER, strFragmentShader)
    )

    val theProgram = createProgram(shaderList)
    val perspectiveMatrixUnif = GL20.glGetUniformLocation(theProgram, "perspectiveMatrix")

    val theMatrixBuffer = BufferUtils.createFloatBuffer(perspectiveMatrix.length)
    theMatrixBuffer.put(perspectiveMatrix)
    theMatrixBuffer.flip()

    GL20.glUseProgram(theProgram)
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

  def initMatrix(): Array[Float] = {
    val zNear = 0.5f
    val zFar = 3.0f
    val matrix = Array.ofDim[Float](16)
    matrix(0) = frustumScale
    matrix(5) = frustumScale
    matrix(10) = (zFar + zNear) / (zNear - zFar)
    matrix(14) = (2 * zFar * zNear) / (zNear - zFar)
    matrix(11) = -1.0f
    matrix
  }

  def initVertexBuffer(): Int = {
    val vertexPositionsBuffer = BufferUtils.createFloatBuffer(vertexData.length)
    vertexPositionsBuffer.put(vertexData)
    vertexPositionsBuffer.flip()

    val vertexBufferObject = GL15.glGenBuffers()
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBufferObject)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexPositionsBuffer, GL15.GL_STATIC_DRAW)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
    vertexBufferObject
  }

  def initIndexBuffer(): Int = {
    val indexDataBuffer = BufferUtils.createShortBuffer(indexData.length);
    indexDataBuffer.put(indexData);
    indexDataBuffer.flip();

    val indexBufferObject = GL15.glGenBuffers();
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBufferObject);
    GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexDataBuffer, GL15.GL_STATIC_DRAW);
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    indexBufferObject
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
    GL11.glClearDepth(1.0f)
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT)

    GL20.glUseProgram(theProgram)

    val offsetUniform = GL20.glGetUniformLocation(theProgram, "offset");
    GL30.glBindVertexArray(vao)
    GL20.glUniform3f(offsetUniform, 0.0f, 0.0f, offset1)
    GL11.glDrawElements(GL11.GL_TRIANGLES, indexData.length, GL11.GL_UNSIGNED_SHORT, 0)

    GL20.glUniform3f(offsetUniform, 0.0f, 0.0f, offset2)
    GL32.glDrawElementsBaseVertex(GL11.GL_TRIANGLES, indexData.length, GL11.GL_UNSIGNED_SHORT, 0, numberOfVertices / 2)

    GL30.glBindVertexArray(0)
    GL20.glUseProgram(0)
  }

  def reshape(width: Int, height: Int) {
    perspectiveMatrix(0) = frustumScale / (width / height.toFloat)
    perspectiveMatrix(5) = frustumScale

    val perspectiveMatrixBuffer = BufferUtils.createFloatBuffer(perspectiveMatrix.length)
    perspectiveMatrixBuffer.put(perspectiveMatrix)
    perspectiveMatrixBuffer.flip();

    val perspectiveMatrixUnif = GL20.glGetUniformLocation(theProgram, "perspectiveMatrix")
    GL20.glUseProgram(theProgram);
    GL20.glUniformMatrix4(perspectiveMatrixUnif, false, perspectiveMatrixBuffer);
    GL20.glUseProgram(0);

    GL11.glViewport(0, 0, width, height);
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
