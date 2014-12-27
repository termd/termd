package io.modsh.core.telnet;

import io.modsh.core.Function;
import io.modsh.core.Handler;
import io.modsh.core.telnet.vertx.TelnetHandler;
import org.apache.commons.net.telnet.TelnetClient;
import org.junit.After;
import org.junit.Before;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.net.NetServer;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetTestBase extends TestBase {

  private Vertx vertx;
  private NetServer server;
  protected TelnetClient client;

  @Before
  public void before() throws InterruptedException {
    vertx = VertxFactory.newVertx();
  }

  protected final void server(Function<Handler<byte[]>, TelnetConnection> factory) {
    server = vertx.createNetServer().connectHandler(new TelnetHandler(factory));
    final BlockingQueue<AsyncResult<NetServer>> latch = new ArrayBlockingQueue<>(1);
    server.listen(4000, "localhost", new org.vertx.java.core.Handler<AsyncResult<NetServer>>() {
      @Override
      public void handle(AsyncResult<NetServer> event) {
        latch.add(event);
      }
    });
    AsyncResult<NetServer> result;
    try {
      result = latch.poll(2, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw failure(e);
    }
    if (result.failed()) {
      throw failure(result.cause());
    }
  }

  @After
  public void after() {
    if (server != null) {
      server.close();
    }
    vertx.stop();
    if (client != null && client.isConnected()) {
      try {
        client.disconnect();
      } catch (IOException ignore) {
      }
    }
  }
}
