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

import java.util.LinkedList;

/**
 * Encapsulate evalutation state + operations.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class EvalContext {

  final LinkedList<String> stack = new LinkedList<>();
  final String[] parameters;
  final StringBuilder result;

  public EvalContext(String[] parameters, StringBuilder result) {
    this.parameters = parameters;
    this.result = result;
  }

  public EvalContext(String... parameters) {
    this.parameters = parameters;
    this.result = new StringBuilder();
  }

  public int getParametersLength() {
    return parameters.length;
  }

  public String getParameter(int index) {
    return parameters[index];
  }

  public void setParameter(int index, String value) {
    parameters[index] = value;
  }

  public String pop() {
    return stack.pop();
  }

  public EvalContext push(String s) {
    stack.push(s);
    return this;
  }

  public void writeString(String s) {
    result.append(s);
  }

  public void writeNumber(int number) {
    result.append(number);
  }

  public void writeCodePoint(int codePoint) {
    result.appendCodePoint(codePoint);
  }
}
