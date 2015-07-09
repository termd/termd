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

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
class ParsedBuffer {

  final LinkedList<Integer> buffer = new LinkedList<>();
  Quote quoting = Quote.NONE;
  boolean escaped = false;
  final QuoteFilter filter = new QuoteFilter(new QuoteListener() {
    @Override
    public void escaping() {
      escaped = true;
      buffer.add((int)'\\');
    }
    @Override
    public void quotingChanged(Quote prev, Quote q) {
      if (q == Quote.NONE) {
        if (!escaped) {
          buffer.add(quoting.ch);
        }
      } else {
        buffer.add(q.ch);
      }
      quoting = q;
    }
    @Override
    public void accept(int ch) {
      if (escaped) {
        if (ch != '\r') {
          buffer.add(ch);
        } else {
          buffer.removeLast();
        }
        escaped = false;
      } else {
        buffer.add(ch);
      }
    }
  });
}
