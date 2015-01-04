package io.termd.core.telnet.netty;

import io.termd.core.telnet.TelnetTermConnection;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NettyTermConnection extends TelnetTermConnection {
  @Override
  public void schedule(Runnable task) {
    ((NettyTelnetConnection) conn).context.channel().eventLoop().schedule(task, 0, TimeUnit.SECONDS);
  }
}
