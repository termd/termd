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

import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
class EscapeFilter implements Consumer<Integer> {

  private EscStatus status = EscStatus.NORMAL;
  private final Escaper escaper;

  EscapeFilter(Escaper escaper) {
    this.escaper = escaper;
  }

  @Override
  public void accept(Integer code) {
    switch (status) {
      case NORMAL:
        switch ((int)code) {
          case '\'':
            escaper.beginQuotes('\'');
            status = EscStatus.IN_QUOTE;
            break;
          case '"':
            escaper.beginQuotes('\"');
            status = EscStatus.IN_DOUBLE_QUOTE;
            break;
          case '\\':
            escaper.escaping();
            status = EscStatus.IN_BACKSLASH;
            break;
          default:
            escaper.accept(code);
            break;
        }
        break;
      case IN_QUOTE:
        if (code == '\'') {
          escaper.endQuotes('\'');
          status = EscStatus.NORMAL;
        } else {
          escaper.accept(code);
        }
        break;
      case IN_DOUBLE_QUOTE:
        if (code == '"') {
          escaper.endQuotes('\"');
          status = EscStatus.NORMAL;
        } else {
          escaper.accept(code);
        }
        break;
      case IN_BACKSLASH:
        escaper.escaped(code);
        status = EscStatus.NORMAL;
        break;
      default:
        break;
    }
  }

  public static enum EscStatus {

    NORMAL, IN_QUOTE, IN_DOUBLE_QUOTE, IN_BACKSLASH

  }
}
