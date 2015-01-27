package io.termd.core.telnet.vertx;

import io.termd.core.Provider;
import io.termd.core.telnet.TelnetBootstrap;
import io.termd.core.telnet.TelnetHandler;
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
  public void start(Provider<TelnetHandler> factory) {
    NetServer server = vertx.createNetServer();
    server.connectHandler(new TelnetNetSocketHandler(vertx, factory));
    server.listen(port, host);
  }
}
