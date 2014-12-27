package io.modsh.core.telnet;

import io.modsh.core.Handler;
import io.modsh.core.io.BinaryDecoder;
import io.modsh.core.io.BinaryEncoder;
import io.modsh.core.term.TermConnection;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetTermConnection extends TelnetConnection implements TermConnection {

  BinaryDecoder decoder;
  BinaryEncoder encoder;
  Handler<Map.Entry<Integer, Integer>> sizeHandler;
  HashMap.SimpleEntry<Integer, Integer> size;
  Handler<int[]> charsHandler;

  public TelnetTermConnection(Handler<byte[]> output) {
    super(output);
  }

  @Override
  protected void onSendBinary(boolean binary) {
    super.onSendBinary(binary);
    if (binary) {
      encoder = new BinaryEncoder(TelnetConnection.UTF_8, new Handler<byte[]>() {
        @Override
        public void handle(byte[] event) {
          write(event);
        }
      });
    }
  }

  @Override
  protected void onReceiveBinary(boolean binary) {
    super.onReceiveBinary(binary);
    decoder = new BinaryDecoder(TelnetConnection.UTF_8, new Handler<int[]>() {
      @Override
      public void handle(int[] event) {
        if (charsHandler != null) {
          charsHandler.handle(event);
        }
      }
    });
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
}
