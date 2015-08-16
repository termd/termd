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

package io.termd.core.tty;

import io.termd.core.util.Helper;
import org.junit.Test;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TtyOutputModeTest {

  @Test
  public void testTranslateLFToCRLF() {
    assertOutput("a", "a");
    assertOutput("\r\n", "\n");
    assertOutput("a\r\n", "a\n");
    assertOutput("\r\na", "\na");
    assertOutput("a\r\nb\r\nc", "a\nb\nc");
  }

  private void assertOutput(String expected, String actual) {
    TtyOutputMode abc = new TtyOutputMode();
    Stream.Builder<int[]> builder = Stream.<int[]>builder();
    abc.setReadHandler(builder);
    abc.accept(Helper.toCodePoints(actual));
    String result = Helper.fromCodePoints(builder.build().flatMapToInt(IntStream::of).toArray());
    assertEquals(expected, result);
  }
}
