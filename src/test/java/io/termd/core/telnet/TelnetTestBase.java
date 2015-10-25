package io.termd.core.telnet;

import io.termd.core.TestBase;
import org.junit.Rule;

import java.io.Closeable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class TelnetTestBase extends TestBase {

  @Rule
  public TelnetServerRule server = new TelnetServerRule(serverFactory());

  @Rule
  public TelnetClientRule client = new TelnetClientRule();
  
  protected abstract Function<Supplier<TelnetHandler>, Closeable> serverFactory();

}
