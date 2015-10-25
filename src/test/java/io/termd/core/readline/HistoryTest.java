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

import io.termd.core.TestBase;
import io.termd.core.util.Helper;
import org.junit.Test;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class HistoryTest extends TestBase {

  @Test
  public void testHistory() {
    TestTerm term = new TestTerm(this);
    term.readline.getHistory().add(Helper.toCodePoints("abc"));
    term.readline.getHistory().add(Helper.toCodePoints("def"));
    term.readlineComplete();
    term.read(Keys.UP.sequence);
    term.assertScreen("% abc");
    term.assertAt(0, 5);
    term.read(Keys.UP.sequence);
    term.assertScreen("% def");
    term.assertAt(0, 5);
    term.read(Keys.UP.sequence);
    term.assertScreen("% def");
    term.assertAt(0, 5);
    term.read(Keys.DOWN.sequence);
    term.assertScreen("% abc");
    term.assertAt(0, 5);
    term.read(Keys.DOWN.sequence);
    term.assertScreen("% ");
    term.assertAt(0, 2);
    term.read(Keys.DOWN.sequence);
    term.assertScreen("% ");
    term.assertAt(0, 2);
  }

  @Test
  public void testMultiline() {
    TestTerm term = new TestTerm(this);
    term.readline.getHistory().add(Helper.toCodePoints("abc\ndef\nghi"));
    term.readlineComplete();
    term.read(Keys.UP.sequence);
    term.assertScreen("% abc", "def", "ghi");
    term.read(Keys.DOWN.sequence);
    term.assertScreen("% ", "", "");
  }

  @Test
  public void testEmptyLineMustNotBeAddedToHistory() {
    TestTerm term = new TestTerm(this);
    term.readlineComplete();
    term.read('\r');
    assertEquals(0, term.readline.getHistory().size());
  }
}
