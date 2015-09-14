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

import io.termd.core.telnet.TestBase;
import io.termd.core.util.Helper;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class CompletionTest extends TestBase {

  @Test
  public void testCompletion() {
    TestTerm term = new TestTerm(this);
    term.readline(line -> {
    }, completion -> {
      assertEquals(0, completion.prefix().length);
      term.assertScreen("% ");
      term.assertAt(0, 2);
      testComplete();
    });
    term.read('\t');
    await();
  }

  @Test
  public void testIllegalCompletion() {
    TestTerm term = new TestTerm(this);
    AtomicReference<Completion> completion = new AtomicReference<>();
    term.readline(line -> {
    }, completion::set);
    term.read('a');
    term.read('\t');
    completion.get().suggest(new int[]{'b'});
    try {
      completion.get().suggest(new int[]{'c'});
      fail("Was expecting an IllegalStateException");
    } catch (IllegalStateException ignore) {
    }
  }

  @Test
  public void testEmptyCompletion() {
    TestTerm term = new TestTerm(this);
    AtomicBoolean completed = new AtomicBoolean();
    Supplier<String> line = term.readlineComplete(completion -> {
      completion.end();
      completed.set(true);
    });
    term.read('\t');
    assertTrue(completed.get());
    term.assertScreen("% ");
    term.assertAt(0, 2);
    term.read('a');
    term.assertScreen("% a");
    term.assertAt(0, 3);
    term.read('\r');
    term.assertScreen("% a");
    term.assertAt(1, 0);
    assertEquals("a", line.get());
  }

  @Test
  public void testEmptyCompletionAsync() throws Exception {
    TestTerm term = new TestTerm(this);
    CompletableFuture<Completion> completed = new CompletableFuture<>();
    Supplier<String> line = term.readlineComplete(completed::complete);
    term.read('\t');
    term.assertScreen("% ");
    term.assertAt(0, 2);
    term.read('a');
    term.assertScreen("% ");
    term.assertAt(0, 2);
    Completion completion = completed.get();
    completion.end();
    term.executeTasks();
    term.assertScreen("% a");
    term.assertAt(0, 3);
    term.read('\r');
    term.assertScreen("% a");
    term.assertAt(1, 0);
    assertEquals("a", line.get());
  }

  @Test
  public void testSingleTerminalCompletion() {
    TestTerm term = new TestTerm(this);
    AtomicBoolean completed = new AtomicBoolean();
    Supplier<String> line = term.readlineComplete(completion -> {
      completion.complete(new int[]{'b','c','d','e','f'}, true);
      completed.set(true);
    });
    term.read('a');
    term.assertScreen("% a");
    term.assertAt(0, 3);
    term.read('\t');
    assertTrue(completed.get());
    term.assertScreen("% abcdef ");
    term.assertAt(0, 9);
    term.read('g');
    term.assertScreen("% abcdef g");
    term.assertAt(0, 10);
    term.read('\r');
    term.assertScreen("% abcdef g");
    term.assertAt(1, 0);
    assertEquals("abcdef g", line.get());
  }

  @Test
  public void testSingleTerminalCompletionAsync() throws Exception {
    TestTerm term = new TestTerm(this);
    CompletableFuture<Completion> completed = new CompletableFuture<>();
    Supplier<String> line = term.readlineComplete(completed::complete);
    term.read('a');
    term.assertScreen("% a");
    term.assertAt(0, 3);
    term.read('\t');
    Completion completion = completed.get();
    term.assertScreen("% a");
    term.assertAt(0, 3);
    term.read('g');
    term.assertScreen("% a");
    term.assertAt(0, 3);
    completion.complete(new int[]{'b', 'c', 'd', 'e', 'f'}, true);
    term.executeTasks();
    term.assertScreen("% abcdef g");
    term.assertAt(0, 10);
    term.read('h');
    term.assertScreen("% abcdef gh");
    term.assertAt(0, 11);
    term.read('\r');
    term.assertScreen("% abcdef gh");
    term.assertAt(1, 0);
    assertEquals("abcdef gh", line.get());
  }

  @Test
  public void testSingleNonTerminalCompletion() {
    TestTerm term = new TestTerm(this);
    AtomicBoolean completed = new AtomicBoolean();
    Supplier<String> line = term.readlineComplete(completion -> {
      completion.complete(new int[]{'b','c','d','e','f'});
      completed.set(true);
    });
    term.read('a');
    term.assertScreen("% a");
    term.assertAt(0, 3);
    term.read('\t');
    assertTrue(completed.get());
    term.assertScreen("% abcdef");
    term.assertAt(0, 8);
    term.read('g');
    term.assertScreen("% abcdefg");
    term.assertAt(0, 9);
    term.read('\r');
    term.assertScreen("% abcdefg");
    term.assertAt(1, 0);
    assertEquals("abcdefg", line.get());
  }

  @Test
  public void testSingleEmptyCompletion() {
    TestTerm term = new TestTerm(this);
    AtomicBoolean completed = new AtomicBoolean();
    Supplier<String> line = term.readlineComplete(completion -> {
      completion.complete(new int[0], true);
      completed.set(true);
    });
    term.read('a', 'b');
    term.assertScreen("% ab");
    term.assertAt(0, 4);
    term.read('\t');
    assertTrue(completed.get());
    term.assertScreen("% ab ");
    term.assertAt(0, 5);
    term.read('\r');
    term.assertScreen("% ab ");
    term.assertAt(1, 0);
    assertEquals("ab ", line.get());
  }

  @Test
  public void testNoCommonPrefixCompletion() {
    TestTerm term = new TestTerm(this);
    AtomicBoolean completed = new AtomicBoolean();
    Supplier<String> line = term.readlineComplete(completion -> {
      completion.suggest(Arrays.asList(
          new int[]{'f','o','o','a'},
          new int[]{'f','o','o','b'},
          new int[]{'f','o','o','c'}
      ));
      completed.set(true);
    });
    term.read('f', 'o', 'o');
    term.assertScreen("% foo");
    term.assertAt(0, 5);
    term.read('\t');
    assertTrue(completed.get());
    term.assertScreen("% foo", "fooa foob fooc ", "% foo");
    term.assertAt(2, 5);
    term.read('g');
    term.assertScreen("% foo", "fooa foob fooc ", "% foog");
    term.assertAt(2, 6);
    term.read('\r');
    term.assertScreen("% foo", "fooa foob fooc ", "% foog");
    term.assertAt(3, 0);
    assertEquals("foog", line.get());
  }

  @Test
  public void testCompletionBlock1() throws Exception {
    TestTerm term = new TestTerm(this);
    AtomicBoolean completed = new AtomicBoolean();
    Supplier<String> line = term.readlineComplete(completion -> {
      completion.suggest(Helper.toCodePoints("a\r\nb\r\nc\r\n"));
      completed.set(true);
    });
    term.read('a', 'b');
    term.read(BACKWARD_KEY);
    term.assertScreen("% ab");
    term.assertAt(0, 3);
    term.read('\t');
    assertTrue(completed.get());
    term.assertScreen("% ab", "a", "b", "c", "% ab");
    term.assertAt(4, 3);
    term.read('c');
    term.assertScreen("% ab", "a", "b", "c", "% acb");
    term.assertAt(4, 4);
    term.read('\r');
    term.assertScreen("% ab", "a", "b", "c", "% acb");
    term.assertAt(5, 0);
    assertEquals("acb", line.get());
  }

  @Test
  public void testCompletionBlock2() throws Exception {
    TestTerm term = new TestTerm(this);
    AtomicBoolean completed = new AtomicBoolean();
    Supplier<String> line = term.readlineComplete(completion -> {
      completion.suggest(Helper.toCodePoints("a\r\nb\r\nc\r\n"));
      completed.set(true);
    });
    term.read('a', '\\', '\r', 'b', 'c');
    term.read(BACKWARD_KEY);
    term.assertScreen("% a\\", "> bc");
    term.assertAt(1, 3);
    term.read('\t');
    assertTrue(completed.get());
    term.assertScreen("% a\\", "> bc", "a", "b", "c", "> bc");
    term.assertAt(5, 3);
    term.read('d');
    term.assertScreen("% a\\", "> bc", "a", "b", "c", "> bdc");
    term.assertAt(5, 4);
    term.read('\r');
    term.assertScreen("% a\\", "> bc", "a", "b", "c", "> bdc");
    term.assertAt(6, 0);
    assertEquals("abdc", line.get());
  }

  @Test
  public void testCompletionBlock3() throws Exception {
    TestTerm term = new TestTerm(this);
    AtomicBoolean completed = new AtomicBoolean();
    Supplier<String> line = term.readlineComplete(completion -> {
      completion.suggest(Helper.toCodePoints("a\r\nb\r\nc\r\n"));
      completed.set(true);
    });
    term.read('a', '"', '\r', 'b', 'c');
    term.read(BACKWARD_KEY);
    term.assertScreen("% a\"", "> bc");
    term.assertAt(1, 3);
    term.read('\t');
    assertTrue(completed.get());
    term.assertScreen("% a\"", "> bc", "a", "b", "c", "> bc");
    term.assertAt(5, 3);
    term.read('d');
    term.assertScreen("% a\"", "> bc", "a", "b", "c", "> bdc");
    term.assertAt(5, 4);
    term.read('"', '\r');
    term.assertScreen("% a\"", "> bc", "a", "b", "c", "> bd\"c");
    term.assertAt(6, 0);
    assertEquals("a\"\nbd\"c", line.get());
  }

  @Test
  public void testEscape() throws Exception {
    assertPrefix("\\", "\\");
    assertPrefix("\"", "\"");
    assertPrefix("'", "'");
    assertPrefix("\\a", "\\a");
    assertPrefix("\"a", "\"a");
    assertPrefix("'a", "'a");
  }

  @Test
  public void testCompleteEscape() throws Exception {
    assertCompleteInline("", "a", "% a");
    assertCompleteInline("", "a", true, "% a ");

    assertCompleteInline("", " ", "% \\ ");
    assertCompleteInline("", " ", true, "% \\  ");
    assertCompleteInline("", "\\", "% \\\\");
    assertCompleteInline("", "\\", true, "% \\\\ ");
    assertCompleteInline("", "'", "% \\'");
    assertCompleteInline("", "'", true, "% \\' ");
    assertCompleteInline("", "\"", "% \\\"");
    assertCompleteInline("", "\"", true, "% \\\" ");

    assertCompleteInline("\"", " ", "% \" ");
    assertCompleteInline("\"", " ", true, "% \" \" ");
    assertCompleteInline("\"", "\\", "% \"\\\\");
    assertCompleteInline("\"", "\\", true, "% \"\\\\\" ");
    assertCompleteInline("\"", "'", "% \"'");
    assertCompleteInline("\"", "'", true, "% \"'\" ");
    assertCompleteInline("\"", "\"", "% \"\\\"");
    assertCompleteInline("\"", "\"", true, "% \"\\\"\" ");

    assertCompleteInline("\"\\", "\"", "% \"\\\"");
    assertCompleteInline("\"\\", "\"", true, "% \"\\\"\" ");
    assertCompleteInline("\"\\", "\\", "% \"\\\\");
    assertCompleteInline("\"\\", "\\", true, "% \"\\\\\" ");
    assertCompleteInline("\"\\", "a", "% \"\\");
    assertCompleteInline("\"\\", "a", true, "% \"\\");

    assertCompleteInline("'", " ", "% ' ");
    assertCompleteInline("'", " ", true, "% ' ' ");
    assertCompleteInline("'", "\\", "% '\\");
    assertCompleteInline("'", "\\", true, "% '\\' ");
    assertCompleteInline("'", "'", "% ''\\''");
    assertCompleteInline("'", "'", true, "% ''\\''' ");
    assertCompleteInline("'", "\"", "% '\"");
    assertCompleteInline("'", "\"", true, "% '\"' ");

    assertCompleteInline("\\", "a", "% \\a");
    assertCompleteInline("\\", "a", true, "% \\a ");
  }

  private void assertPrefix(String line, String expected) {
    TestTerm term = new TestTerm(this);
    AtomicReference<String> prefix = new AtomicReference<>();
    term.readlineComplete(comp -> {
      prefix.set(Helper.fromCodePoints(comp.prefix()));
    });
    term.read(Helper.toCodePoints(line));
    term.read('\t');
    assertEquals(expected, prefix.get());
  }

  private void assertCompleteInline(String line, String inline, String... expected) {
    assertCompleteInline(line, inline, false, expected);
  }

  private void assertCompleteInline(String line, String inline, boolean terminate, String... expected) {
    TestTerm term = new TestTerm(this);
    AtomicReference<Completion> completion = new AtomicReference<>();
    term.readlineComplete(completion::set);
    term.read(Helper.toCodePoints(line));
    term.read('\t');
    completion.get().complete(Helper.toCodePoints(inline), terminate);
    term.assertScreen(expected);
  }
}
