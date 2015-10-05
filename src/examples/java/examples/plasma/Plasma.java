package examples.plasma;

import io.termd.core.tty.TtyConnection;
import io.termd.core.tty.TtyEvent;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.lang.Math.cos;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Plasma implements Consumer<TtyConnection> {

  private volatile boolean interrupted = false;

  @Override
  public void accept(TtyConnection conn) {
    conn.setEventHandler((event, key) -> {
      if (event == TtyEvent.INTR) {
        interrupted = true;
      }
    });
    if (conn.size() != null) {
      run(conn);
    } else {
      conn.setSizeHandler(size -> run(conn));
    }
  }

  public void run(TtyConnection conn) {

    int width = conn.size().x();
    int height = conn.size().y();

    long t = System.currentTimeMillis();
    double a = 0.10 * 128 / width;
    double b = 0.0015;
    double c = 0.07;
    double d = 0.08 * 128 / width;
    double e = 0.002;
    double f = 0.01;
    double g = 0.08 * 32 / height;
    double h = 0.003;
    double i = 0.01;
    double j = 0.1 * 32 / height;
    double k = 0.001;
    double l = 0.06;

    double[][] abc = new double[width][height];
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        abc[x][y] = 32 * (
            cos(a * x + b * t + c) +
                cos(d * x + e * t + f) +
                cos(g * y + h * t + i) +
                cos(j * y + k * t + l) + 4);
      }
    }

    // Refresh the screen using Ansi magic + the unicode block code points
    StringBuilder sb = new StringBuilder();
    for (int y = 0; y < height; y++) {
      sb.append("\033[").append(y + 1).append(";1H");
      for (int x = 0; x < width; x++) {
        double val = abc[x][y];
        if (val < 51) {
          sb.append('\u2588');
        } else if (val < 102) {
          sb.append('\u2593');
        } else if (val < 153) {
          sb.append('\u2592');
        } else if (val < 204) {
          sb.append('\u2591');
        } else {
          sb.append(' ');
        }
      }
    }

    conn.write(sb.toString());

    //
    if (!interrupted) {
      conn.schedule(() -> {
        run(conn);
      }, 50, TimeUnit.MILLISECONDS);
    } else {
      conn.close();
    }
  }
}
