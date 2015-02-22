package io.termd.core.term;

import java.util.List;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ParserHandler {

  public void beginHeaderLine(String name) {}

  public void addHeader(String name) {}

  public void endHeaderLine() {}

  public void addBooleanFeature(String name, boolean value) {}

  public void addStringFeature(String name, String value) {}

  public void addStringFeature(String name, List<Op> value) {
    StringBuilder buffer = new StringBuilder();
    for (Op op : value) {
      op.toString(buffer);
    }
    addStringFeature(name, buffer.toString());
  }

  public void addNumericFeature(String name, int value) {}

  public void endDevice() {}

  public void endDatabase() {}
}
