package io.termd.core.telnet.vertx;

import io.termd.core.telnet.TelnetConnection;
import io.termd.core.telnet.TelnetHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxTelnetConnection extends TelnetConnection {

  final NetSocket socket;

  public VertxTelnetConnection(TelnetHandler handler, NetSocket socket) {
    super(handler);
    this.socket = socket;
  }

  @Override
  protected void send(byte[] data) {
    socket.write(new Buffer(data));
  }
}
