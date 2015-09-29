package examples.screencast;

import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Vector;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

/**
 * Createa a screencast of the current screen to the TTY.
 *
 * This example shows how to push data to the TTY on regular intervals.
 */
public class Screencaster {

  private final Robot robot;
  private final TtyConnection conn;
  private boolean interrupted;

  public Screencaster(Robot robot, TtyConnection conn) {
    this.robot = robot;
    this.conn = conn;
    conn.setEventHandler((event, key) -> interrupted = true);
  }

  public void handle() {
    if (conn.size() != null) {
      broadcast();
    } else {
      conn.setSizeHandler(size -> broadcast());
    }
  }

  private void broadcast() {
    if (interrupted) {
      conn.close();
      return;
    }
    BufferedImage capture = robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
    Vector size = conn.size();
    Image temp = capture.getScaledInstance(size.x(), size.y(), Image.SCALE_SMOOTH);
    BufferedImage scaled = new BufferedImage(size.x(), size.y(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = scaled.createGraphics();
    g2d.drawImage(temp, 0, 0, null);
    g2d.dispose();
    StringBuilder sb = new StringBuilder();
    for (int y = 0; y < size.y(); y++) {
      sb.append("\033[").append(y + 1).append(";1H");
      for (int x = 0; x < size.x(); x++) {
        Color pixel = new Color(scaled.getRGB(x, y));
        int r = pixel.getRed();
        int g = pixel.getGreen();
        int b = pixel.getBlue();
        double grey = (r + g + b) / 3.0;
        if (grey < 51) {
          sb.append('\u2588');
        } else if (grey < 102) {
          sb.append('\u2593');
        } else if (grey < 153) {
          sb.append('\u2592');
        } else if (grey < 204) {
          sb.append('\u2591');
        } else {
          sb.append(' ');
        }
      }
    }
    conn.write(sb.toString());
    conn.schedule(this::broadcast, 100, TimeUnit.MILLISECONDS);
  }
}
