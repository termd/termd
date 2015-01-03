package io.termd.core.io;

import io.termd.core.Handler;
import io.termd.core.Helper;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
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
    BinaryEncoder encoder = new BinaryEncoder(UTF8, new Handler<byte[]>() {
      @Override
      public void handle(byte[] event) {
        for (byte b : event) {
          actualBytes.add(b);
        }
      }
    });
    encoder.handle(Helper.toCodePoints(s));
    ArrayList<Byte> expectedBytes = new ArrayList<>();
    for (int b : expected) {
      expectedBytes.add((byte) b);
    }
    assertEquals(expectedBytes, actualBytes);
    final StringBuilder sb = new StringBuilder();
    BinaryDecoder decoder = new BinaryDecoder(UTF8, new Handler<int[]>() {
      @Override
      public void handle(int[] event) {
        for (int cp : event) {
          sb.appendCodePoint(cp);
        }
      }
    });
    byte[] data = new byte[expected.length];
    for (int i = 0;i < expected.length;i++) {
      data[i] = (byte) expected[i];
    }
    decoder.write(data);
    assertEquals(s, sb.toString());
  }

  private void assertDecode(int initialSize, List<String> chars, int... bytes) {
    final List<String> abc = new ArrayList<>();
    BinaryDecoder decoder = new BinaryDecoder(initialSize, UTF8, new Handler<int[]>() {
      @Override
      public void handle(int[] event) {
        StringBuilder sb = new StringBuilder();
        for (int cp : event) {
          sb.appendCodePoint(cp);
        }
        abc.add(sb.toString());
      }
    });
    byte[] data = new byte[bytes.length];
    for (int i = 0;i < bytes.length;i++) {
      data[i] = (byte) bytes[i];
    }
    decoder.write(data);
    assertEquals(chars, abc);
  }

  @Test
  public void testDecoderOverflow() throws Exception {
    assertDecode(2, Arrays.asList("AB", "CD", "E"), 65, 66, 67, 68, 69);
    assertDecode(3, Arrays.asList("ABC", "DE"), 65, 66, 67, 68, 69);
    assertDecode(4, Arrays.asList("ABCD", "E"), 65, 66, 67, 68, 69);
    assertDecode(5, Arrays.asList("ABCDE"), 65, 66, 67, 68, 69);
    assertDecode(6, Arrays.asList("ABCDE"), 65, 66, 67, 68, 69);
  }

  @Test
  public void testDecodeMalformed() throws Exception {
    // Todo
  }

  @Test
  public void testEncoderOverflow() throws Exception {
    assertEncode(4, "ABCDE", new int[]{65, 66, 67, 68}, new int[]{69});
    assertEncode(5, "ABCDE", new int[]{65, 66, 67, 68, 69});
  }

  @Test
  public void testEncodeMalformed() throws Exception {
    // Todo
  }

  private void assertEncode(int bufferSize, String chars, int[]... bytes) {
    final List<int[]> abc = new ArrayList<>();
    BinaryEncoder decoder = new BinaryEncoder(bufferSize, UTF8, new Handler<byte[]>() {
      @Override
      public void handle(byte[] event) {
        int[] def = new int[event.length];
        for (int i = 0;i < event.length;i++) {
          def[i] = event[i];
        }
        abc.add(def);
      }
    });
    decoder.handle(Helper.toCodePoints(chars));
    assertEquals(bytes.length, abc.size());
    for (int i = 0;i < bytes.length;i++) {
      assertArrayEquals(bytes[i], abc.get(i));
    }
  }
}
