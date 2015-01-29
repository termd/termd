package io.termd.core.io;

import io.termd.core.util.Handler;
import io.termd.core.util.Helper;
import org.junit.Test;

import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetCharsetTest {

  @Test
  public void testDecodeSingleByte() {
    for (int i = 13;i < 256;i++) {
      byte[] bytes = {(byte) i};
      assertEquals("Invalid encoding at " + i, new String(new char[]{(char) i}), TelnetCharset.INSTANCE.decode(ByteBuffer.wrap(bytes)).toString());
    }
  }

  @Test
  public void testDecodeByte() {
    for (int i = 0;i < 256;i++) {
      byte[] bytes = {(byte) i, 'A'};
      assertEquals("Invalid encoding at " + i, new String(new char[]{(char)i, 'A'}), TelnetCharset.INSTANCE.decode(ByteBuffer.wrap(bytes)).toString());
    }
  }

  @Test
  public void testDecodeCRLF() {
    for (int i = 0;i < 256;i++) {
      byte[] bytes = {(byte) i, '\n'};
      if (i != '\r') {
        assertEquals("Invalid encoding at " + i, new String(new char[]{(char)i, '\n'}), TelnetCharset.INSTANCE.decode(ByteBuffer.wrap(bytes)).toString());
      } else {
        assertEquals("Invalid encoding at " + i, "\r", TelnetCharset.INSTANCE.decode(ByteBuffer.wrap(bytes)).toString());
      }
    }
  }

  @Test
  public void testDecodeCRNULL() {
    for (int i = 0;i < 256;i++) {
      byte[] bytes = {(byte) i, 0};
      if (i != '\r') {
        assertEquals("Invalid encoding at " + i, new String(new char[]{(char)i, 0}), TelnetCharset.INSTANCE.decode(ByteBuffer.wrap(bytes)).toString());
      } else {
        assertEquals("Invalid encoding at " + i, "\r", TelnetCharset.INSTANCE.decode(ByteBuffer.wrap(bytes)).toString());
      }
    }
  }

  @Test
  public void testDecoderOverflow() {
    CharsetDecoder decoder = TelnetCharset.INSTANCE.newDecoder();
    assertTrue(decoder.decode(ByteBuffer.wrap(new byte[]{'A', 'B'}), CharBuffer.allocate(1), true).isOverflow());
  }

  @Test
  public void testBinaryDecoder() {
    byte[] input = { '\n', 0, 'A'};
    int[][] expectedOutput = {{'\r'},{'\r'},{'\r','A'}};
    for (int i = 0;i < input.length;i++) {
      final ArrayList<Integer> codePoints = new ArrayList<>();
      BinaryDecoder decoder = new BinaryDecoder(512, TelnetCharset.INSTANCE, new Handler<int[]>() {
        @Override
        public void handle(int[] event) {
          for (int i : event) {
            codePoints.add(i);
          }
        }
      });
      decoder.write(new byte[]{'\r'});
      assertEquals(1, codePoints.size());
      decoder.write(new byte[]{input[i]});
      assertEquals(Helper.list(expectedOutput[i]), codePoints);
    }
  }
}
