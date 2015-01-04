package io.termd.core.readline;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.IntBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class EventMapperTest {

  @Test
  public void testDecodeKeySeq() {
    EventMapper eventMapper = new EventMapper(new ByteArrayInputStream("\"ab\":foo".getBytes()));
    eventMapper.append('a').reduce();
    assertEquals(0, eventMapper.getEvents().size());
    eventMapper.append('b', 'c').reduce();
    assertEquals(2, eventMapper.getEvents().size());
    FunctionEvent action = (FunctionEvent) eventMapper.getEvents().get(0);
    assertEquals("foo", action.getName());
    KeyEvent key = (KeyEvent) eventMapper.getEvents().get(1);
    assertEquals(1, key.length());
    assertEquals('c', key.getAt(0));
  }

  @Test
  public void testDecodeKeySeqPrefix() {
    EventMapper eventMapper = new EventMapper(new ByteArrayInputStream("\"ab\":foo".getBytes()));
    eventMapper.append('a').reduce();
    assertEquals(0, eventMapper.getEvents().size());
    eventMapper.append('c').reduce();
    assertEquals(2, eventMapper.getEvents().size());
    KeyEvent key = (KeyEvent) eventMapper.getEvents().get(0);
    assertEquals(1, key.length());
    assertEquals('a', key.getAt(0));
    key = (KeyEvent) eventMapper.getEvents().get(1);
    assertEquals(1, key.length());
    assertEquals('c', key.getAt(0));
  }

  @Test
  public void testRecognizePredefinedKey1() {
    EventMapper eventMapper = new EventMapper();
    eventMapper.append(27, 91, 65);
    eventMapper.append(65);
    eventMapper.reduceOnce();
    assertEquals(1, eventMapper.getEvents().size());
    assertEquals(Collections.<Event>singletonList(Keys.UP), eventMapper.getEvents());
  }

  @Test
  public void testRecognizePredefinedKey2() {
    EventMapper eventMapper = new EventMapper();
    eventMapper.append(27, 91);
    eventMapper.append(66);
    eventMapper.reduceOnce();
    assertEquals(1, eventMapper.getEvents().size());
    assertEquals(Collections.<Event>singletonList(Keys.DOWN), eventMapper.getEvents());
  }

  @Test
  public void testNotRecognizePredefinedKey() {
    EventMapper eventMapper = new EventMapper();
    eventMapper.append('a');
    eventMapper.reduceOnce();
    assertEquals(1, eventMapper.getEvents().size());
    KeyEvent key = (KeyEvent) eventMapper.getEvents().get(0);
    assertEquals(1, key.length());
    assertEquals('a', key.getAt(0));
  }

  @Test
  public void testDouble() {
    EventMapper eventMapper = new EventMapper();
    eventMapper.append(27);
    eventMapper.append(65);
    eventMapper.reduceOnce();
    eventMapper.reduceOnce();
    assertEquals(2, eventMapper.getEvents().size());
  }

  @Test
  public void testHasEvents() {
    EventMapper eventMapper = new EventMapper();
    assertFalse(eventMapper.hasEvents());
    eventMapper.append(65, 66);
    assertFalse(eventMapper.hasEvents());
    eventMapper.reduceOnce();
    assertTrue(eventMapper.hasEvents());
    eventMapper.popEvent();
    assertFalse(eventMapper.hasEvents());
    eventMapper.reduceOnce();
    assertTrue(eventMapper.hasEvents());
    eventMapper.popEvent();
    assertFalse(eventMapper.hasEvents());
    eventMapper.reduceOnce();
    assertFalse(eventMapper.hasEvents());
  }

  @Test
  public void testBuffer() {
    EventMapper eventMapper = new EventMapper();
    assertEquals(0, eventMapper.getBuffer().capacity());
    eventMapper.append('h', 'e', 'l', 'l', 'o');
    IntBuffer buffer = eventMapper.getBuffer();
    buffer.mark();
    assertEquals(5, buffer.capacity());
    assertEquals('h', buffer.get());
    assertEquals('e', buffer.get());
    assertEquals('l', buffer.get());
    assertEquals('l', buffer.get());
    assertEquals('o', buffer.get());
    buffer.reset();
    try {
      buffer.put(0, 'p');
      fail();
    } catch (ReadOnlyBufferException ignore) {
    }
  }
}
