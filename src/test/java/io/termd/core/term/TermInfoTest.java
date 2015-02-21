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
    assertFeatureLine(" foo=bar\\\\,\n", Feature.create("foo", "bar\\"));
    assertFeatureLine(" foo,\n", Feature.create("foo", true));
    assertFeatureLine(" foo@,\n", Feature.create("foo", false));
    assertFeatureLine(" foo,bar,\n", Feature.create("foo", true), Feature.create("bar", true));
    assertFeatureLine(" foo, bar,\n", Feature.create("foo", true), Feature.create("bar", true));
    assertFeatureLine(" foo=,\n", Feature.create("foo", ""));
    assertFeatureLine(" foo=\\s,\n", Feature.create("foo", " "));
    assertFeatureLine(" foo=\\\\,\n", Feature.create("foo", "\\"));
    assertFeatureLine(" foo=\\,,\n", Feature.create("foo", ","));
    assertFeatureLine(" foo=\\^,\n", Feature.create("foo", "^"));
    assertFeatureLine(" foo=^\\,\n", Feature.create("foo", Character.toString((char) 28)));
    assertFeatureLine(" foo=^^,\n", Feature.create("foo", Character.toString((char) 30)));
    assertFeatureLine(" foo=bar,\n", Feature.create("foo", "bar"));
    assertFeatureLine(" foo=bar,juu=daa,\n", Feature.create("foo", "bar"), Feature.create("juu", "daa"));
    assertFeatureLine(" foo=bar, juu=daa,\n", Feature.create("foo", "bar"), Feature.create("juu", "daa"));
    assertFeatureLine(" foo,bar=juu,\n", Feature.create("foo", true), Feature.create("bar", "juu"));
    assertFeatureLine(" foo=bar,juu,\n", Feature.create("foo", "bar"), Feature.create("juu", true));
    assertFeatureLine(" foo=b\\,ar,\n", Feature.create("foo", "b,ar"));
    assertFeatureLine(" foo#1234,\n", Feature.create("foo", 1234));
    assertFeatureLine(" foo#0,\n", Feature.create("foo", 0));
    assertFeatureLine(" foo#0x1234,\n", Feature.create("foo", 0x1234));
    assertFeatureLine(" foo#01234,\n", Feature.create("foo", 01234));
    failParseFeatureLine(" ");
    failParseFeatureLine(" foo,");
    failParseFeatureLine(" foo#,\n");
    failParseFeatureLine(" foo#a,\n");
  }

  @Test
  public void testStringParameterized() {
    String[] tests = {
        // Parameters
        "%p1",
        "%p2",
        "%p3",
        "%p4",
        "%p5",
        "%p6",
        "%p7",
        "%p7",
        "%p8",
        "%p9",
        // Printf
        "%d",
        "%o",
        "%x",
        "%X",
        "%s",
        "%+d",
        "%#d",
        "% d",
        //
//        "%{1}",
    };
    for (int i = 0;i < tests.length;i += 1) {
      String info = " " + Capability.acs_chars.name + "=" + tests[i] + ",\n";
      assertParseFeatureLine(info);
    }
  }

  @Test
  public void testOpParam() {
    assertParseOp("p1", new Op.PushParam(1));
    assertParseOp("p2", new Op.PushParam(2));
    assertParseOp("p3", new Op.PushParam(3));
    assertParseOp("p4", new Op.PushParam(4));
    assertParseOp("p5", new Op.PushParam(5));
    assertParseOp("p6", new Op.PushParam(6));
    assertParseOp("p7", new Op.PushParam(7));
    assertParseOp("p8", new Op.PushParam(8));
    assertParseOp("p9", new Op.PushParam(9));
  }

  @Test
  public void testOpIntegerConstant() {
    assertParseOp("{0}", new Op.IntegerConstant(0));
    assertParseOp("{1}", new Op.IntegerConstant(1));
    assertParseOp("{01}", new Op.IntegerConstant(1));
    assertParseOp("{2}", new Op.IntegerConstant(2));
    assertParseOp("{10}", new Op.IntegerConstant(10));
    assertParseOp("{11}", new Op.IntegerConstant(11));
  }

  @Test
  public void testOpPrintf() throws ParseException {

    // Specifier
    assertParseOpPrintf("d", null, null, null, 'd');
    assertParseOpPrintf("o", null, null, null, 'o');
    assertParseOpPrintf("x", null, null, null, 'x');
    assertParseOpPrintf("X", null, null, null, 'X');

    // Flags
    assertParseOpPrintf("#", '#', null, null, null);
    assertParseOpPrintf(" ", ' ', null, null, null);
    assertParseOpPrintf(":#", '#', null, null, null);
    assertParseOpPrintf(":-", '-', null, null, null);
    assertParseOpPrintf(":+", '+', null, null, null);
    assertParseOpPrintf(": ", ' ', null, null, null);

    // Width
    assertParseOpPrintf("0", null, "0", null, null);
    assertParseOpPrintf("10", null, "10", null, null);

    // Precision
    assertParseOpPrintf(".0", null, null, "0", null);
    assertParseOpPrintf(".10", null, null, "10", null);

    // Various
    assertParseOpPrintf(" 10", ' ', "10", null, null);
    assertParseOpPrintf(":-16s", '-', "16", null, 's');
    assertParseOpPrintf(":-16.16s", '-', "16", "16", 's');
    assertParseOpPrintf("03d", null, "03", null, 'd');
  }

  @Test
  public void testOpVariable() {
    assertParseOp("Pa", new Op.SetPopVar('a'));
    assertParseOp("Pz", new Op.SetPopVar('z'));
    assertParseOp("PA", new Op.SetPopVar('A'));
    assertParseOp("PZ", new Op.SetPopVar('Z'));
    assertParseOp("ga", new Op.GetPushVar('a'));
    assertParseOp("gz", new Op.GetPushVar('z'));
    assertParseOp("gA", new Op.GetPushVar('A'));
    assertParseOp("gZ", new Op.GetPushVar('Z'));
  }

  @Test
  public void testOpBit() {
    assertParseOp("&", Op.Bit.AND);
    assertParseOp("|", Op.Bit.OR);
    assertParseOp("^", Op.Bit.XOR);
  }

  @Test
  public void testOpLogical() {
    assertParseOp("=", Op.Logical.EQ);
    assertParseOp(">", Op.Logical.GT);
    assertParseOp("<", Op.Logical.LT);
    assertParseOp("A", Op.Logical.AND);
    assertParseOp("O", Op.Logical.OR);
    assertParseOp("!", Op.Logical.NEG);
    assertParseOp("~", Op.Logical.NEG);
  }

  @Test
  public void testOpArithmetic() {
    assertParseOp("+", Op.Arithmetic.PLUS);
    assertParseOp("-", Op.Arithmetic.MINUS);
    assertParseOp("*", Op.Arithmetic.MUL);
    assertParseOp("/", Op.Arithmetic.DIV);
    assertParseOp("m", Op.Arithmetic.MOD);
  }

  @Test
  public void testOpExpr() {
    assertParseOp("?", Op.Expr.IF);
    assertParseOp("t", Op.Expr.THEN);
    assertParseOp("e", Op.Expr.ELSE);
    assertParseOp(";", Op.Expr.FI);
  }

  @Test
  public void testOpEsc() {
    assertParseOp("%", Op.Esc.INSTANCE);
  }

  @Test
  public void testOpStrLen() {
    assertParseOp("l", Op.StrLen.INSTANCE);
  }

  @Test
  public void testOpCharConstant() {
    assertParseOp("'a'", new Op.CharConstant('a'));
    assertParseOp("'%'", new Op.CharConstant('%'));
    assertParseOp("'\''", new Op.CharConstant('\''));
  }

  @Test
  public void testOpPrintPop() {
    assertParseOp("c", Op.PrintPop.c);
    assertParseOp("s", Op.PrintPop.s);
  }

  private void assertParseOpPrintf(String s, Character flag, String width, String precision, Character specifier) {
    assertParseOp(s, new Op.Printf(flag, width, precision, specifier));
  }

  private void assertParseOp(String s, Op expected) {
    try {
      Op op = new TermInfoParser(s).parseOp();
      assertEquals(expected, op);
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
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
        "^c", String.valueOf((char)('C' - 64)),
    };
    for (int i = 0;i < tests.length;i += 2) {
      String info = " " + Capability.acs_chars.name + "=" + tests[i] + ",\n";
      assertFeatureLine(info, Feature.create(Capability.acs_chars.name, tests[i + 1]));
    }
  }

  private void assertFeatureLine(String s, Feature<?>... expected) {
    List<Feature<?>> features = assertParseFeatureLine(s);
    assertEquals(Arrays.asList(expected), features);
  }

  private List<Feature<?>> assertParseFeatureLine(String s) {
    try {
      final List<Feature<?>> features = new ArrayList<>();
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
      return features;
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
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
