package io.modsh.core.readline;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface InputrcHandler {

  Pattern COMMENT = Pattern.compile("#.*");
  Pattern CONDITIONAL = Pattern.compile("\\$.*");
  Pattern SET_VARIABLE = Pattern.compile("set\\s+(\\S)+\\s+(\\S)+\\s*");
  Pattern BIND = Pattern.compile("(?:(?:\"(.*)\")|(.*))" + ":\\s*" + "(?:(?:\"(.*)\")|(?:'(.*)')|(\\S+))" + "\\s*");
  Pattern A = Pattern.compile("^\\\\([0-9]{1,3})");
  Pattern B = Pattern.compile("^\\\\x([0-9,A-F,a-f]{1,2})");

  public static void parse(String s, InputrcHandler handler) throws UnsupportedEncodingException {
    parse(new ByteArrayInputStream(s.getBytes("US-ASCII")), handler);
  }

  public static void parse(InputStream s, InputrcHandler handler) {

    Scanner sc = new Scanner(s, "US-ASCII").useDelimiter("\n");
    while (sc.hasNext()) {
      String next = sc.next();
      if (COMMENT.matcher(next).matches()) {
        System.out.println("comment");
      } else if (CONDITIONAL.matcher(next).matches()) {
        System.out.println("conditional");
      } else {
        Matcher matcher = SET_VARIABLE.matcher(next);
        if (matcher.matches()) {
          String variable = matcher.group(1);
          String value = matcher.group(2);
          System.out.println("SET " + variable + " = " + value);
        } else {
          matcher = BIND.matcher(next);
          if (matcher.matches()) {
            String keyseq = matcher.group(1);
            String keyname = matcher.group(2);
            String macro1 = matcher.group(3);
            String macro2 = matcher.group(4);
            String functionname = matcher.group(5);
            if (keyseq != null) {
              IntStream.Builder builder = IntStream.builder();
              while (keyseq.length() > 0) {
                if (keyseq.startsWith("\\C-") && keyseq.length() > 3) {
                  int c = (Character.toUpperCase(keyseq.charAt(3)) - '@') & 0x7F;
                  builder.add(c);
                  keyseq = keyseq.substring(4);
                } else if (keyseq.startsWith("\\M-") && keyseq.length() > 3) {
                  int c = (Character.toUpperCase(keyseq.charAt(3)) - '@') & 0x7F;
                  builder.add(27).add(c);
                  keyseq = keyseq.substring(4);
                } else if (keyseq.startsWith("\\e")) {
                  builder.add(27);
                  keyseq = keyseq.substring(2);
                } else if (keyseq.startsWith("\\\\")) {
                  builder.add((int)'\\');
                  keyseq = keyseq.substring(2);
                } else if (keyseq.startsWith("\\\"")) {
                  builder.add((int)'"');
                  keyseq = keyseq.substring(2);
                } else if (keyseq.startsWith("\\'")) {
                  builder.add((int)'\'');
                  keyseq = keyseq.substring(2);
                } else if (keyseq.startsWith("\\a")) {
                  builder.add(7);
                  keyseq = keyseq.substring(2);
                } else if (keyseq.startsWith("\\b")) {
                  builder.add(8);
                  keyseq = keyseq.substring(2);
                } else if (keyseq.startsWith("\\d")) {
                  builder.add(127);
                  keyseq = keyseq.substring(2);
                } else if (keyseq.startsWith("\\f")) {
                  builder.add(12);
                  keyseq = keyseq.substring(2);
                } else if (keyseq.startsWith("\\n")) {
                  builder.add(10);
                  keyseq = keyseq.substring(2);
                } else if (keyseq.startsWith("\\r")) {
                  builder.add(13);
                  keyseq = keyseq.substring(2);
                } else if (keyseq.startsWith("\\t")) {
                  builder.add(9);
                  keyseq = keyseq.substring(2);
                } else if (keyseq.startsWith("\\v")) {
                  builder.add(11);
                  keyseq = keyseq.substring(2);
                } else {
                  matcher = A.matcher(keyseq);
                  if (matcher.find()) {
                    builder.add(Integer.parseInt(matcher.group(1), 8));
                    keyseq = keyseq.substring(matcher.end());
                  } else {
                    matcher = B.matcher(keyseq);
                    if (matcher.find()) {
                      builder.add(Integer.parseInt(matcher.group(1), 16));
                      keyseq = keyseq.substring(matcher.end());
                    } else {
                      builder.add((int) keyseq.charAt(0));
                      keyseq = keyseq.substring(1);
                    }
                  }
                }
              }
              int[] f = builder.build().toArray();
              if (functionname != null) {
                handler.bindFunction(f, functionname);
              } else if (macro1 != null) {
                handler.bindMacro(f, macro1);
              } else {
                handler.bindMacro(f, macro2);
              }
            } else {
              if (functionname != null) {
                handler.bindFunction(keyname, functionname);
              } else if (macro1 != null) {
                handler.bindMacro(keyname, macro1);
              } else {
                handler.bindMacro(keyname, macro2);
              }
            }
          }
        }
      }
    }
  }

  default void bindMacro(String keyName, String macro) {
  }

  default void bindFunction(String keyName, String functionName) {
  }

  default void bindMacro(int[] keySequence, String macro) {
  }

  default void bindFunction(int[] keySequence, String functionName) {
  }
}
