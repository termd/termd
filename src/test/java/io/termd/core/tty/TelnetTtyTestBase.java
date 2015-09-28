package io.termd.core.tty;

import io.termd.core.telnet.TelnetClientRule;
import io.termd.core.telnet.TelnetHandler;
import io.termd.core.telnet.TelnetServerRule;
import io.termd.core.telnet.TelnetTtyConnection;
import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.SimpleOptionHandler;
import org.apache.commons.net.telnet.TerminalTypeOptionHandler;
import org.apache.commons.net.telnet.WindowSizeOptionHandler;
import org.junit.Rule;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class TelnetTtyTestBase extends TtyTestBase {

  protected boolean binary;
  private WindowSizeOptionHandler wsHandler;

  @Rule
  public TelnetServerRule server = new TelnetServerRule(serverFactory());

  @Rule
  public TelnetClientRule client = new TelnetClientRule();

  protected abstract Function<Supplier<TelnetHandler>, Closeable> serverFactory();

  protected void server(Consumer<TtyConnection> onConnect) {
    server.start(() -> new TelnetTtyConnection(onConnect));
  }

  @Override
  protected void resize(int width, int height) {
  }

  @Override
  protected void assertConnect(String term) throws Exception {
    client.setOptionHandler(new EchoOptionHandler(false, false, true, true));
    if (binary) {
      client.setOptionHandler(new SimpleOptionHandler(0, false, false, true, true));
    }
    if (term != null) {
      client.setOptionHandler(new TerminalTypeOptionHandler(term, false, false, true, false));
    }
    client.connect("localhost", 4000);
  }

  @Override
  protected void assertWrite(String s) throws Exception {
    client.write(s.getBytes(StandardCharsets.UTF_8));
    client.flush();
  }

  protected final void assertWriteln(String s) throws Exception {
    assertWrite(s + (binary ? "\r" : "\r\n"));
  }

  @Override
  protected String assertReadString(int len) throws Exception {
    return client.assertReadString(len);
  }

  @Override
  public void testSize() throws Exception {
    wsHandler = new WindowSizeOptionHandler(80, 24, false, false, true, true);
    client.setOptionHandler(wsHandler);
    super.testSize();
  }

  @Override
  public void testResize() throws Exception {
    // Cannot be tested with this client that does not support resize
  }
}
