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

import java.util.LinkedList;
import java.util.function.IntConsumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class LineStatus implements IntConsumer {

  // Keeping this internal to this package
  static class Ext extends LineStatus implements IntConsumer {
    final LinkedList<Integer> buffer;
    Ext() {
      this.buffer = new LinkedList<>();
    }
    public void accept(int codePoint) {
      super.accept(codePoint);
      switch (transition) {
        case TO_WEAK:
          buffer.add((int) '"');
          break;
        case TO_STRONG:
          buffer.add((int) '\'');
          break;
        case FROM_WEAK:
          buffer.add((int) '"');
          break;
        case FROM_STRONG:
          buffer.add((int) '\'');
          break;
        case TO_ESC:
          buffer.add((int)'\\');
          break;
        case FROM_ESC:
          if (codePoint != '\r') {
            buffer.add(codePoint);
          } else {
            buffer.removeLast();
          }
          break;
        case CODE_POINT:
          buffer.add(codePoint);
          break;
      }
    }
  }

  protected int quote = 0;
  protected Transition transition;

  public boolean isEscaped() {
    return transition == Transition.FROM_ESC;
  }

  public boolean isEscaping() {
    return transition == Transition.TO_ESC;
  }

  /**
   * @return true if it's currently quoted
   */
  public boolean isQuoted() {
    return quote != 0;
  }

  public boolean isWeaklyQuoted() {
    return quote == '"';
  }

  public boolean isStronglyQuoted() {
    return quote == '\'';
  }

  /**
   * @return the current quote: {@code 0}, {@code '} or {@code "} value
   */
  public int getQuote() {
    return quote;
  }

  public boolean isCodePoint() {
    return transition == Transition.CODE_POINT || transition == Transition.FROM_ESC;
  }

  @Override
  public void accept(int cp) {
    Transition next;
    if (transition == Transition.TO_ESC) {
      next = Transition.FROM_ESC;
    } else {
      switch (quote) {
        case 0:
          switch (cp) {
            case '\'':
              quote = '\'';
              next =  Transition.TO_STRONG;
              break;
            case '"':
              quote = '"';
              next =  Transition.TO_WEAK;
              break;
            case '\\':
              next =  Transition.TO_ESC;
              break;
            default:
              next =  Transition.CODE_POINT;
              break;
          }
          break;
        case '\'':
          if (cp == '\'') {
            quote = 0;
            next =  Transition.FROM_STRONG;
          } else {
            next =  Transition.CODE_POINT;
          }
          break;
        case '"':
          if (cp == '"') {
            quote = 0;
            next =  Transition.FROM_WEAK;
          } else if (cp == '\\') {
            // Note we don't make the distinction between special chars like " or \ from other chars
            // that are supposed to not escaped (i.e "\a" is \a and "\$" is $)
            // this interpretation is not done by termd
            next =  Transition.TO_ESC;
          } else {
            next =  Transition.CODE_POINT;
          }
          break;
        default:
          throw new AssertionError();
      }
    }
    this.transition = next;
  }

  private enum Transition {

    TO_STRONG, TO_WEAK, FROM_STRONG, FROM_WEAK, TO_ESC, CODE_POINT, FROM_ESC;

  }
}
