package io.termd.core.term;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Sequence implements Iterable<OpCode> {

  private final List<OpCode> value;

  public Sequence(List<OpCode> value) {
    this.value = value;
  }

  public Sequence(String s) {
    value = Collections.<OpCode>singletonList(new OpCode.Constant(s));
  }

  public int size() {
    return value.size();
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
