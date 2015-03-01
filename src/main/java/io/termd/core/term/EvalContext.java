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

  public void write(String s) {
    result.append(s);
  }

  public void write(int number) {
    result.append(number);
  }
}
