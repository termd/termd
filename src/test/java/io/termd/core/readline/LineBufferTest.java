package io.termd.core.readline;

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
  public void deleteAt1() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', 'b', 'c', 'd');
    buffer.setCursor(2);
    assertEquals(-1, buffer.deleteAt(-1));
    assertEquals(3, buffer.getSize());
    assertEquals(1, buffer.getCursor());
    assertEquals("acd", buffer.toString());
  }

  @Test
  public void deleteAt2() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', 'b', 'c', 'd');
    buffer.setCursor(2);
    assertEquals(-2, buffer.deleteAt(-2));
    assertEquals(2, buffer.getSize());
    assertEquals(0, buffer.getCursor());
    assertEquals("cd", buffer.toString());
  }

  @Test
  public void deleteAt3() {
    LineBuffer buffer = new LineBuffer();
    buffer.insert('a', 'b', 'c', 'd');
    buffer.setCursor(2);
    assertEquals(-2, buffer.deleteAt(-3));
    assertEquals(2, buffer.getSize());
    assertEquals(0, buffer.getCursor());
    assertEquals("cd", buffer.toString());
  }
}
