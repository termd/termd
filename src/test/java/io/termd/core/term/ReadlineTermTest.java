package io.termd.core.term;

import io.termd.core.Handler;
import io.termd.core.Provider;
import io.termd.core.readline.RequestContext;
import io.termd.core.telnet.TelnetConnection;
import io.termd.core.telnet.TelnetHandler;
import io.termd.core.telnet.TelnetTestBase;
import io.termd.core.telnet.vertx.VertxTermConnection;
import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.TelnetClient;
import org.junit.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReadlineTermTest extends TelnetTestBase {

  @Test
  public void testGetPrompt() throws Exception {
    final AtomicInteger connectionCount = new AtomicInteger();
    final AtomicInteger requestCount = new AtomicInteger();
    server(new Provider<TelnetHandler>() {
      @Override
      public TelnetHandler provide() {
        connectionCount.incrementAndGet();
        return new VertxTermConnection() {
          @Override
          protected void onOpen(TelnetConnection conn) {
            super.onOpen(conn);
            new ReadlineTerm(this, new Handler<RequestContext>() {
              @Override
              public void handle(RequestContext event) {
                requestCount.incrementAndGet();
                event.end();
              }
            });
          }
        };
      }
    });
    client = new TelnetClient();
    client.addOptionHandler(new EchoOptionHandler(false, false, true, true));
    client.connect("localhost", 4000);
    byte[] bytes = new byte[100];
    assertEquals(2, client.getInputStream().read(bytes));
    assertEquals("% ", new String(bytes, 0, 2));
    assertEquals(1, connectionCount.get());
    assertEquals(0, requestCount.get());
  }

  @Test
  public void testRequestWrite() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    server(new Provider<TelnetHandler>() {
      @Override
      public TelnetHandler provide() {
        return new VertxTermConnection() {
          @Override
          protected void onOpen(TelnetConnection conn) {
            super.onOpen(conn);
            new ReadlineTerm(this, new Handler<RequestContext>() {
              @Override
              public void handle(RequestContext event) {
                event.write("hello");
                event.end();
                latch.countDown();
              }
            });
          }
        };
      }
    });
    client = new TelnetClient();
    client.addOptionHandler(new EchoOptionHandler(false, false, true, true));
    client.connect("localhost", 4000);
    byte[] bytes = new byte[100];
    assertEquals(2, client.getInputStream().read(bytes));
    assertEquals("% ", new String(bytes, 0, 2));
    client.getOutputStream().write('\r');
    client.getOutputStream().flush();
    assertTrue(latch.await(10, TimeUnit.SECONDS));
    assertEquals(9, client.getInputStream().read(bytes));
    assertEquals("\r\nhello% ", new String(bytes, 0, 9));
  }

  @Test
  public void testAsyncEndRequest() throws Exception {
    final ArrayBlockingQueue<RequestContext> requestContextWait = new ArrayBlockingQueue<>(1);
    server(new Provider<TelnetHandler>() {
      @Override
      public TelnetHandler provide() {
        return new VertxTermConnection() {
          @Override
          protected void onOpen(TelnetConnection conn) {
            super.onOpen(conn);
            new ReadlineTerm(this, new Handler<RequestContext>() {
              @Override
              public void handle(RequestContext event) {
                requestContextWait.add(event);
              }
            });
          }
        };
      }
    });
    client = new TelnetClient();
    client.addOptionHandler(new EchoOptionHandler(false, false, true, true));
    client.connect("localhost", 4000);
    byte[] bytes = new byte[100];
    assertEquals(2, client.getInputStream().read(bytes));
    assertEquals("% ", new String(bytes, 0, 2));
    client.getOutputStream().write('\r');
    client.getOutputStream().flush();
    RequestContext requestContext = assertNotNull(requestContextWait.poll(10, TimeUnit.SECONDS));
    assertEquals(2, client.getInputStream().read(bytes));
    assertEquals("\r\n", new String(bytes, 0, 2));
    requestContext.end();
    assertEquals(2, client.getInputStream().read(bytes));
    assertEquals("% ", new String(bytes, 0, 2));
  }
}
