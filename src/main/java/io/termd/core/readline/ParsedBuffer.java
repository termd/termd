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
class ParsedBuffer implements IntConsumer {

  public ParsedBuffer() {
    buffer = new LinkedList<>();

  }

  Quote quoting = Quote.NONE;
  boolean escaping = false;
  final LinkedList<Integer> buffer;

  public void accept(int codePoint) {
    switch (update(codePoint)) {
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

  private QuoteResult update(int code) {
    if (escaping) {
      escaping = false;
      return QuoteResult.FROM_ESC;
    } else {
      switch (quoting) {
        case NONE:
          switch (code) {
            case '\'':
              quoting = Quote.STRONG;
              return QuoteResult.TO_STRONG;
            case '"':
              quoting = Quote.WEAK;
              return QuoteResult.TO_WEAK;
            case '\\':
              escaping = true;
              return QuoteResult.TO_ESC;
            default:
              return QuoteResult.CODE_POINT;
          }
        case STRONG:
          if (code == '\'') {
            quoting = Quote.NONE;
            return QuoteResult.FROM_STRONG;
          } else {
            return QuoteResult.CODE_POINT;
          }
        case WEAK:
          if (code == '"') {
            quoting = Quote.NONE;
            return QuoteResult.FROM_WEAK;
          } else if (code == '\\') {
            // Note we don't make the distinction between special chars like " or \ from other chars
            // that are supposed to not escaped (i.e "\a" is \a and "\$" is $)
            // this interpretation is not done by termd
            escaping = true;
            return QuoteResult.TO_ESC;
          } else {
            return QuoteResult.CODE_POINT;
          }
        default:
          throw new AssertionError();
      }
    }
  }

  private enum QuoteResult {

    TO_STRONG, TO_WEAK, FROM_STRONG, FROM_WEAK, TO_ESC, CODE_POINT, FROM_ESC;

  }
}
