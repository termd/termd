package io.termd.core.telnet.netty;

import io.termd.core.telnet.TelnetTtyConnection;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Helper;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NettyTelnetTtyBootstrap {

  private final NettyTelnetBootstrap telnet;

  public NettyTelnetTtyBootstrap() {
    this.telnet = new NettyTelnetBootstrap();
  }

  public String getHost() {
    return telnet.getHost();
  }

  public NettyTelnetTtyBootstrap setHost(String host) {
    telnet.setHost(host);
    return this;
  }

  public int getPort() {
    return telnet.getPort();
  }

  public NettyTelnetTtyBootstrap setPort(int port) {
    telnet.setPort(port);
    return this;
  }


  public CompletableFuture<?> start(Consumer<TtyConnection> factory) {
    CompletableFuture<?> fut = new CompletableFuture<>();
    start(factory, Helper.startedHandler(fut));
    return fut;
  }

  public CompletableFuture<?> stop() {
    CompletableFuture<?> fut = new CompletableFuture<>();
    stop(Helper.stoppedHandler(fut));
    return fut;
  }

  public void start(Consumer<TtyConnection> factory, Consumer<Throwable> doneHandler) {
    telnet.start(() -> new TelnetTtyConnection(factory), doneHandler);
  }

  public void stop(Consumer<Throwable> doneHandler) {
    telnet.stop(doneHandler);
  }
}
