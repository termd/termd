package io.termd.core.telnet;

import org.apache.commons.net.telnet.TelnetClient;
import org.junit.After;
import org.junit.Rule;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class TelnetTestBase extends TestBase {

  @Rule
  public TelnetServerRule server = new TelnetServerRule(serverFactory());
  protected TelnetClient client;
  
  protected abstract Function<Supplier<TelnetHandler>, Closeable> serverFactory();

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


  @After
  public void after() throws Exception {
    if (client != null && client.isConnected()) {
      try {
        client.disconnect();
      } catch (IOException ignore) {
      }
    }
  }
}
