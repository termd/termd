package io.termd.core.term;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ParserHandler {

  public void beginHeaderLine(String name) {}

  public void addHeader(String name) {}

  public void endHeaderLine() {}

  public void addBooleanFeature(String name, boolean value) {}

  public void addStringFeature(String name, Sequence value) {}

  public void addNumericFeature(String name, int value) {}

  public void endDevice() {}

  public void endDatabase() {}
}
