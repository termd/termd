package io.termd.core.telnet;

import io.termd.core.util.Handler;
import io.termd.core.io.BinaryDecoder;
import io.termd.core.io.BinaryEncoder;
import io.termd.core.io.TelnetCharset;
import io.termd.core.term.TermConnection;
import io.termd.core.term.TermEvent;

import java.nio.charset.StandardCharsets;

/**
 * A telnet handler that implements {@link io.termd.core.term.TermConnection}.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetTermConnection extends TelnetHandler implements TermConnection {

  private int width = -1;
  private int height = -1;
  private Handler<TermEvent> eventHandler;
  protected TelnetConnection conn;

  private final BinaryDecoder decoder = new BinaryDecoder(512, TelnetCharset.INSTANCE, new Handler<int[]>() {
    @Override
    public void handle(int[] event) {
      if (eventHandler != null) {
        eventHandler.handle(new TermEvent.Read(event));
      }
    }
  });
  private final BinaryEncoder encoder = new BinaryEncoder(512, StandardCharsets.US_ASCII, new Handler<byte[]>() {
    @Override
    public void handle(byte[] event) {
      conn.write(event);
    }
  });

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
  protected void onSize(int width, int height) {
    this.width = width;
    this.height = height;
    if (eventHandler != null) {
      eventHandler.handle(new TermEvent.Size(width, height));
    }
  }

  @Override
  public void eventHandler(Handler<TermEvent> handler) {
    this.eventHandler = handler;
    if (handler != null && width >= 0 && height >= 0) {
      handler.handle(new TermEvent.Size(width, height));
    }
  }

  @Override
  public Handler<int[]> dataHandler() {
    return encoder;
  }
}
