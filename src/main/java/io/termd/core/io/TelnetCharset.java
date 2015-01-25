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
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetCharset extends Charset {

  public final CharsetDecoder DECODER = new CharsetDecoder(this, 1.0f, 1.0f) {
    @Override
    protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
      while (in.position() < in.limit()) {
        byte b = in.get(in.position());
        if (out.position() >= out.limit()) {
          return CoderResult.OVERFLOW;
        }
        out.put((char) b);
      }
      return CoderResult.UNDERFLOW;
    }
  };

  public TelnetCharset() {
    super("BILTO", new String[0]);
  }

  @Override
  public boolean contains(Charset cs) {
    return cs.name().equals(name());
  }

  @Override
  public CharsetDecoder newDecoder() {
    return DECODER;
  }

  @Override
  public CharsetEncoder newEncoder() {
    throw new UnsupportedOperationException();
  }
}
