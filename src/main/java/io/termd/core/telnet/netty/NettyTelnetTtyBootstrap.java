package io.termd.core.telnet.netty;

import io.termd.core.telnet.TelnetTtyConnection;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Helper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NettyTelnetTtyBootstrap {

  private final NettyTelnetBootstrap telnet;
  private boolean outBinary;
  private boolean inBinary;
  private Charset charset = StandardCharsets.UTF_8;

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

  public boolean isOutBinary() {
    return outBinary;
  }

  /**
   * Enable or disable the TELNET BINARY option on output.
   *
   * @param outBinary true to require the client to receive binary
   * @return this object
   */
  public NettyTelnetTtyBootstrap setOutBinary(boolean outBinary) {
    this.outBinary = outBinary;
    return this;
  }

  public boolean isInBinary() {
    return inBinary;
  }

  /**
   * Enable or disable the TELNET BINARY option on input.
   *
   * @param inBinary true to require the client to emit binary
   * @return this object
   */
  public NettyTelnetTtyBootstrap setInBinary(boolean inBinary) {
    this.inBinary = inBinary;
    return this;
  }

  public Charset getCharset() {
    return charset;
  }

  public void setCharset(Charset charset) {
    this.charset = charset;
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
    telnet.start(() -> new TelnetTtyConnection(inBinary, outBinary, charset, factory), doneHandler);
  }

  public void stop(Consumer<Throwable> doneHandler) {
    telnet.stop(doneHandler);
  }
}
