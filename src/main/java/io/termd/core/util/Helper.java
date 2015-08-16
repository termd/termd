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

/**
 * Various utils.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Helper {

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

  public static void appendCodePoints(StringBuilder sb, int[] codePoints) {
    for (int codePoint : codePoints) {
      sb.appendCodePoint(codePoint);
    }
  }

  public static void appendTo(int[] codePoints, StringBuilder sb) {
    for (int codePoint : codePoints) {
      sb.appendCodePoint(codePoint);
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

  public static int[] computeBlock(Dimension size, List<int[]> completions) {

    StringBuilder sb = new StringBuilder();

    // Determine the longest value
    int max = completions.stream().mapToInt(comp -> comp.length).max().getAsInt();

    //
    int row = size.width() / (max + 1);

    int count = 0;
    for (int[] completion : completions) {
      Helper.appendCodePoints(sb, completion);
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

    //
    sb.append("\n");

    //
    return Helper.toCodePoints(sb.toString());
  }
}
