package io.termd.core.readline;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class QuotingTest {

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
    assertEscape("\"\\", "<\">[");
    assertEscape("\"\\\"", "<\">[\"]");
    assertEscape("\"\\\\", "<\">[\\]");
    assertEscape("\"\\a", "<\">[a]");
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
    ParsedBuffer buf = new ParsedBuffer();
    boolean escaping = false;
    Quote prev = Quote.NONE;
    for (int offset = 0; offset < line.length(); ) {
      int cp = line.codePointAt(offset);
      buf.accept(cp);
      if (buf.escaping) {
        builder.append("[");
        escaping = true;
      } else {
        if (prev != buf.quoting) {
          switch (prev) {
            case NONE:
              builder.append("<").appendCodePoint(buf.quoting.ch).append(">");
              break;
            default:
              builder.append("</").appendCodePoint(prev.ch).append(">");
              break;
          }
          prev = buf.quoting;
        } else {
          builder.appendCodePoint(cp);
          if (escaping) {
            builder.append(']');
            escaping = false;
          }
        }
      }
      offset += Character.charCount(cp);
    }
    return builder.toString();
  }

}
