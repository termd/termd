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
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

/**
 * Ascii based telnet charset.
 *
 * The decoder transforms {@code \r\n} sequence and {@code \r0} to {@code \r}.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetCharset extends Charset {

  public static final Charset INSTANCE = new TelnetCharset();

  private TelnetCharset() {
    super("Telnet", new String[0]);
  }

  @Override
  public boolean contains(Charset cs) {
    return cs.name().equals(name());
  }

  @Override
  public CharsetDecoder newDecoder() {
    return new CharsetDecoder(this, 1.0f, 1.0f) {
      private boolean prevCR;
      @Override
      protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
        int pos = in.position();
        int limit = in.limit();
        try {
          while (pos < limit) {
            byte b = in.get(pos);
            char c;
            if (b >= 0) {
              if (prevCR && (b == '\n' || b == 0)) {
                pos++;
                prevCR = false;
                continue;
              }
              c = (char) b;
              prevCR = b == '\r';
            } else {
              c = (char)(256 + b);
            }
            if (out.position() >= out.limit()) {
              return CoderResult.OVERFLOW;
            }
            pos++;
            out.put(c);
          }
          return CoderResult.UNDERFLOW;
        } finally {
          in.position(pos);
        }
      }
    };
  }

  @Override
  public CharsetEncoder newEncoder() {
    throw new UnsupportedOperationException();
  }
}
