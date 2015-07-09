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
    Quoter escaper = new Quoter();
    boolean escaping = false;
    Quote prev = Quote.NONE;
    for (int offset = 0; offset < line.length(); ) {
      int cp = line.codePointAt(offset);
      switch (escaper.update(cp)) {
        case UPDATED:
          Quote update = escaper.getQuote();
          switch (update) {
            case NONE:
              builder.append("</").appendCodePoint(prev.ch).append(">");
              break;
            default:
              builder.append("<").appendCodePoint(update.ch).append(">");
              break;
          }
          prev = update;
          break;
        case ESC:
          builder.append("[");
          escaping = true;
          break;
        case CODE_POINT:
          builder.appendCodePoint(cp);
          if (escaping) {
            builder.append(']');
            escaping = false;
          }
          break;
      }
      offset += Character.charCount(cp);
    }
    return builder.toString();
  }

}
