package io.termd.core.term;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class TermEvent {

  public static class Read extends TermEvent {
    final int[] data;
    public Read(int[] data) {
      this.data = data;
    }
    public int[] getData() {
      return data;
    }
  }

  public static class Size extends TermEvent {

    final int width;
    final int height;

    public Size(int width, int height) {
      this.width = width;
      this.height = height;
    }

    public int getWidth() {
      return width;
    }

    public int getHeight() {
      return height;
    }
  }
}
