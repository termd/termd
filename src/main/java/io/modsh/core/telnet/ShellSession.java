package io.modsh.core.telnet;

import io.modsh.core.Handler;
import io.modsh.core.io.BinaryDecoder;
import io.modsh.core.io.BinaryEncoder;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ShellSession extends TelnetSession {

  BinaryDecoder decoder;
  BinaryEncoder encoder;

  public ShellSession(Handler<byte[]> output) {
    super(output);
  }

  @Override
  protected void onSendBinary(boolean binary) {
    super.onSendBinary(binary);
    if (binary) {
      encoder = new BinaryEncoder(TelnetSession.UTF_8, new Handler<Byte>() {
        @Override
        public void handle(Byte event) {
          write(new byte[]{event});
        }
      });
    }
  }

  @Override
  protected void onReceiveBinary(boolean binary) {
    super.onReceiveBinary(binary);
    decoder = new BinaryDecoder(TelnetSession.UTF_8, new Handler<Integer>() {
      @Override
      public void handle(Integer event) {
        onChar(event);
      }
    });
  }

  @Override
  protected void onByte(byte b) {
    if (decoder != null) {
      decoder.onByte(b);
    } else {
      onChar((char) b);
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

  protected void onChar(int c) {}

}
