package io.modsh.core.telnet;

import org.vertx.java.core.Handler;
import org.vertx.java.core.net.NetSocket;

import java.util.function.Function;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetHandler implements Handler<NetSocket> {

  final Function<NetSocket, TelnetSession> factory;

  public TelnetHandler(Function<NetSocket, TelnetSession> factory) {
    this.factory = factory;
  }

  public TelnetHandler() {
    this(TelnetSession::new);
  }

  @Override
  public void handle(NetSocket socket) {
    TelnetSession session = factory.apply(socket);
    socket.dataHandler(session);
    socket.closeHandler(v -> session.destroy());
    session.init();
  }
}
