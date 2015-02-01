package io.termd.core.tty;

import io.termd.core.util.Handler;
import io.termd.core.util.Helper;
import io.termd.core.util.Provider;
import io.termd.core.telnet.TelnetConnection;
import io.termd.core.telnet.TelnetHandler;
import io.termd.core.telnet.TelnetTtyConnection;
import io.termd.core.telnet.TelnetTestBase;
import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.SimpleOptionHandler;
import org.apache.commons.net.telnet.TelnetClient;
import org.junit.Test;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class ReadlineTermTtyBase extends TelnetTestBase {

  protected boolean binary;

  protected final void assertConnect() throws Exception {
    client = new TelnetClient();
    client.addOptionHandler(new EchoOptionHandler(false, false, true, true));
    if (binary) {
      client.addOptionHandler(new SimpleOptionHandler(0, false, false, true, true));
    }
    client.connect("localhost", 4000);
  }

  protected final void assertWrite(int... codePoints) throws Exception {
    assertWrite(Helper.fromCodePoints(codePoints));
  }

  protected final void assertWrite(byte... data) throws Exception {
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
  public void testWrite() throws Exception {
    final AtomicInteger connectionCount = new AtomicInteger();
    final AtomicInteger requestCount = new AtomicInteger();
    server(new Provider<TelnetHandler>() {
      @Override
      public TelnetHandler provide() {
        connectionCount.incrementAndGet();
        return new TelnetTtyConnection() {
          @Override
          protected void onOpen(TelnetConnection conn) {
            super.onOpen(conn);
            requestCount.incrementAndGet();
            writeHandler().handle(new int[]{'%', ' '});
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
  public void testRead() throws Exception {
    final ArrayBlockingQueue<int[]> queue = new ArrayBlockingQueue<>(1);
    server(new Provider<TelnetHandler>() {
      @Override
      public TelnetHandler provide() {
        return new TelnetTtyConnection() {
          @Override
          protected void onOpen(TelnetConnection conn) {
            super.onOpen(conn);
            setReadHandler(new Handler<int[]>() {
              @Override
              public void handle(int[] data) {
                queue.add(data);
                writeHandler().handle(new int[]{'h', 'e', 'l', 'l', 'o'});
              }
            });
          }
        };
      }
    });
    assertConnect();
    assertWriteln("");
    int[] data = queue.poll(10, TimeUnit.SECONDS);
    assertTrue(Arrays.equals(new int[]{'\r'}, data));
    assertEquals("hello", assertReadString(5));
  }

  @Test
  public void testSignalInterleaving() throws Exception {
    server(new Provider<TelnetHandler>() {
      @Override
      public TelnetHandler provide() {
        return new TelnetTtyConnection() {
          StringBuilder buffer = new StringBuilder();
          int count = 0;
          @Override
          protected void onOpen(TelnetConnection conn) {
            super.onOpen(conn);
            setReadHandler(new Handler<int[]>() {
              @Override
              public void handle(int[] event) {
                Helper.appendTo(event, buffer);
              }
            });
            setSignalHandler(new Handler<Signal>() {
              @Override
              public void handle(Signal event) {
                if (event == Signal.INT) {
                  switch (count) {
                    case 0:
                      assertEquals("hello", buffer.toString());
                      buffer.setLength(0);
                      count = 1;
                      break;
                    case 1:
                      assertEquals("bye", buffer.toString());
                      count = 2;
                      testComplete();
                      break;
                    default:
                      fail("Not expected");
                  }
                }
              }
            });
          }
        };
      }
    });
    assertConnect();
    assertWrite('h','e','l','l','o',3,'b','y','e',3);
    await();
  }

/*
  @Test
  public void testAsyncEndRequest() throws Exception {
    final ArrayBlockingQueue<ReadlineRequest> requestContextWait = new ArrayBlockingQueue<>(1);
    server(new Provider<TelnetHandler>() {
      @Override
      public TelnetHandler provide() {
        return new TelnetTermConnection() {
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
*/
  @Test
  public void testBufferedRead() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    server(new Provider<TelnetHandler>() {
      @Override
      public TelnetHandler provide() {
        return new TelnetTtyConnection() {
          @Override
          protected void onOpen(TelnetConnection conn) {
            super.onOpen(conn);
            setSignalHandler(new Handler<Signal>() {
              StringBuilder buffer = new StringBuilder();
              AtomicInteger count = new AtomicInteger();
              @Override
              public void handle(Signal event) {
                setReadHandler(new Handler<int[]>() {
                  @Override
                  public void handle(int[] event) {
                    switch (count.getAndIncrement()) {
                      case 0:
                        assertEquals("hello", Helper.fromCodePoints(event));
                        latch.countDown();
                        break;
                      case 1:
                        assertEquals("bye", Helper.fromCodePoints(event));
                        testComplete();
                        break;
                      default:
                        fail("Too many requests");
                    }
                  }
                });
              }
            });
          }
        };
      }
    });
    assertConnect();
    assertWrite("hello");
    assertWrite(3);
    await(latch);
    assertWrite("bye");
    await();
  }
}