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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class BinaryEncoder implements Consumer<int[]> {

  private volatile Charset charset;
  final Consumer<byte[]> onByte;

  public BinaryEncoder(Charset charset, Consumer<byte[]> onByte) {
    this.charset = charset;
    this.onByte = onByte;
  }

  /**
   * Set a new charset on the encoder.
   *
   * @param charset the new charset
   */
  public void setCharset(Charset charset) {
    this.charset = charset;
  }

  @Override
  public void accept(int[] codePoints) {
    final char[] tmp = new char[2];
    int capacity = 0;
    for (int codePoint : codePoints) {
      capacity += Character.charCount(codePoint);
    }
    CharBuffer charBuf = CharBuffer.allocate(capacity);
    for (int codePoint : codePoints) {
      int size = Character.toChars(codePoint, tmp, 0);
      charBuf.put(tmp, 0, size);
    }
    charBuf.flip();
    ByteBuffer bytesBuf = charset.encode(charBuf);
    byte[] bytes = bytesBuf.array();
    if (bytesBuf.limit() < bytesBuf.array().length) {
      bytes = Arrays.copyOf(bytes, bytesBuf.limit());
    }
    onByte.accept(bytes);
  }
}
