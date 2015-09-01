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

import io.termd.core.util.Vector;
import io.termd.core.util.Helper;
import io.termd.core.util.Wcwidth;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

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

  public int[] toArray() {
    return Arrays.copyOf(data, size);
  }

  public int getAt(int index) {
    if (index < 0 | index >= size) {
      throw new IndexOutOfBoundsException();
    }
    return data[index];
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

  /**
   * Insert a string in the buffer at the current cursor position.
   *
   * @see {@link #insert(int)}
   */
  public LineBuffer insert(String s) {
    return insert(Helper.toCodePoints(s));
  }

  /**
   * Insert a codepoint in the buffer at the current cursor position.
   *
   * @param cp the codepoint to insert
   * @return this object
   * @throws IllegalArgumentException when an illegal character is inserted
   */
  public LineBuffer insert(int cp) {
    int w = Wcwidth.of(cp);
    if (w == -1) {
      if (cp != '\n') {
        throw new IllegalArgumentException("LineBuffer can only contain \n control char");
      }
    } else if (w != 1) {
      throw new IllegalArgumentException("LineBuffer cannot contain chars of width!=1 for the moment");
    }
    if (cursor < size) {
      System.arraycopy(data, cursor, data, cursor + 1, size - cursor);
    }
    data[cursor++] = cp;
    size++;
    return this;
  }

  /**
   * Insert an array of code points in the buffer at the current cursor position.
   */
  public LineBuffer insert(int... codePoints) {
    for (int cp : codePoints) {
      insert(cp);
    }
    return this;
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

  public void clear() {
    size = 0;
    cursor = 0;
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

  public int[] compute(LineBuffer target) {

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
    return ret.stream().mapToInt(i->i).toArray();
  }

  /**
   * Compute the current cursor position of this line buffer given a {@literal width} and a relative {@literal origin}
   * position.
   *
   * @param width the screen width
   * @return the height
   */
  public Vector getCursorPosition(int width) {
    return getPosition(cursor, width);
  }

  /**
   * Compute the position of the char at the specified {@literal offset} of this line buffer given a
   * {@literal width} and a relative {@literal start} position.
   *
   * @param width the screen width
   * @return the height
   */
  public Vector getPosition(int offset, int width) {
    if (offset > size) {
      throw new IndexOutOfBoundsException("Offset cannot bebe greater than the buffer size");
    }
    return Helper.computePosition(data, new Vector(0, 0), offset, width);
  }

  private int findEndOfLine(int offset) {
    while (offset < size) {
      int c = data[offset];
      int w = Wcwidth.of(c);
      if (w == -1) {
        if (c == '\n') {
          break; // ?? unsure
        } else {
          throw new UnsupportedOperationException();
        }
      }
      offset++;
    }
    return offset;
  }

  public void update(LineBuffer dst, Consumer<int[]> out, int width) {
    new Update(out, width).perform(dst);
  }

  // The update algorithm encapsulated in an inner class
  // todo : use term capabilities instead of hardcoded ansi programming
  // todo : support other control chars
  // todo : support codepoint of with != 1 (like combining chars, etc...)
  // todo : issue existing chars for moving right instead of cursor left movement
  private class Update {

    private final Consumer<int[]> out;
    private final int width;
    private int scrCol, scrRow; // The current screen cursor position
    private int srcIdx, srcCol, srcRow; // The source state
    private int dstIdx, dstCol, dstRow; // The destination state

    public Update(Consumer<int[]> out, int width) {
      this.out = out;
      this.width = width;
      this.scrCol = getCursorPosition(width).x();
      this.scrRow = getCursorPosition(width).y();
    }

    public void perform(LineBuffer dst) {

      while (dstIdx < dst.size) {

        int eol = dst.findEndOfLine(dstIdx);
        boolean needGlitchCorrection = dstIdx < eol;

        // Handle one dest line at a time
        while (dstIdx < eol) {
          int c = dst.data[dstIdx];
          int w = Wcwidth.of(c);
          if (w != 1) {
            throw new UnsupportedOperationException();
          }
          if (srcIdx < size && new Vector(srcCol, srcRow).equals(new Vector(dstCol, dstRow))) {
            if (data[srcIdx] == dst.data[dstIdx]) {
              dstCol += w;
              if (dstCol == width) {
                dstCol = 0;
                dstRow++;
              }
            } else {
              moveCursor(dstCol, dstRow);
              out.accept(new int[]{c});
              dstCol += w;
              if (dstCol == width) {
                dstCol = 0;
                dstRow++;
              }
              scrCol = dstCol;
              scrRow = dstRow;
            }
            dstIdx++;
          } else {
            moveCursor(dstCol, dstRow);
            dstIdx++;
            out.accept(new int[]{c});
            dstCol += w;
            if (dstCol == width) {
              dstCol = 0;
              dstRow++;
            }
            scrCol = dstCol;
            scrRow = dstRow;
          }
          ensure(dstCol, dstRow);
        }

        // Glitch correction if needed
        if (needGlitchCorrection && dstCol == 0) {
          out.accept(new int[]{' ','\r'});
        }

        // Remove extra chars if needed
        if (dstIdx < dst.size) {
          dstIdx++;
          dstCol = 0;
          ++dstRow;
          int _col = srcCol, _row = srcRow;
          if (ensure(dstCol, dstRow)) {
            moveCursor(_col, _row);
            out.accept(new int[]{'\033','[', 'K'});
          }
        }

        // We may need to issue some \n after we are done
        while (scrRow < dstRow) {
          out.accept(new int[]{'\n'});
          scrRow++;
          scrCol = 0;
        }
      }

      // Erase extra remaining chars
      if (srcIdx < size) {
        int _col = srcCol;
        int _row = srcRow;
        int count = 0;
        while (srcIdx < size) {
          int c = data[srcIdx++];
          if (c == '\n') {
            if (count > 0) {
              moveCursor(_col, _row);
              out.accept(new int[]{'\033', '[', 'K'});
              count = 0;
            }
            _col = srcCol = 0;
            _row = ++srcRow;
          } else {
            int w = Wcwidth.of(c);
            if (w != 1) {
              throw new UnsupportedOperationException();
            }
            srcCol++;
            if (srcCol == width) {
              if (count > 0) {
                moveCursor(_col, _row);
                out.accept(new int[]{'\033', '[', 'K'});
                count = 0;
                _col = srcCol;
                _row = srcRow;
              }
              srcCol = 0;
              srcRow++;
            } else {
              count++;
            }
          }
        }
        if (count > 0) {
          moveCursor(_col, _row);
          out.accept(new int[]{'\033', '[', 'K'});
        }
      }

      // Move cursor to initial position
      moveCursor(dst.getCursorPosition(width).x(), dst.getCursorPosition(width).y());

      // Update internal state
      data = dst.data.clone();
      cursor = dst.cursor;
      size = dst.size;
    }

    /**
     * Ensure the source pointers are at least matching the specified column and row
     *
     * @param col the column
     * @param row the row
     * @return true if we skipped some source chars when moving the source pointers
     */
    private boolean ensure(int col, int row) {
      boolean ret = false;
      while (srcIdx < size) {
        if (srcRow > row || (srcRow == row && srcCol >= col)) {
          break;
        }
        int c = data[srcIdx];
        int w = Wcwidth.of(c);
        if (w == 1) {
          ret = true;
          srcCol++;
          if (srcCol == width) {
            srcRow++;
            srcCol = 0;
          }
        } else if (c == '\n') {
          srcCol = 0;
          srcRow++;
        } else {
          throw new UnsupportedOperationException();
        }
        srcIdx++;
      }
      return ret;
    }

    /**
     * Move the cursor to the specified coordinates, this updates the internal physical cursor.
     *
     * @param col the column
     * @param row the row
     */
    private void moveCursor(int col, int row) {
      if (scrCol != col) {
        if (col == 0) {
          out.accept(new int[]{'\r'});
          scrCol = 0;
        } else {
          while (scrCol != col) {
            if (scrCol < col) {
              scrCol++;
              out.accept(new int[]{'\033','[','1','C'});
            } else {
              scrCol--;
              out.accept(new int[]{'\b'});
            }
          }
        }
      }
      while (scrRow != row) {
        if (row < scrRow) {
          scrRow--;
          out.accept(new int[]{27,'[','1','A'});
        } else {
          scrRow++;
          out.accept(new int[]{27,'[','1','B'});
        }
      }
    }
  }
}
