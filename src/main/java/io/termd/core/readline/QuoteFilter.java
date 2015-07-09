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

import java.util.function.IntConsumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
class QuoteFilter implements IntConsumer {

  private Quoting status = Quoting.NONE;
  private final Quoter quoter;

  QuoteFilter(Quoter quoter) {
    this.quoter = quoter;
  }

  @Override
  public void accept(int code) {
    switch (status) {
      case NONE:
        switch (code) {
          case '\'':
            quoter.quotingChanged(Quoting.NONE, Quoting.STRONG);
            status = Quoting.STRONG;
            break;
          case '"':
            quoter.quotingChanged(Quoting.NONE, Quoting.WEAK);
            status = Quoting.WEAK;
            break;
          case '\\':
            quoter.quotingChanged(Quoting.NONE, Quoting.ESC);
            status = Quoting.ESC;
            break;
          default:
            quoter.accept(code);
            break;
        }
        break;
      case STRONG:
        if (code == '\'') {
          quoter.quotingChanged(Quoting.STRONG, Quoting.NONE);
          status = Quoting.NONE;
        } else {
          quoter.accept(code);
        }
        break;
      case WEAK:
        if (code == '"') {
          quoter.quotingChanged(Quoting.STRONG, Quoting.NONE);
          status = Quoting.NONE;
        } else {
          quoter.accept(code);
        }
        break;
      case ESC:
        quoter.accept(code);
        quoter.quotingChanged(Quoting.STRONG, Quoting.NONE);
        status = Quoting.NONE;
        break;
      default:
        break;
    }
  }

}
