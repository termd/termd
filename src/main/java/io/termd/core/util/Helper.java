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

package io.termd.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.regex.Pattern;

/**
 * Various utils.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Helper {

  public static void uncheckedThrow(Throwable throwable) {
    Helper.<RuntimeException>throwIt(throwable);
  }

  private static <T extends Throwable> void throwIt(Throwable throwable) throws T {
    throw (T)throwable;
  }

  /**
   * Do absolutely nothing. This can be useful for code coverage analysis.
   */
  public static void noop() {}

  /**
   * Convert the string to an array of code points.
   *
   * @param s the string to convert
   * @return the code points
   */
  public static int[] toCodePoints(String s) {
    return s.codePoints().toArray();
  }

  /**
   * Code point to string conversion.
   *
   * @param codePoints the code points
   * @return the corresponding string
   */
  public static String fromCodePoints(int[] codePoints) {
    return new String(codePoints, 0, codePoints.length);
  }

  public static void appendCodePoints(int[] codePoints, StringBuilder sb) {
    consumeTo(codePoints, sb::appendCodePoint);
  }

  public static void consumeTo(int[] i, IntConsumer consumer) {
    for (int codePoint : i) {
      consumer.accept(codePoint);
    }
  }

  public static <S> List<S> loadServices(ClassLoader loader, Class<S> serviceClass) {
    ArrayList<S> services = new ArrayList<>();
    Iterator<S> i = ServiceLoader.load(serviceClass, loader).iterator();
    while (i.hasNext()) {
      try {
        S service = i.next();
        services.add(service);
      } catch (Exception ignore) {
        // Log me
      }
    }
    return services;
  }

  public static List<Integer> list(int... list) {
    ArrayList<Integer> result = new ArrayList<>(list.length);
    for (int i : list) {
      result.add(i);
    }
    return result;
  }

  public static List<String> split(String s, char c) {
    List<String> ret = new ArrayList<>();
    int prev = 0;
    while (true) {
      int pos = s.indexOf('\n', prev);
      if (pos == -1) {
        break;
      }
      ret.add(s.substring(prev, pos));
      prev = pos + 1;
    }
    ret.add(s.substring(prev));
    return ret;
  }

  /**
   * Escape a string to be printable in a terminal: any non printable char is replaced by its
   * octal escape and the {@code \} char is replaced by the @{code \\} sequence.
   *
   * @param s the string to escape
   * @return the escaped string
   */
  public static String escape(String s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0;i < s.length();i++) {
      char c = s.charAt(i);
      if (c == 0) {
        sb.append("\\0");
      }  else if (c < 32) {
        sb.append("\\");
        String octal = Integer.toOctalString(c);
        for (int j = octal.length();j < 3;j++) {
          sb.append('0');
        }
        sb.append(octal);
      } else if (c == '\\') {
        sb.append("\\\\");
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  public static int[] findLongestCommonPrefix(List<int[]> entries) {
    if (entries.isEmpty()) {
      return new int[0];
    }
    int minLen = entries.stream().mapToInt(entry -> entry.length).min().getAsInt();
    int len = 0;
    out:
    while (len < minLen) {
      for (int j = 1;j < entries.size();j++) {
        if (entries.get(j)[len] != entries.get(j - 1)[len]) {
          break out;
        }
      }
      len++;
    }
    return Arrays.copyOf(entries.get(0), len);
  }

  public static int[] computeBlock(Vector size, List<int[]> completions) {
    if (completions.size() == 0) {
      return new int[0];
    }
    int max = completions.stream().mapToInt(comp -> comp.length).max().getAsInt();
    int row = size.x() / (max + 1);
    int count = 0;
    StringBuilder sb = new StringBuilder();
    for (int[] completion : completions) {
      Helper.appendCodePoints(completion, sb);
      for (int i = completion.length;i < max;i++) {
        sb.append(' ');
      }
      if (count++ < row) {
        sb.append(' ');
      } else {
        sb.append('\n');
        count = 0;
      }
    }
    sb.append("\n");
    return Helper.toCodePoints(sb.toString());
  }

  /**
   * Compute the position of the char at the specified {@literal offset} of the {@literal codePoints} given a
   * {@literal width} and a relative {@literal origin} position.
   *
   * @param origin the relative position to start from
   * @param width the screen width
   * @return the height
   */
  public static Vector computePosition(int[] codePoints, Vector origin, int offset, int width) {
    if (offset < 0) {
      throw new IndexOutOfBoundsException("Offset cannot be negative");
    }
    if (offset > codePoints.length) {
      throw new IndexOutOfBoundsException("Offset cannot bebe greater than the length");
    }
    int col = origin.x();
    int row = origin.y();
    for (int i = 0;i < offset;i++) {
      int cp = codePoints[i];
      int w = Wcwidth.of(cp);
      if (w == -1) {
        if (cp == '\r') {
          col = 0;
        } else if (cp == '\n') {
          col = 0;
          row++;
        }
      } else {
        if (col + w > width) {
          if (w > width) {
            throw new UnsupportedOperationException("Handle this case gracefully");
          }
          col = 0;
          row++;
        }
        col += w;
        if (col >= width) {
          col -= width;
          row++;
        }
      }
    }
    return new Vector(col, row);
  }

  public static Consumer<Throwable> startedHandler(CompletableFuture<?> fut) {
    return err -> {
      if (err == null) {
        fut.complete(null);
      } else {
        fut.completeExceptionally(err);
      }
    };
  }

  public static Consumer<Throwable> stoppedHandler(CompletableFuture<?> fut) {
    return err -> {
      fut.complete(null);
    };
  }

  private static final String SPACE = " ";
  private static final char SPACE_CHAR = ' ';
  private static final char BACK_SLASH = '\\';
  private static final Pattern spaceEscapedPattern = Pattern.compile("\\\\ ");

  public static String findWordClosestToCursor(String text, int cursor) {
    boolean startOutsideText = false;
    if (cursor >= text.length()) {
      cursor = text.length() - 1;
      startOutsideText = true;
    }
    if (cursor < 0 || text.trim().length() == 0)
      return "";

    boolean foundBackslash = false;

    if (text.contains(SPACE)) {
      int start, end;
      if (text.charAt(cursor) == SPACE_CHAR) {
        if (startOutsideText)
          return "";
        if (cursor > 0) {
          if (text.charAt(cursor - 1) == SPACE_CHAR)
            return "";
          else
            cursor--;
        }
      }

      boolean space = false;
      for (start = cursor; start > 0; start--) {
        if (space) {
          if (text.charAt(start) == BACK_SLASH) {
            space = false;
            foundBackslash = true;
          }
          else {
            start += 2;
            break;
          }
        }
        if (Character.isSpaceChar(text.charAt(start)))
          space = true;
      }

      boolean back = false;
      for (end = cursor; end < text.length(); end++) {
        if (text.charAt(end) == BACK_SLASH) {
          back = true;
          foundBackslash = true;
        }
        else if (back) {
          if (Character.isSpaceChar(text.charAt(end))) {
            back = false;
          }
        }
        else if (Character.isSpaceChar(text.charAt(end))) {
          break;
        }
      }
      if (foundBackslash)
        return switchEscapedSpacesToSpacesInWord(text.substring(start, end));
      else
        return text.substring(start, end);
    }
    else {
      return text.trim();
    }
  }

  public static String switchEscapedSpacesToSpacesInWord(String word) {
    return spaceEscapedPattern.matcher(word).replaceAll(SPACE);
  }

}
