package io.modsh.core.io;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import static org.junit.Assert.*;

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
    ArrayList<Byte> actualBytes = new ArrayList<>();
    BinaryEncoder encoder = new BinaryEncoder(UTF8, actualBytes::add);
    s.codePoints().forEach(encoder::accept);
    ArrayList<Byte> expectedBytes = new ArrayList<>();
    for (int b : expected) {
      expectedBytes.add((byte) b);
    }
    assertEquals(expectedBytes, actualBytes);
    StringBuilder sb = new StringBuilder();
    BinaryDecoder decoder = new BinaryDecoder(UTF8, sb::appendCodePoint);
    for (int i : expected) {
      decoder.onByte((byte) i);
    }
    assertEquals(s, sb.toString());
  }
}
