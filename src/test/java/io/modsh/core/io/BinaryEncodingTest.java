package io.modsh.core.io;

import io.modsh.core.Handler;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class BinaryEncodingTest {

  private static final Charset UTF8 = Charset.forName("UTF-8");

  @Test
  public void testChars() throws IOException {
    testChars("A", 65);
    testChars("\u20AC", -30, -126, -84); // Euro
    testChars(new StringBuilder().appendCodePoint(66231).toString(), -16, -112, -118, -73); // Surrogate
  }

  private void testChars(String s, int... expected) {
    final ArrayList<Byte> actualBytes = new ArrayList<>();
    BinaryEncoder encoder = new BinaryEncoder(UTF8, new Handler<Byte>() {
      @Override
      public void handle(Byte event) {
        actualBytes.add(event);
      }
    });
    for (int offset = 0;offset < s.length();) {
      int cp = s.codePointAt(offset);
      encoder.handle(cp);
      offset += Character.charCount(cp);
    }
    ArrayList<Byte> expectedBytes = new ArrayList<>();
    for (int b : expected) {
      expectedBytes.add((byte) b);
    }
    assertEquals(expectedBytes, actualBytes);
    final StringBuilder sb = new StringBuilder();
    BinaryDecoder decoder = new BinaryDecoder(UTF8, new Handler<Integer>() {
      @Override
      public void handle(Integer event) {
        sb.appendCodePoint(event);
      }
    });
    for (int i : expected) {
      decoder.onByte((byte) i);
    }
    assertEquals(s, sb.toString());
  }
}
