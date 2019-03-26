/*
 * Copyright 2015 Julien Viet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.termd.core.io;

import io.termd.core.util.Helper;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class BinaryDecoder {

  private static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

  private CharsetDecoder decoder;
  private ByteBuffer bBuf;
  private final CharBuffer cBuf;
  private final Consumer<int[]> onChar;

  public BinaryDecoder(Charset charset, Consumer<int[]> onChar) {
    this(2, charset, onChar);
  }

  public BinaryDecoder(int initialSize, Charset charset, Consumer<int[]> onChar) {
    if (initialSize < 2) {
      throw new IllegalArgumentException("Initial size must be at least 2");
    }
    decoder = charset.newDecoder();
    decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
    decoder.onMalformedInput(CodingErrorAction.REPLACE);
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
    decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
    decoder.onMalformedInput(CodingErrorAction.REPLACE);
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
      onChar.accept(codePoints);
      cBuf.compact();
      if (result.isOverflow()) {
        // We still have work to do
      } else if (result.isUnderflow()) {
        if (bBuf.hasRemaining()) {
          // We need more input
          Helper.noop();
        } else {
          // We are done
          Helper.noop();
        }
        break;
      } else {
        throw new UnsupportedOperationException("Handle me gracefully");
      }
    }
    bBuf.compact();
  }
}
