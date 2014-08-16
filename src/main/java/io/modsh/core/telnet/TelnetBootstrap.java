package io.modsh.core.telnet;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.net.NetServer;

import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetBootstrap {

  public static void main(String[] args) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    new TelnetBootstrap("localhost", 4000).start();
    latch.await();
  }

  private final String host;
  private final int port;
  private final Vertx vertx;
  private NetServer server;

  public TelnetBootstrap(String host, int port) {
    this(VertxFactory.newVertx(), host, port);
  }

  public TelnetBootstrap(Vertx vertx, String host, int port) {
    this.vertx = vertx;
    this.host = host;
    this.port = port;
  }

  public void start() {
    NetServer server = vertx.createNetServer();
    server.connectHandler(new TelnetHandler());
    server.listen(port, host);
  }
}
