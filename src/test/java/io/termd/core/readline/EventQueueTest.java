/*
 * Copyright 2015 Julien Viet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.termd.core.readline;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.IntBuffer;
import java.nio.ReadOnlyBufferException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class EventQueueTest {

  @Test
  public void testDecodeKeySeq() {
    EventQueue queue = new EventQueue(new Keymap(new ByteArrayInputStream("\"ab\":foo".getBytes())));
    assertFalse(queue.append('a').hasNext());
    assertTrue(queue.append('b', 'c').hasNext());
    FunctionEvent action = (FunctionEvent) queue.next();
    assertEquals("foo", action.name());
    assertTrue(queue.hasNext());
    KeyEvent key = (KeyEvent) queue.next();
    assertEquals(1, key.length());
    assertEquals('c', key.getCodePointAt(0));
    assertFalse(queue.hasNext());
  }

  @Test
  public void testDecodeKeySeqPrefix() {
    EventQueue queue = new EventQueue(new Keymap(new ByteArrayInputStream("\"ab\":foo".getBytes())));
    assertFalse(queue.append('a').hasNext());
    assertTrue(queue.append('c').hasNext());
    KeyEvent key = (KeyEvent) queue.next();
    assertEquals(1, key.length());
    assertEquals('a', key.getCodePointAt(0));
    assertTrue(queue.hasNext());
    key = (KeyEvent) queue.next();
    assertEquals(1, key.length());
    assertEquals('c', key.getCodePointAt(0));
    assertFalse(queue.hasNext());
  }

  @Test
  public void testRecognizePredefinedKey1() {
    EventQueue queue = new EventQueue(new Keymap());
    queue.append(27, 91);
    assertTrue(queue.hasNext());
    assertEquals(1, ((KeyEvent) queue.peek()).length());
    assertEquals(27, ((KeyEvent) queue.peek()).getCodePointAt(0));
    queue.append(65);
    assertTrue(queue.hasNext());
    assertEquals(Keys.UP, queue.next());
  }

  @Test
  public void testRecognizePredefinedKey2() {
    EventQueue queue = new EventQueue(new Keymap());
    queue.append(27, 91);
    assertEquals(1, ((KeyEvent) queue.peek()).length());
    assertEquals(27, ((KeyEvent)queue.peek()).getCodePointAt(0));
    assertTrue(queue.hasNext());
    queue.append(66);
    assertTrue(queue.hasNext());
    assertEquals(Keys.DOWN, queue.next());
  }

  @Test
  public void testNotRecognizePredefinedKey() {
    EventQueue queue = new EventQueue(new Keymap());
    queue.append('a');
    assertTrue(queue.hasNext());
    KeyEvent key = (KeyEvent) queue.next();
    assertEquals(1, key.length());
    assertEquals('a', key.getCodePointAt(0));
  }

  @Test
  public void testBuffer() {
    EventQueue queue = new EventQueue(new Keymap());
    assertEquals(0, queue.getBuffer().capacity());
    queue.append('h', 'e', 'l', 'l', 'o');
    IntBuffer buffer = queue.getBuffer();
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
