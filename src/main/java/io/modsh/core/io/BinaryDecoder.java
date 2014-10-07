package io.modsh.core.io;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.function.IntConsumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class BinaryDecoder {

  final CharsetDecoder decoder;
  final ByteBuffer bBuf;
  final CharBuffer cBuf;
  final IntConsumer onChar;

  public BinaryDecoder(Charset charset, IntConsumer onChar) {
    decoder = charset.newDecoder();
    bBuf = ByteBuffer.allocate(4);
    cBuf = CharBuffer.allocate(1);
    this.onChar = onChar;
  }

  public void onByte(byte b) {
    bBuf.put(b);
    bBuf.flip();
    decoder.decode(bBuf, cBuf, false);
    cBuf.flip();
    while (cBuf.hasRemaining()) {
      char c = cBuf.get();
      onChar.accept(c);
    }
    bBuf.compact();
    cBuf.compact();
  }
}
