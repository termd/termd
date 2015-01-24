package io.termd.core.term;

import io.termd.core.Handler;
import io.termd.core.Provider;
import io.termd.core.readline.RequestContext;
import io.termd.core.telnet.TelnetConnection;
import io.termd.core.telnet.TelnetHandler;
import io.termd.core.telnet.TelnetTestBase;
import io.termd.core.telnet.vertx.VertxTermConnection;
import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.SimpleOptionHandler;
import org.apache.commons.net.telnet.TelnetClient;
import org.junit.Test;

import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReadlineTermTest extends TelnetTestBase {

  protected final void assertConnect() throws Exception {
    client = new TelnetClient();
    client.addOptionHandler(new EchoOptionHandler(false, false, true, true));
    client.addOptionHandler(new SimpleOptionHandler(0, false, false, true, true));
    client.connect("localhost", 4000);
  }

  protected final String assertRead(int length) throws Exception {
    byte[] bytes = new byte[length];
    while (length > 0) {
      int i = client.getInputStream().read(bytes, bytes.length - length, length);
      if (i == -1) {
        throw new AssertionError();
      }
      length -= i;
    }
    return new String(bytes, 0, bytes.length, "UTF-8");
  }

  protected final void assertWrite(String s) throws Exception {
    OutputStream out = client.getOutputStream();
    out.write(s.getBytes("UTF-8"));
    out.flush();
  }

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
    assertConnect();
    assertEquals("% ", assertRead(2));
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
    assertConnect();
    assertEquals("% ", assertRead(2));
    assertWrite("\r");
    assertTrue(latch.await(10, TimeUnit.SECONDS));
    assertEquals("\r\nhello% ", assertRead(9));
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
    assertConnect();
    assertEquals("% ", assertRead(2));
    assertWrite("\r");
    RequestContext requestContext = assertNotNull(requestContextWait.poll(10, TimeUnit.SECONDS));
    assertEquals("\r\n", assertRead(2));
    requestContext.end();
    assertEquals("% ", assertRead(2));
  }

  @Test
  public void testBufferedRequest() throws Exception {
    final ArrayBlockingQueue<RequestContext> requestContextWait = new ArrayBlockingQueue<>(10);
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
    assertConnect();
    assertEquals("% ", assertRead(2));
    assertWrite("abc\r");
    RequestContext requestContext = assertNotNull(requestContextWait.poll(10, TimeUnit.SECONDS));
    assertEquals("abc\r\n", assertRead(5));
    assertEquals("abc", requestContext.getRaw());
    assertWrite("def\r");
    requestContext.end();
    assertEquals("% def", assertRead(5));
    requestContext = assertNotNull(requestContextWait.poll(10, TimeUnit.SECONDS));
    assertEquals("def", requestContext.getRaw());
    requestContext.end();
    assertEquals("\r\n% ", assertRead(4));
  }
}
