package io.termd.core.io;

import io.termd.core.Handler;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class BinaryDecoder {

  private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

  private CharsetDecoder decoder;
  private ByteBuffer bBuf;
  private final CharBuffer cBuf;
  private final Handler<int[]> onChar;

  public BinaryDecoder(Charset charset, Handler<int[]> onChar) {
    this(2, charset, onChar);
  }

  public BinaryDecoder(int initialSize, Charset charset, Handler<int[]> onChar) {
    if (initialSize < 2) {
      throw new IllegalArgumentException("Initial size must be at least 2");
    }
    decoder = charset.newDecoder();
    bBuf = EMPTY;
    cBuf = CharBuffer.allocate(initialSize); // We need at least 2
    this.onChar = onChar;
  }

  /**
   * Set a new charset on the decoder.
   *
   * @param charset the new charset
   */
  public void setCharset(Charset charset) {
    decoder = charset.newDecoder();
  }

  public void write(byte[] data) {
    write(data, 0, data.length);
  }

  public void write(byte[] data, int start, int len) {

    // Fill the byte buffer
    int remaining = bBuf.remaining();
    if (len > remaining) {
      // Allocate a new buffer
      ByteBuffer tmp = bBuf;
      int length = tmp.position() + len;
      bBuf = ByteBuffer.allocate(length);
      tmp.flip();
      bBuf.put(tmp);
    }
    bBuf.put(data, start, len);
    bBuf.flip();

    // Drain the byte buffer
    while (true) {
      IntBuffer iBuf = IntBuffer.allocate(bBuf.remaining());
      CoderResult result = decoder.decode(bBuf, cBuf, false);
      cBuf.flip();
      while (cBuf.hasRemaining()) {
        char c = cBuf.get();
        if (Character.isSurrogate(c)) {
          if (Character.isHighSurrogate(c)) {
            if (cBuf.hasRemaining()) {
              char low = cBuf.get();
              if (Character.isLowSurrogate(low)) {
                int codePoint = Character.toCodePoint(c, low);
                if (Character.isValidCodePoint(codePoint)) {
                  iBuf.put(codePoint);
                } else {
                  throw new UnsupportedOperationException("Handle me gracefully");
                }
              } else {
                throw new UnsupportedOperationException("Handle me gracefully");
              }
            } else {
              throw new UnsupportedOperationException("Handle me gracefully");
            }
          } else {
            throw new UnsupportedOperationException("Handle me gracefully");
          }
        } else {
          iBuf.put((int) c);
        }
      }
      iBuf.flip();
      int[] codePoints = new int[iBuf.limit()];
      iBuf.get(codePoints);
      onChar.handle(codePoints);
      cBuf.compact();
      if (result.isOverflow()) {
        // We still have work to do
      } else if (result.isUnderflow()) {
        // Either we are done or we need more input
        break;
      } else {
        throw new UnsupportedOperationException("Handle me gracefully");
      }
    }
    bBuf.compact();
  }
}
