package io.termd.core.telnet;

import io.termd.core.Handler;
import io.termd.core.io.BinaryDecoder;
import io.termd.core.io.BinaryEncoder;
import io.termd.core.io.TelnetCharset;
import io.termd.core.term.TermConnection;

import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class TelnetTermConnection extends TelnetHandler implements TermConnection {

  private Handler<Map.Entry<Integer, Integer>> sizeHandler;
  private HashMap.SimpleEntry<Integer, Integer> size;
  private Handler<int[]> charsHandler;
  protected TelnetConnection conn;

  private final BinaryDecoder decoder = new BinaryDecoder(512, TelnetCharset.INSTANCE, new Handler<int[]>() {
    @Override
    public void handle(int[] event) {
      if (charsHandler != null) {
        charsHandler.handle(event);
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
    size = new AbstractMap.SimpleEntry<>(width, height);
    if (sizeHandler != null) {
      sizeHandler.handle(new AbstractMap.SimpleEntry<>(size));
    }
  }

  @Override
  public void sizeHandler(Handler<Map.Entry<Integer, Integer>> handler) {
    sizeHandler = handler;
    if (handler != null && size != null) {
      handler.handle(new AbstractMap.SimpleEntry<>(size));
    }
  }

  @Override
  public void charsHandler(Handler<int[]> handler) {
    charsHandler = handler;
  }

  @Override
  public Handler<int[]> charsHandler() {
    return encoder;
  }
}
