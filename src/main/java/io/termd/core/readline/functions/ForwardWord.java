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
 * <i>Move forward to the end of the next word. Words are composed of letters and digits.</i>
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ForwardWord implements Function {

  @Override
  public String name() {
    return "forward-word";
  }

  @Override
  public void apply(Readline.Interaction interaction) {
    LineBuffer buf = interaction.buffer().copy();
    int size = buf.getSize();
    int next;
    while ((next = buf.getCursor()) < size) {
      int codePoint = buf.getAt(next);
      if (Character.isLetterOrDigit(codePoint)) {
        break;
      } else {
        buf.moveCursor(1);
      }
    }
    while ((next = buf.getCursor()) < size) {
      int codePoint = buf.getAt(next);
      if (Character.isLetterOrDigit(codePoint)) {
        buf.moveCursor(1);
      } else {
        break;
      }
    }
    interaction.refresh(buf);
    interaction.resume();
  }
}
