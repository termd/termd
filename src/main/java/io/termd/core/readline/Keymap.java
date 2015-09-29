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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A keymap, binds key events to key sequence.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Keymap {

  public static Keymap getDefault() {
    InputStream inputrc = Keymap.class.getResourceAsStream("inputrc");
    return new Keymap(inputrc);
  }

  final List<KeyEvent> bindings;

  public Keymap() {
    this(Arrays.asList(Keys.values()));
  }

  public Keymap(List<KeyEvent> keys) {
    this.bindings = new ArrayList<>(keys);
  }

  /**
   * Create a new decoder configured from the <i>inputrc</i> configuration file.
   *
   * @param inputrc the configuration file
   */
  public Keymap(InputStream inputrc) {
    final ArrayList<KeyEvent> actions = new ArrayList<>();
    InputrcParser handler = new InputrcParser() {
      @Override
      public void bindFunction(final int[] keySequence, final String functionName) {
        actions.add(new FunctionEvent(functionName, keySequence));
      }
    };
    InputrcParser.parse(inputrc, handler);
    this.bindings = actions;
  }

  /**
   * Bind a function to a key sequence, the key seq must be in <i>inputrc</i> format.
   *
   * @param keyseq the key sequence
   * @param function the function to bind
   * @return this keymap
   */
  public Keymap bindFunction(String keyseq, String function) {
    return bindFunction(InputrcParser.parseKeySeq(keyseq), function);
  }

  /**
   * Bind a function to a key sequence, the key seq must be in <i>inputrc</i> format.
   *
   * @param keyseq the key sequence
   * @param function the function to bind
   * @return this keymap
   */
  public Keymap bindFunction(int[] keyseq, String function) {
    bindings.add(new FunctionEvent(function, keyseq));
    return this;
  }
}
