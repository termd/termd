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

package io.termd.core.readline.functions;

import io.termd.core.readline.Function;
import io.termd.core.readline.LineBuffer;
import io.termd.core.readline.Readline;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class BackwardChar implements Function {

  @Override
  public String name() {
    return "backward-char";
  }

  @Override
  public void apply(Readline.Interaction interaction) {
    LineBuffer buf = interaction.buffer().copy();
    buf.moveCursor(-1);
    interaction.refresh(buf);
    interaction.resume();
  }
}
