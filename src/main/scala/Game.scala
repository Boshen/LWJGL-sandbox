import org.lwjgl.LWJGLException
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.DisplayMode

object Game extends App {
  val display = new Game()
  display.start()
}

class Game {
  def start() {
    try {
      val displayMode = new DisplayMode(800, 600)
      Display.setTitle("LWJGL Test")
      Display.setDisplayMode(displayMode)
      Display.create()
    } catch {
      case e: LWJGLException ⇒
        e.printStackTrace()
        return
    }
  }
}
