package io.modsh.core.telnet;

import io.modsh.core.Function;
import io.modsh.core.Handler;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.WindowSizeOptionHandler;
import org.junit.Test;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
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
    server(new Function<Handler<byte[]>, TelnetConnection>() {
      @Override
      public TelnetConnection call(Handler<byte[]> argument) {
        final AtomicInteger count = new AtomicInteger();
        final TelnetTermConnection connection = new TelnetTermConnection(argument);
        connection.sizeHandler(new Handler<Map.Entry<Integer, Integer>>() {
          @Override
          public void handle(Map.Entry<Integer, Integer> event) {
            switch (count.getAndIncrement()) {
              case 0:
                assertEquals(20, event.getKey());
                assertEquals(10, event.getValue());
                latch.countDown();
                break;
              case 1:
                assertEquals(80, event.getKey());
                assertEquals(24, event.getValue());
                connection.sizeHandler(null);
                connection.sizeHandler(new Handler<Map.Entry<Integer, Integer>>() {
                  @Override
                  public void handle(Map.Entry<Integer, Integer> event) {
                    assertEquals(80, event.getKey());
                    assertEquals(24, event.getValue());
                    testComplete();
                  }
                });
                break;
              default:
                fail("Was not expecting that");
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
