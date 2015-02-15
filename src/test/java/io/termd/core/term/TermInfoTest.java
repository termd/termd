package io.termd.core.term;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TermInfoTest {

  @Test
  public void testParseComment() throws Exception {
    TermInfoParser parser = new TermInfoParser("#\n");
    parser.parseCommentLine();
  }

  @Test
  public void testParseHeaderLine() throws Exception {
    assertParseHeaderLine("foo,\n", "foo");
    assertParseHeaderLine("foo bar,\n", "foo bar");
    assertParseHeaderLine("foo,bar,\n" ,"foo,bar");
    assertParseHeaderLine("foo|bar,\n", "foo", "bar");
    assertParseHeaderLine("foo|bar juu,\n", "foo", "bar juu");
    assertParseHeaderLine("foo|bar,juu,\n", "foo", "bar,juu");
    assertParseHeaderLine("foo|bar|juu,\n", "foo", "bar", "juu");
    failParseHeaderLine("");
    failParseHeaderLine("foo,");
    failParseHeaderLine("foo|");
    failParseHeaderLine("foo|,");
  }

  private void assertParseHeaderLine(String s, String... expected) {
    try {
      TermInfoParser parser = new TermInfoParser(s);
      List<String> actual = parser.parseHeaderLine();
      assertEquals(Arrays.asList(expected), actual);
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
  }

  private void failParseHeaderLine(String s) {
    try {
      TermInfoParser parser = new TermInfoParser(s);
      parser.parseHeaderLine();
      fail();
    } catch (ParseException ignore) {
    } catch (TokenMgrError ignore) {
    }
  }

  @Test
  public void testParseFeatureLine() throws Exception {
    assertParseFeatureLine(" foo=bar\\\\,\n", Feature.create("foo", "bar\\"));
    assertParseFeatureLine(" foo,\n", Feature.create("foo", true));
    assertParseFeatureLine(" foo@,\n", Feature.create("foo", false));
    assertParseFeatureLine(" foo,bar,\n", Feature.create("foo", true), Feature.create("bar", true));
    assertParseFeatureLine(" foo, bar,\n", Feature.create("foo", true), Feature.create(" bar", true));
    assertParseFeatureLine(" foo=,\n", Feature.create("foo", ""));
    assertParseFeatureLine(" foo=\\s,\n", Feature.create("foo", " "));
    assertParseFeatureLine(" foo=\\\\,\n", Feature.create("foo", "\\"));
    assertParseFeatureLine(" foo=\\,,\n", Feature.create("foo", ","));
    assertParseFeatureLine(" foo=\\^,\n", Feature.create("foo", "^"));
    assertParseFeatureLine(" foo=^\\,\n", Feature.create("foo", "^\\"));
    assertParseFeatureLine(" foo=^^,\n", Feature.create("foo", "^^"));
    assertParseFeatureLine(" foo=bar,\n", Feature.create("foo", "bar"));
    assertParseFeatureLine(" foo=bar,juu=daa,\n", Feature.create("foo", "bar"), Feature.create("juu", "daa"));
    assertParseFeatureLine(" foo,bar=juu,\n", Feature.create("foo", true), Feature.create("bar", "juu"));
    assertParseFeatureLine(" foo=bar,juu,\n", Feature.create("foo", "bar"), Feature.create("juu", true));
    assertParseFeatureLine(" foo=b\\,ar,\n", Feature.create("foo", "b,ar"));
    assertParseFeatureLine(" foo#1234,\n", Feature.create("foo", 1234));
    assertParseFeatureLine(" foo#0,\n", Feature.create("foo", 0));
    assertParseFeatureLine(" foo#0x1234,\n", Feature.create("foo", 0x1234));
    assertParseFeatureLine(" foo#01234,\n", Feature.create("foo", 01234));
    failParseFeatureLine(" ");
    failParseFeatureLine(" foo,");
    failParseFeatureLine(" foo#,\n");
    failParseFeatureLine(" foo#a,\n");
  }


  @Test
  public void testStringSpecialCharsFeature() {
    String[] tests = {
        "\\a", String.valueOf((char) 7),
        "\\A", String.valueOf((char) 1),
        "\\b", String.valueOf((char) 8),
        "\\e", String.valueOf((char) 27),
        "\\E", String.valueOf((char) 27),
        "\\f", String.valueOf((char) 12),
        "\\n", String.valueOf('\n'),
        "\\r", String.valueOf('\r'),
        "\\s", String.valueOf(' '),
        "\\t", String.valueOf('\t'),
        "\\^", String.valueOf('^'),
        "\\\\", String.valueOf('\\'),
        "\\:", String.valueOf(':'),
        "\\0", String.valueOf((char) 0),
        "\\030", String.valueOf((char) 24),
        "\\101", String.valueOf((char) 65),
        "\\01ab", String.valueOf((char) 0) + "1ab",
    };
    for (int i = 0;i < tests.length;i += 2) {
      String info = " " + Capability.acs_chars.name + "=" + tests[i] + ",\n";
      assertParseFeatureLine(info, Feature.create(Capability.acs_chars.name, tests[i + 1]));
    }
  }

  private void assertParseFeatureLine(String s, Feature<?>... expected) {
    final List<Feature<?>> features = new ArrayList<>();
    try {
      TermInfoParser parser = new TermInfoParser(s);
      ParserHandler handler = new ParserHandler() {
        @Override
        public void addBooleanFeature(String name, boolean value) {
          features.add(Feature.create(name, value));
        }
        @Override
        public void addStringFeature(String name, String value) {
          features.add(Feature.create(name, value));
        }
        @Override
        public void addNumericFeature(String name, int value) {
          features.add(Feature.create(name, value));
        }
      };
      parser.parseFeatureLine(handler);
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
    assertEquals(Arrays.asList(expected), features);
  }

  private void failParseFeatureLine(String s) {
    try {
      TermInfoParser parser = new TermInfoParser(s);
      parser.parseFeatureLine();
      fail();
    } catch (ParseException ignore) {
    } catch (TokenMgrError ignore) {
    }
  }

  @Test
  public void testParseDevice() {
    assertParseDevice("abc|def,\n foo,bar,\n");
  }

  private void assertParseDevice(String s) {
    try {
      TermInfoParser parser = new TermInfoParser(s);
      parser.parseDevice();
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
  }

  @Test
  public void testParseDatabase() {
    assertParseDatabase("" +
            "# hello \n" +
            "abc|def,\n" +
            " foo,bar,\n" +
            "juu|daa,\n" +
            " bilto|bilta,\n"
    );
  }

  private void assertParseDatabase(String s) {
    try {
      TermInfoParser parser = new TermInfoParser(s);
      parser.parseDatabase();
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
  }

  @Test
  public void testUse() {
    TermInfo info = assertBuildDevices(
        "a,\n" +
        " bw,\n" +
        "b,\n" +
        " use=a,\n" +
        "c,\n" +
        " use=a,bw@,\n"
    );
    assertTrue(info.getDevice("a").getFeature(Capability.auto_left_margin));
    assertTrue(info.getDevice("b").getFeature(Capability.auto_left_margin));
    assertFalse(info.getDevice("c").getFeature(Capability.auto_left_margin));
  }

  @Test
  public void testUseCycle() throws Exception {
    TermInfoBuilder builder = new TermInfoBuilder();
    new TermInfoParser("" +
        "a,\n" +
        " use=b,\n" +
        "b,\n" +
        " use=c,\n" +
        "c,\n" +
        " use=a,\n"
    ).parseDatabase(builder);
    try {
      builder.build();
      fail();
    } catch (IllegalStateException ignore) {
    }
  }

  @Test
  public void testUseNotFound() throws Exception {
    TermInfoBuilder builder = new TermInfoBuilder();
    new TermInfoParser("" +
        "a,\n" +
        " use=b,\n"
    ).parseDatabase(builder);
    try {
      builder.build();
      fail();
    } catch (IllegalStateException ignore) {
    }
  }

  private TermInfo assertBuildDevices(String s) {
    try {
      TermInfoParser parser = new TermInfoParser(s);
      TermInfoBuilder builder = new TermInfoBuilder();
      parser.parseDatabase(builder);
      return builder.build();
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
  }

  @Test
  public void testParse() throws Exception {
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
    TermInfoParser parser = new TermInfoParser(s);
    TermInfoBuilder builder = new TermInfoBuilder();
    parser.parseDatabase(builder);
    TermInfo info = builder.build();
    Collection<Device> entries = info.getDevices();
    assertEquals(2645, entries.size());
    Device device = info.getDevice("xterm-color");
    assertNotNull(device);
  }
}
