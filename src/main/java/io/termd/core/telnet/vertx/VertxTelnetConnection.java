package io.termd.core.telnet.vertx;

import io.termd.core.telnet.TelnetConnection;
import io.termd.core.telnet.TelnetHandler;
import org.vertx.java.core.Context;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxTelnetConnection extends TelnetConnection {

  final NetSocket socket;
  final Context context;
  private Buffer pending;

  public VertxTelnetConnection(TelnetHandler handler, Context context, NetSocket socket) {
    super(handler);
    this.context = context;
    this.socket = socket;
  }

  @Override
  public void schedule(final Runnable task) {
    context.runOnContext(event -> task.run());
  }

  // Not properly synchronized, but ok for now
  @Override
  protected void send(byte[] data) {
    if (pending == null) {
      pending = new Buffer();
      pending.appendBytes(data);
      context.runOnContext(event -> {
        Buffer buf = pending;
        pending = null;
        socket.write(buf);
      });
    } else {
      pending.appendBytes(data);
    }
  }
}
