import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

    gameApp = new Game() {
      {
        this.width = TDMain.APP_WIDTH;
        this.height = TDMain.APP_HEIGHT;
        this.g = this.makeGraphics(TDMain.APP_WIDTH, TDMain.APP_HEIGHT, PApplet.JAVA2D, null, true);
      }
    };

    gameApp.setup();

    gameLoopTimer = new Timer(33, new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (gameApp != null) {
          gameApp.frameCount++;
          gameApp.draw();
          TDMain.this.repaint();
        }
      }
    });
      
    gameLoopTimer.start();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (gameApp != null && gameApp.g != null && gameApp.g.image != null) {
      Graphics2D g2d = (Graphics2D) g;
      g2d.drawImage((java.awt.Image) gameApp.g.image, 0, 0, null);
    }
  }

  public static void main(String[] args) {
    boolean isWebswing = System.getProperty("webswing.clientId") != null;

    if (isWebswing) {
      System.out.println("Webswing server verified. Launching standalone Swing graphics loop...");
      System.setProperty("sun.java2d.noddraw", "true");
      System.setProperty("sun.java2d.d3d", "false");

      SwingUtilities.invokeLater(() -> {
        JFrame webFrame = new JFrame("Tower Defense - APCSA");
        webFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        webFrame.add(new TDMain());
        webFrame.pack();
        webFrame.setLocationRelativeTo(null);
        webFrame.setVisible(true);
      });
    } else {
      PApplet.main("Game", args);
    }
  }
}