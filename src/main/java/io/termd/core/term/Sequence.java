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

package io.termd.core.term;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Sequence implements Iterable<OpCode> {

  private final List<OpCode> value;

  public Sequence(List<OpCode> value) {
    this.value = value;
  }

  public Sequence(String s) {
    value = Collections.<OpCode>singletonList(new OpCode.Literal(s));
  }

  public int size() {
    return value.size();
  }

  public String eval(String... parameters) {
    StringBuilder buffer = new StringBuilder();
    eval(parameters, buffer);
    return buffer.toString();
  }

  public void eval(String[] parameters, StringBuilder result) {
    eval(new EvalContext(parameters, result));
  }

  public void eval(String[] parameters, Consumer<int[]> result) {
    eval(new EvalContext(parameters, result));
  }

  public void eval(EvalContext context) {
    for (OpCode op : value) {
      op.eval(context);
    }
  }

  @Override
  public Iterator<OpCode> iterator() {
    return value.iterator();
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    for (OpCode op : value) {
      op.toString(buffer);
    }
    return buffer.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof Sequence) {
      Sequence that = (Sequence) obj;
      return value.equals(that.value);
    }
    return false;
  }
}
