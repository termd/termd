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

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Quoter {

  private Quote quote = Quote.NONE;
  private boolean escaping = false;

  public Quote getQuote() {
    return quote;
  }

  public QuoteResult update(int code) {
    if (escaping) {
      escaping = false;
      return QuoteResult.CODE_POINT;
    } else {
      switch (quote) {
        case NONE:
          switch (code) {
            case '\'':
              quote = Quote.STRONG;
              return QuoteResult.UPDATED;
            case '"':
              quote = Quote.WEAK;
              return QuoteResult.UPDATED;
            case '\\':
              escaping = true;
              return QuoteResult.ESC;
            default:
              return QuoteResult.CODE_POINT;
          }
        case STRONG:
          if (code == '\'') {
            quote = Quote.NONE;
            return QuoteResult.UPDATED;
          } else {
            return QuoteResult.CODE_POINT;
          }
        case WEAK:
          if (code == '"') {
            quote = Quote.NONE;
            return QuoteResult.UPDATED;
          } else if (code == '\\') {
            // Note we don't make the distinction between special chars like " or \ from other chars
            // that are supposed to not escaped (i.e "\a" is \a and "\$" is $)
            // this interpretation is not done by termd
            escaping = true;
            return QuoteResult.ESC;
          } else {
            return QuoteResult.CODE_POINT;
          }
        default:
          throw new AssertionError();
      }
    }
  }
}
