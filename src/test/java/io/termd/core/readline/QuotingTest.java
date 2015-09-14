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
    LineStatus.Ext buf = new LineStatus.Ext();
    boolean escaping = false;
    int prev = 0;
    for (int offset = 0; offset < line.length(); ) {
      int cp = line.codePointAt(offset);
      buf.accept(cp);
      if (buf.isEscaping()) {
        builder.append("[");
        escaping = true;
      } else {
        if (prev != buf.getQuote()) {
          switch (prev) {
            case 0:
              builder.append("<").appendCodePoint(buf.getQuote()).append(">");
              break;
            default:
              builder.append("</").appendCodePoint(prev).append(">");
              break;
          }
          prev = buf.getQuote();
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
