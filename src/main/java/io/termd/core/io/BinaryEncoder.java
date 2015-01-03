package io.termd.core.io;

import io.termd.core.Handler;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class BinaryEncoder implements Handler<int[]> {

  final CharsetEncoder decoder;
  final ByteBuffer bBuf;
  final CharBuffer cBuf;
  final Handler<byte[]> onByte;
  final char[] tmp;

  public BinaryEncoder(Charset charset, Handler<byte[]> onByte) {
    this(0, charset, onByte);
  }

  public BinaryEncoder(int bufferSize, Charset charset, Handler<byte[]> onByte) {
    decoder = charset.newEncoder();
    int estimated;
    if (charset.name().equals("UTF-8")) {
      estimated = 4; // See justification here http://bugs.java.com/view_bug.do?bug_id=6957230
    } else {
      estimated = (int) (decoder.maxBytesPerChar() * 2);
    }
    if (bufferSize <= 0) {
      bufferSize = estimated;
    } else {
      if (bufferSize < estimated) {
        throw new IllegalArgumentException("Invalid byte buffer size " + bufferSize + " < max byte per char " + decoder.maxBytesPerChar());
      }
    }
    bBuf = ByteBuffer.allocate(bufferSize);
    cBuf = CharBuffer.allocate(2);
    tmp = new char[2];
    this.onByte = onByte;
  }

  @Override
  public void handle(int[] event) {

    for (int codePoint : event) {
      try {
        int len = Character.toChars(codePoint, tmp, 0);
        cBuf.put(tmp, 0, len);
        cBuf.flip();
        while (true) {
          CoderResult result = decoder.encode(cBuf, bBuf, false);
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
    onByte.handle(bytes);
  }
}
