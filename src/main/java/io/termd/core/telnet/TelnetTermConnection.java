package io.termd.core.telnet;

import io.termd.core.Handler;
import io.termd.core.io.BinaryDecoder;
import io.termd.core.io.BinaryEncoder;
import io.termd.core.term.TermConnection;

import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetTermConnection extends TelnetHandler implements TermConnection {

  private Handler<Map.Entry<Integer, Integer>> sizeHandler;
  private HashMap.SimpleEntry<Integer, Integer> size;
  private Handler<int[]> charsHandler;
  private TelnetConnection conn;

  private final BinaryDecoder decoder = new BinaryDecoder(512, StandardCharsets.US_ASCII, new Handler<int[]>() {
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
    decoder.setCharset(StandardCharsets.UTF_8);
  }

  @Override
  protected void onData(byte[] data) {
    if (decoder != null) {
      decoder.write(data);
    } else if (charsHandler != null) {
      int[] chars = new int[data.length];
      for (int i = 0;i < data.length;i++) {
        chars[i] = data[i];
      }
      charsHandler.handle(chars);
    }
  }

  @Override
  protected void onOpen(TelnetConnection conn) {
    this.conn = conn;
    conn.writeWillOption(Option.ECHO);
    conn.writeWillOption(Option.SGA);
    conn.writeDoOption(Option.NAWS);
    conn.writeDoOption(Option.BINARY);
    conn.writeWillOption(Option.BINARY);
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
