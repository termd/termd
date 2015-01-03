package io.termd.core.readline;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    assertEscape("'", "<'>");
    assertEscape("'a", "<'>a");
    assertEscape("'a'", "<'>a</'>");
    assertEscape("'\"'", "<'>\"</'>");
    assertEscape("'\n'", "<'>\n</'>");
    assertEscape("'\\'", "<'>\\</'>");
    assertEscape("'a\nb'", "<'>a\nb</'>");
    assertEscape("'a'\n", "<'>a</'>\n");
  }

  @Test
  public void testDoubleQuote() {
    assertEscape("\"", "<\">");
    assertEscape("\"a", "<\">a");
    assertEscape("\"a\"", "<\">a</\">");
    assertEscape("\"'\"", "<\">'</\">");
    assertEscape("\"\n\"", "<\">\n</\">");
    assertEscape("\"\\\"", "<\">\\</\">");
    assertEscape("\"a\nb\"", "<\">a\nb</\">");
    assertEscape("\"a\"\n", "<\">a</\">\n");
  }

  @Test
  public void testBackslash() {
    assertEscape("\\", "[");
    assertEscape("\\a", "[a]");
    assertEscape("\\ab", "[a]b");
    assertEscape("\\\\", "[\\]");
    assertEscape("\\'", "[']");
    assertEscape("\\\"", "[\"]");
    assertEscape("\\\n", "[\n]");
  }

  private void assertEscape(String line, String expected) {
    String actual = escape(line);
    assertEquals(expected, actual);
  }

  private String escape(String line) {
    final StringBuilder builder = new StringBuilder();
    EscapeFilter escaper = new EscapeFilter(new Escaper() {
      Integer delimiter;
      @Override
      public void beginQuotes(int delim) {
        builder.append("<").appendCodePoint(delim).append(">");
        this.delimiter = delim;
      }
      @Override
      public void escaping() {
        builder.append('[');
      }
      @Override
      public void escaped(int ch) {
        builder.appendCodePoint(ch).appendCodePoint(']');
      }
      @Override
      public void endQuotes(int delim) {
        assertEquals((int)this.delimiter, delim);
        builder.append("</").appendCodePoint(delim).append(">");
        this.delimiter = null;
      }
      @Override
      public void handle(Integer value) {
        builder.appendCodePoint(value);
      }
    });
    for (int offset = 0;offset < line.length();) {
      int cp = line.codePointAt(offset);
      escaper.handle(cp);
      offset += Character.charCount(cp);
    }
    return builder.toString();
  }

}
