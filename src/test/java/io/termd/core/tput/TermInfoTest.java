package io.termd.core.tput;

import junit.framework.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TermInfoTest {

  @Test
  public void testBlankOrComment() {
    String[] tests = {
        "",
        " ",
        "\t",
        " \t",
        "\t ",
        "#",
        "# foo",
        " #",
        "\t#"
    };
    for (String test : tests) {
      Matcher matcher = TermInfoParser.BLANK_OR_COMMENT.matcher(test);
      assertTrue("Was expecting <" + test + "> to match", matcher.matches());
    }
    tests = new String[] {
        "a",
        " a",
        "\ta",
        " \ta",
        "\t a",
    };
    for (String test : tests) {
      Matcher matcher = TermInfoParser.BLANK_OR_COMMENT.matcher(test);
      assertFalse("Was not expecting <" + test + "> to match", matcher.matches());
    }
  }

  private void assertMatch(String s) {
    Matcher matcher = TermInfoParser.BLANK_OR_COMMENT.matcher(s);
    assertTrue(matcher.find());
    assertEquals(s.length(), matcher.end());
  }

  @Test
  public void testRegexp() {
    assertMatch("");
    assertMatch("\n");
    assertMatch(" ");
    assertMatch(" \n");
    assertMatch("\t");
    assertMatch("\t\n");
    assertMatch("\t ");
    assertMatch("\t \n");
    assertMatch(" \t");
    assertMatch(" \t\n");
    assertMatch("#");
    assertMatch("#\n");
    assertMatch(" #");
    assertMatch(" #\n");
    assertMatch("\t#");
    assertMatch("\t#\n");
    assertMatch("#c");
    assertMatch("#c\n");
  }

  private TermInfoParser parser = new TermInfoParser();

  @Test
  public void testParseHeaderLine() {
    assertEquals(3, parser.parseHeaderLine("a,\n", 0, new ArrayList<String>()));
    assertEquals(5, parser.parseHeaderLine("a|b,\n", 0, new ArrayList<String>()));
    assertEquals(7, parser.parseHeaderLine("a|b|c,\n", 0, new ArrayList<String>()));
    failParseHeaderLine("");
    failParseHeaderLine("a");
    failParseHeaderLine("a,");
    failParseHeaderLine("a|b");
    failParseHeaderLine("a|b,");
  }

  private void failParseHeaderLine(String s) {
    try {
      parser.parseHeaderLine(s, 0, new ArrayList<String>());
      fail("was expecting <" + s + "> to fail");
    } catch (IllegalArgumentException ignore) {
    }
  }

  @Test
  public void testParseFeatureLine() {
    assertParseFeatureLine("\ta,\n", new TermInfo.Feature.Boolean("a"));
    assertParseFeatureLine("\ta, \n", new TermInfo.Feature.Boolean("a"));
    assertParseFeatureLine("\ta,\t\n", new TermInfo.Feature.Boolean("a"));
    assertParseFeatureLine("\ta,b,\n", new TermInfo.Feature.Boolean("a"), new TermInfo.Feature.Boolean("b"));
    assertParseFeatureLine("\ta, b,\n", new TermInfo.Feature.Boolean("a"), new TermInfo.Feature.Boolean("b"));
    assertParseFeatureLine("\ta,\tb,\n", new TermInfo.Feature.Boolean("a"), new TermInfo.Feature.Boolean("b"));
    assertParseFeatureLine("\ta=b,\n", new TermInfo.Feature.String("a", "b"));
    assertParseFeatureLine("\ta#1,\n", new TermInfo.Feature.Numeric("a", "1"));
    failParseFeatureLine("");
    failParseFeatureLine("a");
    failParseFeatureLine("a,");
    failParseFeatureLine("a,\n");
    failParseFeatureLine("\t");
    failParseFeatureLine("\ta");
    failParseFeatureLine("\ta,");
    failParseFeatureLine("\ta,b");
    failParseFeatureLine("\ta,b,");
    failParseFeatureLine("\ta=,");
    failParseFeatureLine("\ta#,");
    failParseFeatureLine("\ta#b,");
  }

  private void assertParseFeatureLine(String s, TermInfo.Feature... expectedFeatures) {
    List<TermInfo.Feature> features = new ArrayList<>();
    assertEquals(s.length(), parser.parseFeatureLine(s, 0, features));
    assertEquals(Arrays.asList(expectedFeatures), features);
  }

  private void failParseFeatureLine(String s) {
    try {
      if (parser.parseFeatureLine(s, 0, new ArrayList<TermInfo.Feature>()) > 0) {
        fail("was expecting <" + s + "> to fail");
      }
    } catch (IllegalArgumentException ignore) {
    }
  }

  @Test
  public void testParseDescription() {
    parser.parseDescription("a|b,\n\ta,\n", 0, new ArrayList<TermInfo.Entry>());
  }


  @Test
  public void testParse() throws IOException {
    ByteArrayOutputStream res = new ByteArrayOutputStream();
    try (InputStream in = TermInfoParser.class.getResourceAsStream("terminfo.src")) {
      byte[] buffer = new byte[256];
      while (true) {
        int len = in.read(buffer);
        if (len == -1) {
          break;
        }
        res.write(buffer, 0, len);
      }
    }
    String s = res.toString("ISO-8859-1");
    assertEquals(s.length(), parser.parseDescriptions(s, 0, new ArrayList<TermInfo.Entry>()));
  }
}
