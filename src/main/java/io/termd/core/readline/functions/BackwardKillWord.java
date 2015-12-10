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
 * <i>Kill the word behind point. Word boundaries are the same as backward-word.</i>
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class BackwardKillWord implements Function {

  @Override
  public String name() {
    return "backward-kill-word";
  }

  @Override
  public void apply(Readline.Interaction interaction) {
    LineBuffer buf = interaction.buffer().copy();
    int cursor = BackwardWord.findPos(buf);
    buf.delete(cursor - buf.getCursor());
    interaction.refresh(buf);
    interaction.resume();
  }
}
