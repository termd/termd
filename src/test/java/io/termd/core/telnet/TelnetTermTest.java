package io.termd.core.telnet;

import io.termd.core.Handler;
import io.termd.core.Provider;
import io.termd.core.telnet.vertx.VertxTermConnection;
import io.termd.core.term.TermEvent;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.WindowSizeOptionHandler;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetTermTest extends TelnetTestBase {

  @Test
  public void testSizeHanlder() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    server(new Provider<TelnetHandler>() {
      @Override
      public TelnetHandler provide() {
        final AtomicInteger count = new AtomicInteger();
        final TelnetTermConnection connection = new VertxTermConnection();
        connection.eventHandler(new Handler<TermEvent>() {
          @Override
          public void handle(TermEvent event) {
            if (event instanceof TermEvent.Size) {
              TermEvent.Size size = (TermEvent.Size) event;
              switch (count.getAndIncrement()) {
                case 0:
                  assertEquals(20, size.getWidth());
                  assertEquals(10, size.getHeight());
                  latch.countDown();
                  break;
                case 1:
                  assertEquals(80, size.getWidth());
                  assertEquals(24, size.getHeight());
                  connection.eventHandler(null);
                  connection.eventHandler(new Handler<TermEvent>() {
                    @Override
                    public void handle(TermEvent event) {
                      TermEvent.Size size = (TermEvent.Size) event;
                      assertEquals(80, size.getWidth());
                      assertEquals(24, size.getHeight());
                      testComplete();
                    }
                  });
                  break;
                default:
                  fail("Was not expecting that");
              }
            }
          }
        });
        return connection;
      }
    });
    WindowSizeOptionHandler optionHandler = new WindowSizeOptionHandler(20, 10, false, false, true, false);
    final AtomicReference<OutputStream> out = new AtomicReference<>();
    client = new TelnetClient() {
      @Override
      protected void _connectAction_() throws IOException {
        super._connectAction_();
        out.set(_output_);
      }
    };
    client.addOptionHandler(optionHandler);
    client.connect("localhost", 4000);
    latch.await(30, TimeUnit.SECONDS);
    out.get().write(new byte[]{TelnetConnection.BYTE_IAC, TelnetConnection.BYTE_SB, 31, 0, 80, 0, 24, TelnetConnection.BYTE_IAC, TelnetConnection.BYTE_SE});
    out.get().flush();
    await();
  }
}
