package io.termd.core.term;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
    assertFeatureLine(" foo=bar\\\\,\n", Feature.create("foo", new StringValue("bar\\")));
    assertFeatureLine(" foo,\n", Feature.create("foo", true));
    assertFeatureLine(" foo@,\n", Feature.create("foo", false));
    assertFeatureLine(" foo,bar,\n", Feature.create("foo", true), Feature.create("bar", true));
    assertFeatureLine(" foo, bar,\n", Feature.create("foo", true), Feature.create("bar", true));
    assertFeatureLine(" foo=,\n", Feature.create("foo", new StringValue(Collections.<Op>emptyList())));
    assertFeatureLine(" foo=\\s,\n", Feature.create("foo", new StringValue(" ")));
    assertFeatureLine(" foo=\\\\,\n", Feature.create("foo", new StringValue("\\")));
    assertFeatureLine(" foo=\\,,\n", Feature.create("foo", new StringValue(",")));
    assertFeatureLine(" foo=\\^,\n", Feature.create("foo", new StringValue("^")));
    assertFeatureLine(" foo=^\\,\n", Feature.create("foo", new StringValue(Character.toString((char) 28))));
    assertFeatureLine(" foo=^^,\n", Feature.create("foo", new StringValue(Character.toString((char) 30))));
    assertFeatureLine(" foo=bar,\n", Feature.create("foo", new StringValue("bar")));
    assertFeatureLine(" foo=bar,juu=daa,\n", Feature.create("foo", new StringValue("bar")), Feature.create("juu", new StringValue("daa")));
    assertFeatureLine(" foo=bar, juu=daa,\n", Feature.create("foo", new StringValue("bar")), Feature.create("juu", new StringValue("daa")));
    assertFeatureLine(" foo,bar=juu,\n", Feature.create("foo", true), Feature.create("bar", new StringValue("juu")));
    assertFeatureLine(" foo=bar,juu,\n", Feature.create("foo", new StringValue("bar")), Feature.create("juu", true));
    assertFeatureLine(" foo=b\\,ar,\n", Feature.create("foo", new StringValue("b,ar")));
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
/*
    for (int i = 0;i < tests.length;i += 1) {
      String info = " " + Capability.acs_chars.name + "=" + tests[i] + ",\n";
      assertParseFeatureLine(info);
    }
*/
  }

  @Test
  public void testOpParam() {
    assertParseInstr("%p1", new Op.PushParam(1));
    assertParseInstr("%p2", new Op.PushParam(2));
    assertParseInstr("%p3", new Op.PushParam(3));
    assertParseInstr("%p4", new Op.PushParam(4));
    assertParseInstr("%p5", new Op.PushParam(5));
    assertParseInstr("%p6", new Op.PushParam(6));
    assertParseInstr("%p7", new Op.PushParam(7));
    assertParseInstr("%p8", new Op.PushParam(8));
    assertParseInstr("%p9", new Op.PushParam(9));
  }

  @Test
  public void testOpIntegerConstant() {
    assertParseInstr("%{0}", new Op.IntegerConstant(0));
    assertParseInstr("%{1}", new Op.IntegerConstant(1));
    assertParseInstr("%{01}", new Op.IntegerConstant(1));
    assertParseInstr("%{2}", new Op.IntegerConstant(2));
    assertParseInstr("%{10}", new Op.IntegerConstant(10));
    assertParseInstr("%{11}", new Op.IntegerConstant(11));
  }

  @Test
  public void testOpPrintf() throws ParseException {

    // Specifier
    assertParseOpPrintf("%d", null, null, null, 'd');
    assertParseOpPrintf("%o", null, null, null, 'o');
    assertParseOpPrintf("%x", null, null, null, 'x');
    assertParseOpPrintf("%X", null, null, null, 'X');

    // Flags
    assertParseOpPrintf("%#", '#', null, null, null);
    assertParseOpPrintf("% ", ' ', null, null, null);
    assertParseOpPrintf("%:#", '#', null, null, null);
    assertParseOpPrintf("%:-", '-', null, null, null);
    assertParseOpPrintf("%:+", '+', null, null, null);
    assertParseOpPrintf("%: ", ' ', null, null, null);

    // Width
    assertParseOpPrintf("%0", null, "0", null, null);
    assertParseOpPrintf("%10", null, "10", null, null);

    // Precision
    assertParseOpPrintf("%.0", null, null, "0", null);
    assertParseOpPrintf("%.10", null, null, "10", null);

    // Various
    assertParseOpPrintf("% 10", ' ', "10", null, null);
    assertParseOpPrintf("%:-16s", '-', "16", null, 's');
    assertParseOpPrintf("%:-16.16s", '-', "16", "16", 's');
    assertParseOpPrintf("%03d", null, "03", null, 'd');
  }

  @Test
  public void testOpVariable() {
    assertParseInstr("%Pa", new Op.SetPopVar('a'));
    assertParseInstr("%Pz", new Op.SetPopVar('z'));
    assertParseInstr("%PA", new Op.SetPopVar('A'));
    assertParseInstr("%PZ", new Op.SetPopVar('Z'));
    assertParseInstr("%ga", new Op.GetPushVar('a'));
    assertParseInstr("%gz", new Op.GetPushVar('z'));
    assertParseInstr("%gA", new Op.GetPushVar('A'));
    assertParseInstr("%gZ", new Op.GetPushVar('Z'));
  }

  @Test
  public void testOpBit() {
    assertParseInstr("%&", Op.Bit.AND);
    assertParseInstr("%|", Op.Bit.OR);
    assertParseInstr("%^", Op.Bit.XOR);
  }

  @Test
  public void testOpLogical() {
    assertParseInstr("%=", Op.Logical.EQ);
    assertParseInstr("%>", Op.Logical.GT);
    assertParseInstr("%<", Op.Logical.LT);
    assertParseInstr("%A", Op.Logical.AND);
    assertParseInstr("%O", Op.Logical.OR);
    assertParseInstr("%!", Op.Logical.NEG);
    assertParseInstr("%~", Op.Logical.NEG);
  }

  @Test
  public void testOpArithmetic() {
    assertParseInstr("%+", Op.Arithmetic.PLUS);
    assertParseInstr("%-", Op.Arithmetic.MINUS);
    assertParseInstr("%*", Op.Arithmetic.MUL);
    assertParseInstr("%/", Op.Arithmetic.DIV);
    assertParseInstr("%m", Op.Arithmetic.MOD);
  }

  @Test
  public void testIfThenElse() {
    List<Op> p1 = Arrays.<Op>asList(new Op.PushParam(1));
    List<Op> p2 = Arrays.<Op>asList(new Op.PushParam(2));
    List<Op> p3 = Arrays.<Op>asList(new Op.PushParam(3));
    List<Op> p4 = Arrays.<Op>asList(new Op.PushParam(4));
    List<Op> p5 = Arrays.<Op>asList(new Op.PushParam(5));
    List<Op> p6 = Arrays.<Op>asList(new Op.PushParam(6));
    List<Op> p7 = Arrays.<Op>asList(new Op.PushParam(7));
    assertParseInstr("%?%p1%t%p2%;", new Op.If(p1, new Op.Then(p2)));
    assertParseInstr("%?%p1%t%p2%e%p3%;", new Op.If(p1, new Op.Then(p2, new Op.Else(p3))));
    assertParseInstr("%?%p1%t%p2%e%p3%t%p4%;", new Op.If(p1, new Op.Then(p2, new Op.If(p3, new Op.Then(p4)))));
    assertParseInstr("%?%p1%t%p2%e%p3%t%p4%e%p5%;", new Op.If(p1, new Op.Then(p2, new Op.If(p3, new Op.Then(p4, new Op.Else(p5))))));
    assertParseInstr("%?%p1%t%p2%e%p3%t%p4%e%p5%t%p6%;", new Op.If(p1, new Op.Then(p2, new Op.If(p3, new Op.Then(p4, new Op.If(p5, new Op.Then(p6)))))));
    assertParseInstr("%?%p1%t%p2%e%p3%t%p4%e%p5%t%p6%e%p7%;", new Op.If(p1, new Op.Then(p2, new Op.If(p3, new Op.Then(p4, new Op.If(p5, new Op.Then(p6, new Op.Else(p7))))))));
    List<Op> p1p2 = Arrays.<Op>asList(new Op.PushParam(1), new Op.PushParam(2));
    List<Op> p3p4 = Arrays.<Op>asList(new Op.PushParam(3), new Op.PushParam(4));
    List<Op> p5p6 = Arrays.<Op>asList(new Op.PushParam(5), new Op.PushParam(6));
    assertParseInstr("%?%p1%p2%t%p3%p4%e%p5%p6%;", new Op.If(p1p2, new Op.Then(p3p4, new Op.Else(p5p6))));
  }

  @Test
  public void testOpEsc() {
    assertParseInstr("%%", Op.Esc.INSTANCE);
  }

  @Test
  public void testOpStrLen() {
    assertParseInstr("%l", Op.PushStrLen.INSTANCE);
  }

  @Test
  public void testOpCharConstant() {
    assertParseInstr("%'a'", new Op.Constant("a"));
    assertParseInstr("%'\''", new Op.Constant("'"));
    assertParseInstr("%'\\123'", new Op.Constant(Character.toString((char) 83)));
    assertParseInstr("%'\\0'", new Op.Constant(Character.toString((char) 0)));
  }

  @Test
  public void testOpPrintPop() {
    assertParseInstr("%c", Op.PrintPop.c);
    assertParseInstr("%s", Op.PrintPop.s);
  }

  private void assertParseOpPrintf(String s, Character flag, String width, String precision, Character specifier) {
    assertParseInstr(s, new Op.Printf(flag, width, precision, specifier));
  }

  private void assertParseInstr(String s, Op expected) {
    try {
      Op op = new TermInfoParser(s).parseSingleOp();
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
      assertFeatureLine(info, Feature.create(Capability.acs_chars.name, new StringValue(tests[i + 1])));
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
        public void addStringFeature(String name, StringValue value) {
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
