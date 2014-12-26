package io.modsh.core.readline;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReaderTest {

  @Test
  public void testDecodeKeySeq() {
    Reader reader = new Reader(new ByteArrayInputStream("\"ab\":foo".getBytes()));
    reader.append('a').reduce();
    assertEquals(0, reader.getActions().size());
    reader.append('b', 'c').reduce();
    assertEquals(2, reader.getActions().size());
    FunctionAction action = (FunctionAction) reader.getActions().get(0);
    assertEquals("foo", action.getName());
    KeyAction key = (KeyAction) reader.getActions().get(1);
    assertEquals(1, key.length());
    assertEquals('c', key.getAt(0));
  }

  @Test
  public void testDecodeKeySeqPrefix() {
    Reader reader = new Reader(new ByteArrayInputStream("\"ab\":foo".getBytes()));
    reader.append('a').reduce();
    assertEquals(0, reader.getActions().size());
    reader.append('c').reduce();
    assertEquals(2, reader.getActions().size());
    KeyAction key = (KeyAction) reader.getActions().get(0);
    assertEquals(1, key.length());
    assertEquals('a', key.getAt(0));
    key = (KeyAction) reader.getActions().get(1);
    assertEquals(1, key.length());
    assertEquals('c', key.getAt(0));
  }

  @Test
  public void testRecognizePredefinedKey1() {
    Reader reader = new Reader();
    reader.append(27, 91, 65);
    reader.append(65);
    reader.reduceOnce();
    assertEquals(1, reader.getActions().size());
    assertEquals(Collections.<Action>singletonList(Keys.UP), reader.getActions());
  }

  @Test
  public void testRecognizePredefinedKey2() {
    Reader reader = new Reader();
    reader.append(27, 91);
    reader.append(66);
    reader.reduceOnce();
    assertEquals(1, reader.getActions().size());
    assertEquals(Collections.<Action>singletonList(Keys.DOWN), reader.getActions());
  }

  @Test
  public void testNotRecognizePredefinedKey() {
    Reader reader = new Reader();
    reader.append('a');
    reader.reduceOnce();
    assertEquals(1, reader.getActions().size());
    assertEquals(1, reader.getActions().get(0).length());
    assertEquals('a', reader.getActions().get(0).getAt(0));
  }

  @Test
  public void testDouble() {
    Reader reader = new Reader();
    reader.append(27);
    reader.append(65);
    reader.reduceOnce();
    reader.reduceOnce();
    assertEquals(2, reader.getActions().size());
  }
}
