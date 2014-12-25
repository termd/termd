package io.modsh.core.io;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class BinaryEncoder implements IntConsumer {

  final CharsetEncoder decoder;
  final ByteBuffer bBuf;
  final CharBuffer cBuf;
  final Consumer<Byte> onByte;

  public BinaryEncoder(Charset charset, Consumer<Byte> onByte) {
    decoder = charset.newEncoder();
    bBuf = ByteBuffer.allocate(4);
    cBuf = CharBuffer.allocate(2);
    this.onByte = onByte;
  }

  public void accept(int c) {
    switch (Character.charCount(c)) {
      case 1:
        onChar((char) c);
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
      onByte.accept(b);
    }
    cBuf.compact();
    bBuf.compact();
  }
}
