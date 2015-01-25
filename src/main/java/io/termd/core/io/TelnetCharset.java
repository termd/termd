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
      @Override
      protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
        int pos = in.position();
        int limit = in.limit();
        try {
          while (pos < limit) {
            if (out.position() >= out.limit()) {
              return CoderResult.OVERFLOW;
            }
            byte b = in.get(pos);
            char c;
            if (b >= 0) {
              c = (char) b;
              if (b == '\r') {
                if (pos + 1 >= limit) {
                  return CoderResult.UNDERFLOW;
                }
                byte next = in.get(pos + 1);
                if (next == '\n' || next == 0) {
                  pos++;
                  c = '\r';
                }
              }
            } else {
              c = (char)(256 + b);
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
