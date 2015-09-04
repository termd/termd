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

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Keymap {

  final Binding[] bindings;

  public Keymap() {
    this(Keys.values());
  }

  public Keymap(KeyEvent[] keys) {
    this.bindings = new Binding[keys.length];
    for (int i = 0;i < keys.length;i++) {
      bindings[i] = new Binding(keys[i]);
    }
  }

  /**
   * Create a new decoder configured from the <i>inputrc</i> configuration file.
   *
   * @param inputrc the configuration file
   */
  public Keymap(InputStream inputrc) {
    final ArrayList<Binding> actions = new ArrayList<>();
    InputrcParser handler = new InputrcParser() {
      @Override
      public void bindFunction(final int[] keySequence, final String functionName) {
        actions.add(new Binding(keySequence, new FunctionEvent(functionName, keySequence)));
      }
    };
    InputrcParser.parse(inputrc, handler);
    this.bindings = actions.toArray(new Binding[actions.size()]);
  }

  static class Binding {
    final int[] seq;
    final KeyEvent event;
    public Binding(KeyEvent event) {
      this.seq = new int[event.length()];
      for (int i = 0;i < seq.length;i++) {
        seq[i] = event.getAt(i);
      }
      this.event = event;
    }
    public Binding(int[] seq, KeyEvent event) {
      this.seq = seq;
      this.event = event;
    }
  }
}
