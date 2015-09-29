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

import io.termd.core.util.Helper;

import java.util.ArrayList;
import java.util.List;

/**
 * A readline function.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Function {

  /**
   * Load the defaults function via the {@link java.util.ServiceLoader} SPI.
   *
   * @return the loaded function
   */
  static List<Function> loadDefaults() {
    List<Function> functions = new ArrayList<>();
    for (io.termd.core.readline.Function function : Helper.loadServices(Thread.currentThread().getContextClassLoader(), io.termd.core.readline.Function.class)) {
      functions.add(function);
    }
    return functions;
  }

  /**
   * The function name, for instance <i>backward-char</i>.
   */
  String name();

  /**
   * Apply the function on the current interaction.
   *
   * @param interaction the current interaction
   */
  void apply(Readline.Interaction interaction);

}
