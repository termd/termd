package io.termd.core.tty;

import io.termd.core.telnet.TelnetHandler;

import java.io.Closeable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxAsciiReadlineTtyTest extends ReadlineTermTtyBase {

  public VertxAsciiReadlineTtyTest() {
    binary = false;
  }

  @Override
  protected Function<Supplier<TelnetHandler>, Closeable> serverFactory() {
    return VERTX_SERVER;
  }
}
