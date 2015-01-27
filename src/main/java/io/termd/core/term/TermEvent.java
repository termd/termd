package io.termd.core.term;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class TermEvent {

  public static class Data extends TermEvent {
    final int[] data;
    public Data(int[] data) {
      this.data = data;
    }
    public int[] getData() {
      return data;
    }
  }

  public static class Resize extends TermEvent {

  }
}
