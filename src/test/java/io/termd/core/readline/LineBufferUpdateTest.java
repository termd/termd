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

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class LineBufferUpdateTest {

  @Test
  public void testA() {
    TestTerminal screen = new TestTerminal();
    LineBuffer curr = new LineBuffer();
    curr.update(new LineBuffer().insert("abc"), screen, 20);
    screen.assertCodePoints("abc").assertEmpty();
    assertEquals("abc", curr.toString());
    assertEquals(3, curr.getCursor());
  }

  @Test
  public void testB() {
    TestTerminal screen = new TestTerminal();
    LineBuffer curr = new LineBuffer();
    curr.update(new LineBuffer().insert("abcdef"), screen, 4);
    screen.assertCodePoints("abcdef").assertEmpty();
    assertEquals("abcdef", curr.toString());
    assertEquals(6, curr.getCursor());
  }

  @Test
  public void testC() {
    TestTerminal screen = new TestTerminal();
    LineBuffer curr = new LineBuffer();
    curr.update(new LineBuffer().insert("abcd"), screen, 4);
    screen.assertCodePoints("abcd \r").assertEmpty();
    assertEquals("abcd", curr.toString());
    assertEquals(4, curr.getCursor());
  }

  @Test
  public void testD() {
    TestTerminal screen = new TestTerminal();
    LineBuffer curr = new LineBuffer().insert("abc");
    curr.update(new LineBuffer().insert("def"), screen, 20);
    screen.assertCodePoints("\rdef").assertEmpty();
    assertEquals("def", curr.toString());
    assertEquals(3, curr.getCursor());
  }

  @Test
  public void testE() {
    TestTerminal screen = new TestTerminal();
    LineBuffer curr = new LineBuffer().insert("abc");
    curr.update(new LineBuffer().insert("abdef"), screen, 20);
    screen.assertCodePoints("\bdef").assertEmpty();
    assertEquals("abdef", curr.toString());
    assertEquals(5, curr.getCursor());
  }

  @Test
  public void testF() {
    TestTerminal screen = new TestTerminal();
    LineBuffer curr = new LineBuffer().insert("a\nbc");
    curr.update(new LineBuffer().insert("abdef"), screen, 20);
    // TODO : improve that
    screen.assertCodePoints("\b\033[1Abdef\r\033[1B\033[K\033[1C\033[1C\033[1C\033[1C\033[1C\033[1A").assertEmpty();
  }

  @Test
  public void testG() {
    TestTerminal screen = new TestTerminal();
    LineBuffer curr = new LineBuffer().insert("abcde");
    curr.update(new LineBuffer().insert("aBcdEf"), screen, 20);
    screen.assertCodePoints("\b\b\b\bB\033[1C\033[1CEf").assertEmpty();
  }

  @Test
  public void testH() {
    TestTerminal screen = new TestTerminal();
    LineBuffer curr = new LineBuffer().insert("abcdefgh");
    curr.update(new LineBuffer().insert("aBcdefgHi"), screen, 4);
    screen.assertCodePoints("\033[1C\033[1A\033[1AB\033[1C\033[1BHi").assertEmpty();
  }

  @Test
  public void testI() {
    TestTerminal screen = new TestTerminal();
    LineBuffer curr = new LineBuffer().insert("abc");
    curr.update(new LineBuffer().insert(""), screen, 20);
    screen.assertCodePoints("\r\033[K").assertEmpty();
  }

  @Test
  public void testJ() {
    TestTerminal screen = new TestTerminal();
    LineBuffer curr = new LineBuffer().insert("abc");
    curr.update(new LineBuffer().insert("a\nbc"), screen, 20);
    screen.assertCodePoints("\b\b\033[K\nbc").assertEmpty();
  }

  @Test
  public void testL() {
    TestTerminal screen = new TestTerminal();
    LineBuffer curr = new LineBuffer().insert("ab\ncd");
    curr.update(new LineBuffer().insert("abcde"), screen, 4);
    screen.assertCodePoints("\033[1Acde\033[K").assertEmpty();
  }

  @Test
  public void testM() {
    TestTerminal screen = new TestTerminal();
    LineBuffer curr = new LineBuffer().insert("ab\ncd");
    curr.update(new LineBuffer().insert("abcd"), screen, 4);
    screen.assertCodePoints("\033[1Acd \r\033[K").assertEmpty();
  }

  @Test
  public void testN() {
    TestTerminal screen = new TestTerminal();
    LineBuffer curr = new LineBuffer().insert("ab\nef");
    curr.update(new LineBuffer().insert("abcdefg"), screen, 4);
    screen.assertCodePoints("\033[1Acd\033[1C\033[1Cg").assertEmpty();
  }

  @Test
  public void testO() {
    TestTerminal screen = new TestTerminal();
    LineBuffer curr = new LineBuffer().insert("a\nb");
    curr.update(new LineBuffer().insert("a\nbc"), screen, 20);
    screen.assertCodePoints("c").assertEmpty();
  }

  @Test
  public void testP() {
    TestTerminal screen = new TestTerminal();
    LineBuffer curr = new LineBuffer().insert("abc\nb");
    curr.update(new LineBuffer().insert("a\nbc"), screen, 20);
    screen.assertCodePoints("\033[1A\033[K\n\033[1Cc").assertEmpty();
  }

  @Test
  public void testQ() {
    TestTerminal screen = new TestTerminal();
    LineBuffer curr = new LineBuffer().insert("ab\ncd\nef\ngh");
    curr.update(new LineBuffer().insert("AbC"), screen, 20);
    screen.assertCodePoints("\r\033[1A\033[1A\033[1AA\033[1CC\r\033[1B\033[K"
            + "\033[1B\033[K\033[1B\033[K"
            + "\033[1C\033[1C"
            + "\033[1C\033[1A\033[1A\033[1A"
    ).assertEmpty();
  }

  @Test
  public void testR() {
    TestTerminal screen = new TestTerminal();
    LineBuffer curr = new LineBuffer().insert("");
    curr.update(new LineBuffer().insert("ab\n\ncd"), screen, 20);
    screen.assertCodePoints("ab\n\ncd");
  }

  @Test
  public void testS() {
    TestTerminal screen = new TestTerminal();
    LineBuffer curr = new LineBuffer().insert("abcdefg").setCursor(2);
    curr.update(new LineBuffer().insert("ab"), screen, 4);
    screen.assertCodePoints("\033[K\r\033[1B\033[K\033[1C\033[1C\033[1A").assertEmpty();
  }

  @Test
  public void testT() {
    TestTerminal screen = new TestTerminal();
    LineBuffer curr = new LineBuffer().insert("abcdefg").setCursor(3);
    curr.update(new LineBuffer().insert("abc"), screen, 4);
    screen.assertCodePoints("\033[K\r\033[1B\033[K\033[1C\033[1C\033[1C\033[1A").assertEmpty();
  }

  @Test
  public void testU() {
    TestTerminal screen = new TestTerminal();
    LineBuffer curr = new LineBuffer().insert("abcdefghijk").setCursor(2);
    curr.update(new LineBuffer().insert("ab"), screen, 4);
    // todo:
    // optimize \033[1C + \033[1C -> \033[2C
    // optimize \033[1A + \033[1A -> \033[2A
    screen.assertCodePoints("\033[K\r\033[1B\033[K\033[1B\033[K\033[1C\033[1C\033[1A\033[1A").assertEmpty();
  }
}
