package io.modsh.core.io;

import io.modsh.core.Handler;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class BinaryEncoder implements Handler<Integer> {

  final CharsetEncoder decoder;
  final ByteBuffer bBuf;
  final CharBuffer cBuf;
  final Handler<Byte> onByte;

  public BinaryEncoder(Charset charset, Handler<Byte> onByte) {
    decoder = charset.newEncoder();
    bBuf = ByteBuffer.allocate(4);
    cBuf = CharBuffer.allocate(2);
    this.onByte = onByte;
  }

  public void handle(Integer c) {
    switch (Character.charCount(c)) {
      case 1:
        onChar((char)(int) c);
        break;
      case 2:
        for (char ch : Character.toChars(c)) {
          onChar(ch);
        }
        break;
      default:
        throw new AssertionError();
    }
  }

  private void onChar(char c) {
    cBuf.put(c);
    cBuf.flip();
    decoder.encode(cBuf, bBuf, false);
    bBuf.flip();
    while (bBuf.hasRemaining()) {
      byte b = bBuf.get();
      onByte.handle(b);
    }
    cBuf.compact();
    bBuf.compact();
  }
}
