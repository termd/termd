package io.termd.core.telnet;

import io.termd.core.telnet.vertx.TelnetSocketHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import org.apache.commons.net.telnet.TelnetClient;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetTestBase extends TestBase {

  private Vertx vertx;
  private NetServer server;
  protected TelnetClient client;

  protected final String assertReadString(int length) throws Exception {
    return new String(assertReadBytes(length), 0, length, "UTF-8");
  }

  protected final byte[] assertReadBytes(int length) throws Exception {
    byte[] bytes = new byte[length];
    while (length > 0) {
      int i = client.getInputStream().read(bytes, bytes.length - length, length);
      if (i == -1) {
        throw new AssertionError();
      }
      length -= i;
    }
    return bytes;
  }


  @Before
  public void before() throws InterruptedException {
    vertx = Vertx.vertx();
  }

  protected final void server(Supplier<TelnetHandler> factory) {
    server = vertx.createNetServer().connectHandler(new TelnetSocketHandler(vertx, factory));
    final BlockingQueue<AsyncResult<NetServer>> latch = new ArrayBlockingQueue<>(1);
    server.listen(4000, "localhost", latch::add);
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
    vertx.close();
    if (client != null && client.isConnected()) {
      try {
        client.disconnect();
      } catch (IOException ignore) {
      }
    }
  }
}
