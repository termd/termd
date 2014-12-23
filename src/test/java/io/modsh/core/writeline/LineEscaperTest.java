package io.modsh.core.writeline;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class LineEscaperTest {

  @Test
  public void testFoo() {
    assertEscape("a", "a");
    assertEscape("\n", "\n");
    assertEscape("a\n", "a\n");
    assertEscape("\na", "\na");
  }

  @Test
  public void testQuote() {
    assertEscape("'", "", "");
    assertEscape("'a", "", "a");
    assertEscape("'a'", "", "a", "");
    assertEscape("'\"'", "", "\"", "");
    assertEscape("'\n'", "", "\n", "");
    assertEscape("'a\nb'", "", "a\nb", "");
    assertEscape("'a'\n", "", "a", "\n");
  }

  @Test
  public void testDoubleQuote() {
    assertEscape("\"", "", "");
    assertEscape("\"a", "", "a");
    assertEscape("\"a\"", "", "a", "");
    assertEscape("\"'\"", "", "'", "");
    assertEscape("\"\n\"", "", "\n", "");
    assertEscape("\"a\nb\"", "", "a\nb", "");
    assertEscape("\"a\"\n", "", "a", "\n");
  }

  @Test
  public void testBackslash() {
    assertEscape("\\", "", "");
    assertEscape("\\a", "", "a", "");
    assertEscape("\\ab", "", "a", "b");
    assertEscape("\\\\", "", "\\", "");
    assertEscape("\\'", "", "'", "");
    assertEscape("\\\"", "", "\"", "");
    assertEscape("\\\n", "", "\n", "");
  }

  private void assertEscape(String line, String... expected) {
    List<String> actual = escape(line);
    assertEquals(new ArrayList<>(Arrays.asList(expected)), actual);
  }

  private List<String> escape(String line) {
    StringBuilder buffer = new StringBuilder();
    ArrayList<String> lines = new ArrayList<>();
    Runnable next = () -> {
      lines.add(buffer.toString());
      buffer.setLength(0);
    };
    EscapeFilter escaper = new EscapeFilter(new Escaper() {
      boolean escaped;
      @Override
      public void beginEscape(int delimiter) {
        assertFalse(escaped);
        escaped = true;
        next.run();
      }
      @Override
      public void endEscape(int delimiter) {
        assertTrue(escaped);
        escaped = false;
        next.run();
      }
      @Override
      public void accept(int value) {
        buffer.appendCodePoint(value);
      }
    });
    line.codePoints().forEach(escaper::accept);
    lines.add(buffer.toString());
    return lines;
  }

}
