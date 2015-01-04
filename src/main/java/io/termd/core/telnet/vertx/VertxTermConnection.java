package io.termd.core.telnet.vertx;

import io.termd.core.telnet.TelnetTermConnection;
import org.vertx.java.core.Handler;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxTermConnection extends TelnetTermConnection {
  @Override
  public void schedule(final Runnable task) {
    ((VertxTelnetConnection) conn).context.runOnContext(new Handler<Void>() {
      @Override
      public void handle(Void event) {
        task.run();
      }
    });
  }
}
