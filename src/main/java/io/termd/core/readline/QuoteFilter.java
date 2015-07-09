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

  private Quote status = Quote.NONE;
  private boolean escaping = false;
  private final QuoteListener quoter;

  QuoteFilter(QuoteListener quoter) {
    this.quoter = quoter;
  }

  @Override
  public void accept(int code) {
    if (escaping) {
      escaping = false;
      quoter.accept(code);
    } else {
      switch (status) {
        case NONE:
          switch (code) {
            case '\'':
              quoter.quotingChanged(Quote.NONE, Quote.STRONG);
              status = Quote.STRONG;
              break;
            case '"':
              quoter.quotingChanged(Quote.NONE, Quote.WEAK);
              status = Quote.WEAK;
              break;
            case '\\':
              quoter.escaping();
              escaping = true;
              break;
            default:
              quoter.accept(code);
              break;
          }
          break;
        case STRONG:
          if (code == '\'') {
            quoter.quotingChanged(Quote.STRONG, Quote.NONE);
            status = Quote.NONE;
          } else {
            quoter.accept(code);
          }
          break;
        case WEAK:
          if (code == '"') {
            quoter.quotingChanged(Quote.STRONG, Quote.NONE);
            status = Quote.NONE;
          } else if (code == '\\') {
            // Note we don't make the distinction between special chars like " or \ from other chars
            // that are supposed to not escaped (i.e "\a" is \a and "\$" is $)
            // this interpretation is not done by termd
            escaping = true;
            quoter.escaping();
          } else {
            quoter.accept(code);
          }
          break;
        default:
          break;
      }
    }
  }

}
