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

import org.junit.Assert;

import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
class TestTerminal implements Consumer<int[]> {

  final StringBuilder data = new StringBuilder();

  public TestTerminal() {
  }

  @Override
  public void accept(int[] codePoints) {
    for (int c : codePoints) {
      data.append(Character.toChars(c));
    }
  }

  public TestTerminal assertCodePoints(char... expected) {
    return assertCodePoints(new String(expected));
  }

  public TestTerminal assertCodePoints(String expected) {
    Assert.assertEquals(readable(expected), readable(data.substring(0, Math.min(expected.length(), data.length()))));
    data.delete(0, expected.length());
    return this;
  }

  public TestTerminal clear() {
    data.setLength(0);
    return this;
  }

  private static String readable(String s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0;i < s.length();i++) {
      char c = s.charAt(i);
      if (c < 32) {
        if (c == '\n') {
          sb.append("\\n");
        } else if (c == '\r') {
          sb.append("\\r");
        } else if (c == '\b') {
          sb.append("\\b");
        } else if (c == 27) {
          sb.append("\\033");
        } else {
          throw new UnsupportedOperationException();
        }
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  public TestTerminal assertEmpty() {
    Assert.assertEquals("", readable(data.toString()));
    return this;
  }
}
