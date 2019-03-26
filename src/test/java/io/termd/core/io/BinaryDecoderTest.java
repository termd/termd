package io.termd.core.io;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class BinaryDecoderTest {

  @Test
  public void shouldNotFailOnInvalidUtf8Sequence() {
    List<String> hexes = new ArrayList<>();
    Consumer<int[]> onChar = ints -> {
      for (int i : ints) {
        String hex = Integer.toHexString(i);
        hexes.add(hex);
      }
    };
    BinaryDecoder binaryDecoder = new BinaryDecoder(UTF_8, onChar);
    byte[] bites = new byte[2];
    bites[0] = (byte) Integer.parseInt("c3", 16);
    bites[1] = (byte) Integer.parseInt("28", 16);
    binaryDecoder.write(bites);

    Assert.assertEquals("fffd", hexes.get(0));
  }
}
