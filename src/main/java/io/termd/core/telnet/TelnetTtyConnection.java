package io.termd.core.telnet;

import io.termd.core.tty.ReadBuffer;
import io.termd.core.tty.Signal;
import io.termd.core.tty.SignalDecoder;
import io.termd.core.util.Dimension;
import io.termd.core.util.Handler;
import io.termd.core.io.BinaryDecoder;
import io.termd.core.io.BinaryEncoder;
import io.termd.core.io.TelnetCharset;
import io.termd.core.tty.TtyConnection;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

/**
 * A telnet handler that implements {@link io.termd.core.tty.TtyConnection}.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetTtyConnection extends TelnetHandler implements TtyConnection {

  private Dimension size;
  private String terminalType;
  private Handler<Dimension> resizeHandler;
  private Handler<String> termHandler;
  protected TelnetConnection conn;
  private final ReadBuffer readBuffer = new ReadBuffer(new Executor() {
    @Override
    public void execute(Runnable command) {
      schedule(command);
    }
  });
  private final SignalDecoder signalDecoder = new SignalDecoder(3).setReadHandler(readBuffer);
  private final BinaryDecoder decoder = new BinaryDecoder(512, TelnetCharset.INSTANCE, signalDecoder);
  private final BinaryEncoder encoder = new BinaryEncoder(512, StandardCharsets.US_ASCII, new Handler<byte[]>() {
    @Override
    public void handle(byte[] event) {
      conn.write(event);
    }
  });

  public TelnetTtyConnection() {
  }

  @Override
  public void schedule(Runnable task) {
    conn.schedule(task);
  }

  @Override
  protected void onSendBinary(boolean binary) {
    if (binary) {
      encoder.setCharset(StandardCharsets.UTF_8);
    }
  }

  @Override
  protected void onReceiveBinary(boolean binary) {
    if (binary) {
      decoder.setCharset(StandardCharsets.UTF_8);
    }
  }

  @Override
  protected void onData(byte[] data) {
    decoder.write(data);
  }

  @Override
  protected void onOpen(TelnetConnection conn) {
    this.conn = conn;

    // Kludge mode
    conn.writeWillOption(Option.ECHO);
    conn.writeWillOption(Option.SGA);

    // Window size
    conn.writeDoOption(Option.NAWS);

    // Binary by all means
    conn.writeDoOption(Option.BINARY);
    conn.writeWillOption(Option.BINARY);

    // Get some info about user
    conn.writeDoOption(Option.TERMINAL_TYPE);
  }

  @Override
  protected void onTerminalType(String terminalType) {
    this.terminalType = terminalType;
    if (termHandler != null) {
      termHandler.handle(terminalType);
    }
  }

  @Override
  protected void onSize(int width, int height) {
    this.size = new Dimension(width, height);
    if (resizeHandler != null) {
      resizeHandler.handle(size);
    }
  }

  @Override
  public Handler<Dimension> getResizeHandler() {
    return resizeHandler;
  }

  @Override
  public void setResizeHandler(Handler<Dimension> handler) {
    this.resizeHandler = handler;
    if (handler != null && size != null) {
      handler.handle(size);
    }
  }

  @Override
  public Handler<String> getTermHandler() {
    return termHandler;
  }

  @Override
  public void setTermHandler(Handler<String> handler) {
    termHandler = handler;
    if (handler != null && terminalType != null) {
      handler.handle(terminalType);
    }
  }

  @Override
  public Handler<Signal> getSignalHandler() {
    return signalDecoder.getSignalHandler();
  }

  @Override
  public void setSignalHandler(Handler<Signal> handler) {
    signalDecoder.setSignalHandler(handler);
  }

  @Override
  public Handler<int[]> getReadHandler() {
    return readBuffer.getReadHandler();
  }

  @Override
  public void setReadHandler(Handler<int[]> handler) {
    readBuffer.setReadHandler(handler);
  }

  @Override
  public Handler<int[]> writeHandler() {
    return encoder;
  }
}
