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
public class TelnetTermConnection extends TelnetConnection implements TermConnection {

  Handler<Map.Entry<Integer, Integer>> sizeHandler;
  HashMap.SimpleEntry<Integer, Integer> size;
  Handler<int[]> charsHandler;

  public TelnetTermConnection(Handler<byte[]> output) {
    super(output);
  }

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
      write(event);
    }
  });

  @Override
  protected void onSendBinary(boolean binary) {
    super.onSendBinary(binary);
    if (binary) {
      encoder.setCharset(StandardCharsets.UTF_8);
    }
  }

  @Override
  protected void onReceiveBinary(boolean binary) {
    super.onReceiveBinary(binary);
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
  protected void onOpen() {
    writeWillOption(Option.ECHO);
    writeWillOption(Option.SGA);
    writeDoOption(Option.NAWS);
    writeDoOption(Option.BINARY);
    writeWillOption(Option.BINARY);
    writeDoOption(Option.TERMINAL_TYPE);
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
