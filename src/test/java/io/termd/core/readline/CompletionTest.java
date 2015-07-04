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

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class CompletionTest extends TestBase {

  @Test
  public void testCompletion() {
    TestTerm term = new TestTerm(this);
    term.readline(line -> {
    }, completion -> {
      assertEquals(0, completion.text().length);
      term.assertScreen("% ");
      term.assertAt(0, 2);
      testComplete();
    });
    term.read('\t');
    await();
  }

  @Test
  public void testEmptyCompletion() {
    TestTerm term = new TestTerm(this);
    AtomicBoolean completed = new AtomicBoolean();
    term.readline(line -> {
    }, completion -> {
      completion.complete(Collections.emptyList());
      completed.set(true);
    });
    term.read('\t');
    assertTrue(completed.get());
    term.assertScreen("% ");
    term.assertAt(0, 2);
  }

  @Test
  public void testEmptyCompletionAsync() throws Exception {
    TestTerm term = new TestTerm(this);
    CompletableFuture<Completion> completed = new CompletableFuture<>();
    term.readline(line -> {
    }, completed::complete);
    term.read('\t');
    Completion completion = completed.get();
    completion.complete(Collections.emptyList());
    term.assertScreen("% ");
    term.assertAt(0, 2);
  }

  @Test
  public void testSingleCompletion() {
    TestTerm term = new TestTerm(this);
    AtomicBoolean completed = new AtomicBoolean();
    term.readline(line -> {
    }, completion -> {
      completion.complete(Collections.singletonList(Helper.toCodePoints("abcdef")));
      completed.set(true);
    });
    term.read('\t');
    assertTrue(completed.get());
    term.assertScreen("% abcdef");
    term.assertAt(0, 8);
  }

  @Test
  public void testSingleCompletionAsync() throws Exception {
    TestTerm term = new TestTerm(this);
    CompletableFuture<Completion> completed = new CompletableFuture<>();
    term.readline(line -> {
    }, completed::complete);
    term.read('\t');
    Completion completion = completed.get();
    term.assertScreen("% ");
    term.assertAt(0, 2);
    completion.complete(Collections.singletonList(Helper.toCodePoints("abcdef")));
    term.assertScreen("% abcdef");
    term.assertAt(0, 8);
  }

  // Test send key event while blocked for completion !!!!!
  // Test multiple
  // Refactor to use Function !!!!
}
