import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import processing.core.PApplet;


public class TDMain extends JPanel {

  private static Game gameApp;
  private Timer gameLoopTimer;
  public static final int APP_WIDTH = Game.APP_WIDTH;
  public static final int APP_HEIGHT = Game.APP_HEIGHT;

  public TDMain() {
    Dimension containerSize = new Dimension(APP_WIDTH, APP_HEIGHT);
    setPreferredSize(containerSize);
    setMinimumSize(containerSize);
    setMaximumSize(containerSize);

    // Create Processing sketch (but do NOT manually drive setup()/draw())
    // Processing must own its lifecycle.
    gameApp = new Game();
    gameApp.setSize(APP_WIDTH, APP_HEIGHT);

    // Install mouse bridge (Processing reads its own mouseX/mouseY fields)
    MouseAdapter mouseBridge = new MouseAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        gameApp.mouseX = e.getX();
        gameApp.mouseY = e.getY();
        gameApp.mouseMoved();
      }
      @Override
      public void mouseDragged(MouseEvent e) {
        gameApp.mouseX = e.getX();
        gameApp.mouseY = e.getY();
        gameApp.mouseDragged();
      }
      @Override
      public void mousePressed(MouseEvent e) {
        gameApp.mouseX = e.getX();
        gameApp.mouseY = e.getY();
        gameApp.mousePressed = true;
        gameApp.mouseButton = e.getButton();
        gameApp.mousePressed();
      }
      @Override
      public void mouseReleased(MouseEvent e) {
        gameApp.mouseX = e.getX();
        gameApp.mouseY = e.getY();
        gameApp.mousePressed = false;
        gameApp.mouseReleased();
      }
      @Override
      public void mouseClicked(MouseEvent e) {
        gameApp.mouseClicked();
      }
    };
    addMouseListener(mouseBridge);
    addMouseMotionListener(mouseBridge);

    // Keyboard bridge
    setFocusable(true);
    addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        gameApp.key = e.getKeyChar();
        gameApp.keyCode = e.getKeyCode();
        gameApp.keyPressed = true;
        gameApp.keyPressed();
      }
      @Override
      public void keyReleased(KeyEvent e) {
        gameApp.key = e.getKeyChar();
        gameApp.keyCode = e.getKeyCode();
        gameApp.keyPressed = false;
        gameApp.keyReleased();
      }
      @Override
      public void keyTyped(KeyEvent e) {
        gameApp.key = e.getKeyChar();
        gameApp.keyTyped();
      }
    });

    // Start Processing sketch in embedded mode.
    // This allows Processing to handle rendering and events.
    PApplet.runSketch(new String[] {"TDMain"}, gameApp);
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    // When using Processing's own embedded surface, do not try to draw Processing's backbuffer here.
    // Processing renders directly into its own surface.
  }


  public static void main(String[] args) {
    boolean isWebswing = System.getProperty("webswing.clientId") != null;

    System.out.println("Launching Tower Defense...");
    System.setProperty("sun.java2d.noddraw", "true");
    System.setProperty("sun.java2d.d3d", "false");

    SwingUtilities.invokeLater(() -> {
      JFrame frame = new JFrame("Tower Defense - APCSA");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      TDMain panel = new TDMain();
      frame.add(panel);
      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
      frame.setAlwaysOnTop(true);
      frame.toFront();
      panel.requestFocusInWindow();
    });
  }
}