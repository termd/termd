package io.termd.core.term;

import io.termd.core.Handler;
import io.termd.core.Provider;
import io.termd.core.readline.ReadlineRequest;
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
public abstract class ReadlineTermTestBase extends TelnetTestBase {

  protected boolean binary;

  protected final void assertConnect() throws Exception {
    client = new TelnetClient();
    client.addOptionHandler(new EchoOptionHandler(false, false, true, true));
    if (binary) {
      client.addOptionHandler(new SimpleOptionHandler(0, false, false, true, true));
    }
    client.connect("localhost", 4000);
  }

  protected final void assertWrite(byte[] data) throws Exception {
    OutputStream out = client.getOutputStream();
    out.write(data);
    out.flush();
  }

  protected final void assertWrite(String s) throws Exception {
    assertWrite(s.getBytes("UTF-8"));
  }

  protected final void assertWriteln(String s) throws Exception {
    assertWrite(s + (binary ? "\r" : "\r\n"));
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
            new ReadlineTerm(this, new Handler<ReadlineRequest>() {
              @Override
              public void handle(ReadlineRequest request) {
                requestCount.incrementAndGet();
                request.write("% ").end();
              }
            });
          }
        };
      }
    });
    assertConnect();
    assertEquals("% ", assertReadString(2));
    assertEquals(1, connectionCount.get());
    assertEquals(1, requestCount.get());
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
            new ReadlineTerm(this, new Handler<ReadlineRequest>() {
              @Override
              public void handle(ReadlineRequest request) {
                switch (request.requestCount()) {
                  case 0:
                    request.write("% ").end();
                    break;
                  default:
                    request.write("hello");
                    request.write("% ").end();
                    latch.countDown();
                }
              }
            });
          }
        };
      }
    });
    assertConnect();
    assertEquals("% ", assertReadString(2));
    assertWriteln("");
    assertTrue(latch.await(10, TimeUnit.SECONDS));
    assertEquals("\r\nhello% ", assertReadString(9));
  }

  @Test
  public void testAsyncEndRequest() throws Exception {
    final ArrayBlockingQueue<ReadlineRequest> requestContextWait = new ArrayBlockingQueue<>(1);
    server(new Provider<TelnetHandler>() {
      @Override
      public TelnetHandler provide() {
        return new VertxTermConnection() {
          @Override
          protected void onOpen(TelnetConnection conn) {
            super.onOpen(conn);
            new ReadlineTerm(this, new Handler<ReadlineRequest>() {
              @Override
              public void handle(ReadlineRequest request) {
                switch (request.requestCount()) {
                  case 0:
                    request.write("% ").end();
                    break;
                  default:
                    requestContextWait.add(request);
                }
              }
            });
          }
        };
      }
    });
    assertConnect();
    assertEquals("% ", assertReadString(2));
    assertWriteln("");
    ReadlineRequest requestContext = assertNotNull(requestContextWait.poll(10, TimeUnit.SECONDS));
    assertEquals("\r\n", assertReadString(2));
    requestContext.write("% ").end();
    assertEquals("% ", assertReadString(2));
  }

  @Test
  public void testBufferedRequest() throws Exception {
    final ArrayBlockingQueue<ReadlineRequest> requestContextWait = new ArrayBlockingQueue<>(10);
    server(new Provider<TelnetHandler>() {
      @Override
      public TelnetHandler provide() {
        return new VertxTermConnection() {
          @Override
          protected void onOpen(TelnetConnection conn) {
            super.onOpen(conn);
            new ReadlineTerm(this, new Handler<ReadlineRequest>() {
              @Override
              public void handle(ReadlineRequest request) {
                switch (request.requestCount()) {
                  case 0:
                    request.write("% ").end();
                    break;
                  default:
                    requestContextWait.add(request);
                }
              }
            });
          }
        };
      }
    });
    assertConnect();
    assertEquals("% ", assertReadString(2));
    assertWriteln("abc");
    ReadlineRequest requestContext = assertNotNull(requestContextWait.poll(10000, TimeUnit.SECONDS));
    assertEquals("abc\r\n", assertReadString(5));
    assertEquals("abc", requestContext.getData());
    assertWriteln("def");
    requestContext.write("% ").end();
    assertEquals("% def", assertReadString(5));
    requestContext = assertNotNull(requestContextWait.poll(10000, TimeUnit.SECONDS));
    assertEquals("def", requestContext.getData());
    requestContext.write("% ").end();
    assertEquals("\r\n% ", assertReadString(4));
  }
}
