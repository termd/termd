package io.termd.core.readline;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class LineBuffer implements Iterable<Integer> {

  private int[] data;
  private int cursor;
  private int size;

  public LineBuffer() {
    data = new int[1000];
  }

  public LineBuffer(LineBuffer that) {
    data = that.data.clone();
    cursor = that.cursor;
    size = that.size;
  }

  @Override
  public Iterator<Integer> iterator() {
    return new Iterator<Integer>() {
      int index;
      @Override
      public boolean hasNext() {
        return index < size;
      }
      @Override
      public Integer next() {
        if (index >= size) {
          throw new NoSuchElementException();
        }
        return data[index++];
      }
      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  public void insert(int codePoint) {
    if (cursor < size) {
      System.arraycopy(data, cursor, data, cursor + 1, size - cursor);
    }
    data[cursor++] = codePoint;
    size++;
  }

  public void insert(int... codePoints) {
    int len = codePoints.length;
    if (cursor < size) {
      System.arraycopy(data, cursor, data, cursor + len, size - cursor);
    }
    System.arraycopy(codePoints, 0, data, cursor, len);
    cursor+= len;
    size += len;
  }

  public int deleteAt(int delta) {
    if (delta > 0) {
      throw new UnsupportedOperationException();
    } else if (delta < 0) {
      delta = - Math.min(- delta, cursor);
      System.arraycopy(data, cursor, data, cursor + delta, size - cursor);
      size += delta;
      cursor += delta;
      return delta;
    } else {
      return 0;
    }
  }

  public int getSize() {
    return size;
  }

  public int getCursor() {
    return cursor;
  }

  public void setCursor(int next) {
    this.cursor = next < 0 ? 0 : (next > size ? size : next);
  }

  public int moveCursor(int delta) {
    int prev = cursor;
    setCursor(cursor + delta);
    return cursor - prev;
  }

  public void setSize(int size) {
    this.size = size >= 0 ? size : 0;
    if (cursor > size) {
      cursor = size;
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < size; i++) {
      sb.appendCodePoint(data[i]);
    }
    return sb.toString();
  }

  public LinkedList<Integer> compute(LineBuffer target) {

    LinkedList<Integer> ret = new LinkedList<>();

    int len = Math.min(size, target.size);

    //
    for (int i = 0;i < len;i++) {
      if (data[i] != target.data[i]) {
        while (cursor != i) {
          if (cursor > i) {
            cursor--;
            ret.add((int)'\b');
          } else {
            ret.add(data[cursor++]);
          }
        }
        ret.add(data[cursor] = target.data[cursor++]);
      }
    }

    //
    if (size > target.size) {
      while (cursor < size) {
        ret.add(data[cursor++]);
      }
      while (size > target.size) {
        ret.add((int)'\b');
        ret.add((int)' ');
        ret.add((int)'\b');
        cursor--;
        size--;
      }
    } else if (size < target.size) {
      while (size < target.size) {
        while (cursor < size) {
          ret.add(data[cursor++]);
        }
        ret.add(target.data[cursor++]);
        size++;
      }
    }

    //
    while (cursor != target.cursor) {
      if (cursor < target.cursor) {
        ret.add(data[cursor++]);
      } else {
        ret.add((int)'\b');
        cursor--;
      }
    }

    //
    return ret;
  }
}
