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

package io.termd.core.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class WcwidthTest {

  @Test
  public void testWcwidth() {
    assertEquals(2, Wcwidth.of('한'));
    assertEquals(2, Wcwidth.of('글'));
    assertEquals(1, Wcwidth.of('A'));
    assertEquals(0, Wcwidth.of('\0'));
    assertEquals(-1, Wcwidth.of('\t'));
    assertEquals(0, Wcwidth.of('\u0301'));
    assertEquals(1, Wcwidth.of('\u09C0'));
  }
}
