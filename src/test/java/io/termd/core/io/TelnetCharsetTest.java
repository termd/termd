package io.termd.core.io;

import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetCharsetTest {

  @Test
  public void testFoo() {
    Charset cs = new TelnetCharset();
    assertEquals("A", cs.decode(ByteBuffer.wrap(new byte[]{65})).toString());


  }

}
