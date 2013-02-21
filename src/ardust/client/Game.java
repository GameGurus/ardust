package ardust.client;

import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
//import org.lwjgl.util.glu.GLU;

import java.awt.*;

public class Game {
    private static final int PIXEL_SCALE = 4;


    private Canvas display_parent;
    private Thread gameThread;
    private boolean running;
    private int width;
    private int height;
    private Thread sleeperThread;
    private boolean requestResetViewPort;
    private Input input;
    private Painter painter;
    private GameMenu menu;
    private GameState gameState;

    public enum GameState{
        MENU_STATE,
        CLIENT_STATE,
        SERVER_STATE;
    }

    public GameState getGameState() {return gameState;}
    public void setGameState(GameState newState) {gameState = newState;}

    public GameMenu getMenu() {return menu;}

    public void startLWJGL(final Canvas display_parent) {
        this.display_parent = display_parent;

        gameThread = new Thread() {
            public void run() {
                running = true;
                try {
                    Display.setParent(display_parent);
                    Display.setTitle("ardust");
                    Display.create();
                    input = new Input();
                    painter = new Painter();

                    painter.setScale(PIXEL_SCALE);
                    painter.init();
                    resetViewPort();
                } catch (Throwable e) {
                    e.printStackTrace();
                    return;
                }
                gameLoop();
            }
        };
        gameThread.start();

        sleeperThread = new Thread("High precision sleep workaround") {
            public void run() {
                try {
                    Thread.sleep(Integer.MAX_VALUE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        sleeperThread.setDaemon(true);
        sleeperThread.start();
        start();
    }

    public void stopLWJGL() {
        stop();
        running = false;
        try {
            if (gameThread != null)
                gameThread.join();
            if (sleeperThread != null) {
                sleeperThread.interrupt();
                sleeperThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void init() {

    }

    private void start() {

    }

    private void stop() {

    }

    public void resized() {
        requestResetViewPort = true;
    }

    void resetViewPort() {
        width = display_parent.getWidth();
        height = display_parent.getHeight();
        input.setHeight(height);
        painter.setScreenDimensions(width, height);
        GL11.glViewport(0, 0, width, height);
        setupRenderMode();
    }

    private void setupRenderMode() {
        Display.setVSyncEnabled(true);
        GL11.glClearColor(0f, 0f, 0f, 0f);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        //GL11.glAlphaFunc(GL11.GL_GEQUAL, 0.8f); // cutoff ?

        // for cursor
        GL11.glLogicOp(GL11.GL_XOR);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);

        GL11.glShadeModel(GL11.GL_SMOOTH);


        GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        // flat zero based surface, 0x0 at the top left
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, height, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
    }

    private void gameLoop() {
        try {
            long deadline = Sys.getTime();
            long timerResolution = Sys.getTimerResolution();
            while (running) {

                int error = GL11.glGetError();
                if (error != GL11.GL_NO_ERROR) {
                    //String glerrmsg = GLU.gluErrorString(error);
                    System.err.println("OpenGL Error: (" + error + ") ");
                }

                if (requestResetViewPort) {
                    requestResetViewPort = false;
                    resetViewPort();
                }

                input.tick();
                //update

                //soundmanager.tick();

                //clear
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT + GL11.GL_COLOR_BUFFER_BIT); // assuming we need one

                //render

                painter.start();

                painter.draw(10, 10, 10, 10, 100, 100);

                painter.flush();

                GL11.glBegin(GL11.GL_TRIANGLES);

                if (input.isMouseButtonDown(0, false))
                    GL11.glColor4f(1f, 1f, 1f, 0.5f);
                else
                    GL11.glColor4f(1f, 0f, 0f, 1f);
                GL11.glVertex2d(0, 0);
                GL11.glTexCoord2f(0,0);
                if (input.isKeyDown(Keyboard.KEY_SPACE, false))
                    GL11.glColor4f(0f, 0f, 0f, 1f);
                else
                    GL11.glColor4f(0f, 1f, 0f, 1f);
                GL11.glVertex2d(input.getX(), 0);
                GL11.glTexCoord2f(1, 0);
                GL11.glColor4f(0f, 0f, 1f, 1f);
                GL11.glVertex2d(0, input.getY());
                GL11.glTexCoord2f(0, 1);

                GL11.glEnd();



                //flip

                Thread.yield();
                Display.update(false);
                if (Display.isCloseRequested()) {
                    running = false;
                }


                // sync
                long nextDeadline = Sys.getTime() + (timerResolution * 16) / 1000;
                while (true) {
                    long delta = deadline - Sys.getTime();
                    if (delta < 5)
                        break;
                    Thread.sleep(1);
                }
                while (true) {
                    long delta = deadline - Sys.getTime();
                    if (delta < 2)
                        break;
                    Thread.sleep(0);
                }
                deadline = nextDeadline;

                Display.processMessages();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processKeyboardAndMouse() {

    }
}
