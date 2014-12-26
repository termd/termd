package io.modsh.core.telnet.vertx;

import io.modsh.core.Function;
import io.modsh.core.Handler;
import io.modsh.core.telnet.TelnetBootstrap;
import io.modsh.core.telnet.TelnetConnection;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.net.NetServer;

import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxTelnetBootstrap extends TelnetBootstrap {

  public static void main(String[] args) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    new VertxTelnetBootstrap("localhost", 4000).start();
    latch.await();
  }

  private final Vertx vertx;

  public VertxTelnetBootstrap(String host, int port) {
    this(VertxFactory.newVertx(), host, port);
  }

  public VertxTelnetBootstrap(Vertx vertx, String host, int port) {
    super(host, port);
    this.vertx = vertx;
  }

  @Override
  public void start(Function<Handler<byte[]>, TelnetConnection> factory) {
    NetServer server = vertx.createNetServer();
    server.connectHandler(new TelnetHandler(factory));
    server.listen(port, host);
  }
}
