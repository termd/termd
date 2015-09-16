package io.termd.core.readline;

import io.termd.core.util.Vector;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class LineBufferTest {

  @Test
  public void testInsertAfter() {
    LineBuffer buffer = new LineBuffer();
    assertEquals("", buffer.toString());
    assertEquals(0, buffer.getCursor());
    assertEquals(0, buffer.getSize());
    buffer.insert('a');
    assertEquals("a", buffer.toString());
    assertEquals(1, buffer.getCursor());
    assertEquals(1, buffer.getSize());
    buffer.insert('b');
    assertEquals("ab", buffer.toString());
    assertEquals(2, buffer.getCursor());
    assertEquals(2, buffer.getSize());
  }

  @Test
  public void testInsertArrayAfter() {
    LineBuffer buffer = new LineBuffer();
    assertEquals("", buffer.toString());
    assertEquals(0, buffer.getCursor());
    assertEquals(0, buffer.getSize());
    buffer.insert('a', 'b');
    assertEquals("ab", buffer.toString());
    assertEquals(2, buffer.getCursor());
    assertEquals(2, buffer.getSize());
    buffer.insert('c', 'd');
    assertEquals("abcd", buffer.toString());
    assertEquals(4, buffer.getCursor());
    assertEquals(4, buffer.getSize());
  }

  @Test
  public void testMoveCursor() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', 'b', 'c', 'd');
    assertEquals(-1, buffer.moveCursor(-1));
    assertEquals(3, buffer.getCursor());
    assertEquals(-2, buffer.moveCursor(-2));
    assertEquals(1, buffer.getCursor());
    assertEquals(-1, buffer.moveCursor(-2));
    assertEquals(0, buffer.getCursor());
    assertEquals(1, buffer.moveCursor(1));
    assertEquals(1, buffer.getCursor());
    assertEquals(2, buffer.moveCursor(2));
    assertEquals(3, buffer.getCursor());
    assertEquals(1, buffer.moveCursor(2));
    assertEquals(4, buffer.getCursor());
  }

  @Test
  public void testSetCursor() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', 'b', 'c', 'd');
    buffer.setCursor(4);
    assertEquals(4, buffer.getCursor());
    buffer.setCursor(2);
    assertEquals(2, buffer.getCursor());
    buffer.setCursor(-1);
    assertEquals(0, buffer.getCursor());
    buffer.setCursor(1);
    assertEquals(1, buffer.getCursor());
    buffer.setCursor(0);
    assertEquals(0, buffer.getCursor());
    buffer.setCursor(5);
    assertEquals(4, buffer.getCursor());
  }

  @Test
  public void setSize() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', 'b', 'c', 'd');
    buffer.setCursor(2);
    buffer.setSize(3);
    assertEquals(3, buffer.getSize());
    assertEquals(2, buffer.getCursor());
    buffer.setSize(1);
    assertEquals(1, buffer.getSize());
    assertEquals(1, buffer.getCursor());
  }

  @Test
  public void testInsert() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', 'c');
    buffer.setCursor(1);
    buffer.insert('b');
    assertEquals("abc", buffer.toString());
    assertEquals(2, buffer.getCursor());
    assertEquals(3, buffer.getSize());
  }

  @Test
  public void testInsertArray() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', 'd');
    buffer.setCursor(1);
    buffer.insert('b', 'c');
    assertEquals("abcd", buffer.toString());
    assertEquals(3, buffer.getCursor());
    assertEquals(4, buffer.getSize());
  }

  @Test
  public void deleteAt1() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', 'b', 'c', 'd');
    buffer.setCursor(2);
    assertEquals(1, buffer.delete(-1));
    assertEquals(3, buffer.getSize());
    assertEquals(1, buffer.getCursor());
    assertEquals("acd", buffer.toString());
  }

  @Test
  public void deleteAt2() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', 'b', 'c', 'd');
    buffer.setCursor(2);
    assertEquals(2, buffer.delete(-2));
    assertEquals(2, buffer.getSize());
    assertEquals(0, buffer.getCursor());
    assertEquals("cd", buffer.toString());
  }

  @Test
  public void deleteAt3() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', 'b', 'c', 'd');
    buffer.setCursor(2);
    assertEquals(2, buffer.delete(-3));
    assertEquals(2, buffer.getSize());
    assertEquals(0, buffer.getCursor());
    assertEquals("cd", buffer.toString());
  }

  @Test
  public void deleteAt4() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', 'b', 'c', 'd');
    buffer.setCursor(2);
    assertEquals(1, buffer.delete(1));
    assertEquals(3, buffer.getSize());
    assertEquals(2, buffer.getCursor());
    assertEquals("abd", buffer.toString());
  }

  @Test
  public void deleteAt5() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', 'b', 'c', 'd');
    buffer.setCursor(2);
    assertEquals(2, buffer.delete(2));
    assertEquals(2, buffer.getSize());
    assertEquals(2, buffer.getCursor());
    assertEquals("ab", buffer.toString());
  }

  @Test
  public void deleteAt6() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', 'b', 'c', 'd');
    buffer.setCursor(2);
    assertEquals(2, buffer.delete(3));
    assertEquals(2, buffer.getSize());
    assertEquals(2, buffer.getCursor());
    assertEquals("ab", buffer.toString());
  }

  @Test
  public void testCursorPosition() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', 'b', 'c');
    assertEquals(new Vector(3, 0), buffer.getCursorPosition(4));
    assertEquals(new Vector(0, 1), buffer.getCursorPosition(3));
    assertEquals(new Vector(1, 1), buffer.getCursorPosition(2));
    assertEquals(new Vector(0, 3), buffer.getCursorPosition(1));
  }

  @Test
  public void testCursorPositionWithNewLine() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', 'b', '\n', 'c');
    assertEquals(new Vector(1, 1), buffer.getCursorPosition(4));
    assertEquals(new Vector(1, 1), buffer.getCursorPosition(3));
    assertEquals(new Vector(1, 2), buffer.getCursorPosition(2));
    assertEquals(new Vector(0, 4), buffer.getCursorPosition(1));
  }

  // @Test
  public void testCursorPositionWithCR() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', '\r', 'b', 'c');
    assertEquals(new Vector(2, 0), buffer.getCursorPosition(4));
    assertEquals(new Vector(2, 0), buffer.getCursorPosition(3));
    assertEquals(new Vector(0, 1), buffer.getCursorPosition(2));
    assertEquals(new Vector(0, 3), buffer.getCursorPosition(1));
  }

  // @Test
  public void testCursorControlChar() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', '\t', 'c');
    assertEquals(new Vector(2, 0), buffer.getCursorPosition(4));
    assertEquals(new Vector(2, 0), buffer.getCursorPosition(3));
    assertEquals(new Vector(0, 1), buffer.getCursorPosition(2));
    assertEquals(new Vector(0, 2), buffer.getCursorPosition(1));
  }

  // @Test
  public void testCursorInvisibleChar() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', '\0', 'c');
    assertEquals(new Vector(2, 0), buffer.getCursorPosition(4));
    assertEquals(new Vector(2, 0), buffer.getCursorPosition(3));
    assertEquals(new Vector(0, 1), buffer.getCursorPosition(2));
    assertEquals(new Vector(0, 2), buffer.getCursorPosition(1));
  }

  // @Test
  public void testCursorPositionWithMultiCell1() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('한', 'b');
    assertEquals(new Vector(3, 0), buffer.getCursorPosition(4));
    assertEquals(new Vector(0, 1), buffer.getCursorPosition(3));
    assertEquals(new Vector(1, 1), buffer.getCursorPosition(2));
    try {
      buffer.getCursorPosition(1);
      fail();
    } catch (UnsupportedOperationException ignore) {
    }
  }

  // @Test
  public void testCursorPositionWithMultiCell2() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', '한');
    assertEquals(new Vector(3, 0), buffer.getCursorPosition(4));
    assertEquals(new Vector(0, 1), buffer.getCursorPosition(3));
    assertEquals(new Vector(0, 2), buffer.getCursorPosition(2));
    try {
      buffer.getCursorPosition(1);
      fail();
    } catch (UnsupportedOperationException ignore) {
    }
  }

  @Test
  public void testUpdate1() {
    TestTerminal screen = new TestTerminal();
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a');
    LineBuffer to = new LineBuffer();
    to.insert('a');
    buffer.update(to, screen, 40);
    screen.assertEmpty();
  }

  @Test
  public void testUpdate2() {
    TestTerminal screen = new TestTerminal();
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a');
    LineBuffer to = new LineBuffer();
    to.insert('b');
    buffer.update(to, screen, 40);
    screen.assertCodePoints("\rb").assertEmpty();
  }

  @Test
  public void testUpdate3() {
    TestTerminal screen = new TestTerminal();
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', 'b');
    LineBuffer to = new LineBuffer();
    to.insert('a', 'c');
    buffer.update(to, screen, 40);
    screen.assertCodePoints("\bc").assertEmpty();
  }

  @Test
  public void testUpdate4() {
    TestTerminal screen = new TestTerminal();
    LineBuffer buffer = new LineBuffer();
    LineBuffer to = new LineBuffer();
    to.insert('a');
    buffer.update(to, screen, 40);
    screen.assertCodePoints("a").assertEmpty();
  }

  @Test
  public void testUpdate5() {
    TestTerminal screen = new TestTerminal();
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a');
    LineBuffer to = new LineBuffer();
    buffer.update(to, screen, 40);
    screen.assertCodePoints("\r\033[K").assertEmpty();
  }

  @Test
  public void testUpdate6() {
    TestTerminal screen = new TestTerminal();
    LineBuffer buffer = new LineBuffer();
    LineBuffer to = new LineBuffer();
    to.insert('a', '\n', 'b');
    buffer.update(to, screen, 40);
    screen.assertCodePoints("a\nb").assertEmpty();
  }

  @Test
  public void testUpdate7() {
    TestTerminal screen = new TestTerminal();
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', '\n', 'b');
    LineBuffer to = new LineBuffer();
    to.insert('d');
    buffer.update(to, screen, 40);
    screen.assertCodePoints("\r\033[1Ad\r\033[1B\033[K\033[1C\033[1A").assertEmpty();
  }

  @Test
  public void testUpdate8() {
    TestTerminal screen = new TestTerminal();
    LineBuffer buffer = new LineBuffer();
    LineBuffer to = new LineBuffer();
    to.insert('a', 'b', 'c', 'd', 'e');
    buffer.update(to, screen, 2);
    screen.assertCodePoints("abcde").assertEmpty();
  }
}
