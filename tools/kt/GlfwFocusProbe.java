import java.nio.file.Files;
import java.nio.file.Path;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;

/** Минимальный независимый стенд: та же GLFW, без Minecraft и без OpenGL. */
public final class GlfwFocusProbe {
    public static void main(String[] args) throws Exception {
        String name = args[0];
        GLFWErrorCallback.createPrint(System.err).set();
        if (!GLFW.glfwInit()) throw new IllegalStateException("glfwInit failed");
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
        long window = GLFW.glfwCreateWindow(520, 260, "GLFW focus probe " + name, 0, 0);
        if (window == 0) throw new IllegalStateException("glfwCreateWindow failed");
        System.out.println("PID=" + ProcessHandle.current().pid() + " GLFW=" + GLFW.glfwGetVersionString());
        long frames = 0;
        try {
            while (!GLFW.glfwWindowShouldClose(window) && !Files.exists(Path.of("stop-" + name))) {
                GLFW.glfwPollEvents();
                if (++frames % 60 == 0) Files.writeString(Path.of("heartbeat-" + name), "" + frames);
                Thread.sleep(8);
            }
        } finally {
            GLFW.glfwDestroyWindow(window);
            GLFW.glfwTerminate();
        }
    }
}
