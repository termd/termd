package io.termd.core.io;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class BinaryEncoder implements Consumer<int[]> {

  private CharsetEncoder encoder;
  final ByteBuffer bBuf;
  final CharBuffer cBuf;
  final Consumer<byte[]> onByte;
  final char[] tmp;

  public BinaryEncoder(Charset charset, Consumer<byte[]> onByte) {
    this(0, charset, onByte);
  }

  public BinaryEncoder(int bufferSize, Charset charset, Consumer<byte[]> onByte) {
    encoder = charset.newEncoder();
    int estimated;
    if (charset.name().equals("UTF-8")) {
      estimated = 4; // See justification here http://bugs.java.com/view_bug.do?bug_id=6957230
    } else {
      estimated = (int) (encoder.maxBytesPerChar() * 2);
    }
    if (bufferSize <= 0) {
      bufferSize = estimated;
    } else {
      if (bufferSize < estimated) {
        throw new IllegalArgumentException("Invalid byte buffer size " + bufferSize + " < max byte per char " + encoder.maxBytesPerChar());
      }
    }
    bBuf = ByteBuffer.allocate(bufferSize);
    cBuf = CharBuffer.allocate(2);
    tmp = new char[2];
    this.onByte = onByte;
  }

  /**
   * Set a new charset on the encoder.
   *
   * @param charset the new charset
   */
  public void setCharset(Charset charset) {
    encoder = charset.newEncoder();
  }

  @Override
  public void accept(int[] event) {

    for (int codePoint : event) {
      try {
        int len = Character.toChars(codePoint, tmp, 0);
        cBuf.put(tmp, 0, len);
        cBuf.flip();
        while (true) {
          CoderResult result = encoder.encode(cBuf, bBuf, false);
          if (result.isUnderflow()) {
            break;
          } else if (result.isOverflow()) {
            drainByteBuffer();
          } else {
            throw new UnsupportedOperationException("Handle me gracefully");
          }
        }
        cBuf.compact();
      } catch (IllegalArgumentException e) {
        // Skip invalid
      }
    }
    drainByteBuffer();



  }

  private void drainByteBuffer() {
    bBuf.flip();
    byte[] bytes = new byte[bBuf.limit() - bBuf.position()];
    bBuf.get(bytes);
    bBuf.compact();
    onByte.accept(bytes);
  }
}
