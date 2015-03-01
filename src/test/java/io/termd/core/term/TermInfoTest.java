package io.termd.core.term;

import io.termd.core.util.Helper;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TermInfoTest {

  /**
   * Compare two terminal escape sequences.
   *
   * @param expected the expected sequence
   * @param actual the sequence
   */
  public static void assertSequenceEquals(String expected, String actual) {
    try {
      assertEquals(expected, actual);
    } catch (AssertionError e) {
      throw new AssertionError("Was expecting sequence <" + Helper.escape(actual) + "> to be equals to <"
          + Helper.escape(expected) + ">");
    }
  }

  @Test
  public void testParseComment() throws Exception {
    TermInfoParser parser = new TermInfoParser("#\n");
    parser.parseCommentLine();
  }

  @Test
  public void testParseHeaderLine() throws Exception {
    assertParseHeaderLine("foo,\n", "foo");
    assertParseHeaderLine("foo bar,\n", "foo bar");
    assertParseHeaderLine("foo,bar,\n", "foo,bar");
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
    assertFeatureLine(" foo=bar\\\\,\n", Feature.create("foo", new Sequence("bar\\")));
    assertFeatureLine(" foo,\n", Feature.create("foo", true));
    assertFeatureLine(" foo@,\n", Feature.create("foo", false));
    assertFeatureLine(" foo,bar,\n", Feature.create("foo", true), Feature.create("bar", true));
    assertFeatureLine(" foo, bar,\n", Feature.create("foo", true), Feature.create("bar", true));
    assertFeatureLine(" foo=,\n", Feature.create("foo", new Sequence(Collections.<OpCode>emptyList())));
    assertFeatureLine(" foo=\\s,\n", Feature.create("foo", new Sequence(" ")));
    assertFeatureLine(" foo=\\\\,\n", Feature.create("foo", new Sequence("\\")));
    assertFeatureLine(" foo=\\,,\n", Feature.create("foo", new Sequence(",")));
    assertFeatureLine(" foo=\\^,\n", Feature.create("foo", new Sequence("^")));
    assertFeatureLine(" foo=^\\,\n", Feature.create("foo", new Sequence(Character.toString((char) 28))));
    assertFeatureLine(" foo=^^,\n", Feature.create("foo", new Sequence(Character.toString((char) 30))));
    assertFeatureLine(" foo=bar,\n", Feature.create("foo", new Sequence("bar")));
    assertFeatureLine(" foo=bar,juu=daa,\n", Feature.create("foo", new Sequence("bar")), Feature.create("juu", new Sequence("daa")));
    assertFeatureLine(" foo=bar, juu=daa,\n", Feature.create("foo", new Sequence("bar")), Feature.create("juu", new Sequence("daa")));
    assertFeatureLine(" foo,bar=juu,\n", Feature.create("foo", true), Feature.create("bar", new Sequence("juu")));
    assertFeatureLine(" foo=bar,juu,\n", Feature.create("foo", new Sequence("bar")), Feature.create("juu", true));
    assertFeatureLine(" foo=b\\,ar,\n", Feature.create("foo", new Sequence("b,ar")));
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
    assertParseInstr("%p1", new OpCode.PushParam(1));
    assertParseInstr("%p2", new OpCode.PushParam(2));
    assertParseInstr("%p3", new OpCode.PushParam(3));
    assertParseInstr("%p4", new OpCode.PushParam(4));
    assertParseInstr("%p5", new OpCode.PushParam(5));
    assertParseInstr("%p6", new OpCode.PushParam(6));
    assertParseInstr("%p7", new OpCode.PushParam(7));
    assertParseInstr("%p8", new OpCode.PushParam(8));
    assertParseInstr("%p9", new OpCode.PushParam(9));
  }

  @Test
  public void testOpIntegerConstant() {
    assertParseInstr("%{0}", new OpCode.PushConstant(0, true));
    assertParseInstr("%{1}", new OpCode.PushConstant(1, true));
    assertParseInstr("%{01}", new OpCode.PushConstant(1, true));
    assertParseInstr("%{2}", new OpCode.PushConstant(2, true));
    assertParseInstr("%{10}", new OpCode.PushConstant(10, true));
    assertParseInstr("%{11}", new OpCode.PushConstant(11, true));
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
    assertParseInstr("%Pa", new OpCode.SetPopVar('a'));
    assertParseInstr("%Pz", new OpCode.SetPopVar('z'));
    assertParseInstr("%PA", new OpCode.SetPopVar('A'));
    assertParseInstr("%PZ", new OpCode.SetPopVar('Z'));
    assertParseInstr("%ga", new OpCode.GetPushVar('a'));
    assertParseInstr("%gz", new OpCode.GetPushVar('z'));
    assertParseInstr("%gA", new OpCode.GetPushVar('A'));
    assertParseInstr("%gZ", new OpCode.GetPushVar('Z'));
  }

  @Test
  public void testOpBit() {
    assertParseInstr("%&", OpCode.Bit.AND);
    assertParseInstr("%|", OpCode.Bit.OR);
    assertParseInstr("%^", OpCode.Bit.XOR);
  }

  @Test
  public void testOpLogical() {
    assertParseInstr("%=", OpCode.Logical.EQ);
    assertParseInstr("%>", OpCode.Logical.GT);
    assertParseInstr("%<", OpCode.Logical.LT);
    assertParseInstr("%A", OpCode.Logical.AND);
    assertParseInstr("%O", OpCode.Logical.OR);
    assertParseInstr("%!", OpCode.Logical.NEG);
    assertParseInstr("%~", OpCode.Logical.NEG);
  }

  @Test
  public void testOpArithmetic() {
    assertParseInstr("%+", OpCode.Arithmetic.PLUS);
    assertParseInstr("%-", OpCode.Arithmetic.MINUS);
    assertParseInstr("%*", OpCode.Arithmetic.MUL);
    assertParseInstr("%/", OpCode.Arithmetic.DIV);
    assertParseInstr("%m", OpCode.Arithmetic.MOD);
  }

  @Test
  public void testOpAdd1ToParams() {
    assertParseInstr("%i", OpCode.Add1ToParams.INSTANCE);
  }

  @Test
  public void testIfThenElse() {
    List<OpCode> p1 = Arrays.<OpCode>asList(new OpCode.PushParam(1));
    List<OpCode> p2 = Arrays.<OpCode>asList(new OpCode.PushParam(2));
    List<OpCode> p3 = Arrays.<OpCode>asList(new OpCode.PushParam(3));
    List<OpCode> p4 = Arrays.<OpCode>asList(new OpCode.PushParam(4));
    List<OpCode> p5 = Arrays.<OpCode>asList(new OpCode.PushParam(5));
    List<OpCode> p6 = Arrays.<OpCode>asList(new OpCode.PushParam(6));
    List<OpCode> p7 = Arrays.<OpCode>asList(new OpCode.PushParam(7));
    assertParseInstr("%?%p1%t%p2%;", new OpCode.If(p1, new OpCode.Then(p2)));
    assertParseInstr("%?%p1%t%p2%e%p3%;", new OpCode.If(p1, new OpCode.Then(p2, new OpCode.Else(p3))));
    assertParseInstr("%?%p1%t%p2%e%p3%t%p4%;", new OpCode.If(p1, new OpCode.Then(p2, new OpCode.If(p3, new OpCode.Then(p4)))));
    assertParseInstr("%?%p1%t%p2%e%p3%t%p4%e%p5%;", new OpCode.If(p1, new OpCode.Then(p2, new OpCode.If(p3, new OpCode.Then(p4, new OpCode.Else(p5))))));
    assertParseInstr("%?%p1%t%p2%e%p3%t%p4%e%p5%t%p6%;", new OpCode.If(p1, new OpCode.Then(p2, new OpCode.If(p3, new OpCode.Then(p4, new OpCode.If(p5, new OpCode.Then(p6)))))));
    assertParseInstr("%?%p1%t%p2%e%p3%t%p4%e%p5%t%p6%e%p7%;", new OpCode.If(p1, new OpCode.Then(p2, new OpCode.If(p3, new OpCode.Then(p4, new OpCode.If(p5, new OpCode.Then(p6, new OpCode.Else(p7))))))));
    List<OpCode> p1p2 = Arrays.<OpCode>asList(new OpCode.PushParam(1), new OpCode.PushParam(2));
    List<OpCode> p3p4 = Arrays.<OpCode>asList(new OpCode.PushParam(3), new OpCode.PushParam(4));
    List<OpCode> p5p6 = Arrays.<OpCode>asList(new OpCode.PushParam(5), new OpCode.PushParam(6));
    assertParseInstr("%?%p1%p2%t%p3%p4%e%p5%p6%;", new OpCode.If(p1p2, new OpCode.Then(p3p4, new OpCode.Else(p5p6))));
  }

  @Test
  public void testOpEsc() {
    assertParseInstr("%%", OpCode.Percent.INSTANCE);
  }

  @Test
  public void testOpStrLen() {
    assertParseInstr("%l", OpCode.PushStrLen.INSTANCE);
  }

  @Test
  public void testOpPushConstant() {
    assertParseInstr("%'a'", new OpCode.PushConstant('a', false));
    assertParseInstr("%'\''", new OpCode.PushConstant('\'', false));
    assertParseInstr("%'\\123'", new OpCode.PushConstant((char) 83, false));
    assertParseInstr("%'\\0'", new OpCode.PushConstant(0, false));
  }

  @Test
  public void testOpPrintPop() {
    assertParseInstr("%c", OpCode.PrintPop.c);
    assertParseInstr("%s", OpCode.PrintPop.s);
  }

  private void assertParseOpPrintf(String s, Character flag, String width, String precision, Character specifier) {
    assertParseInstr(s, new OpCode.Printf(flag, width, precision, specifier));
  }

  private void assertParseInstr(String s, OpCode expected) {
    try {
      OpCode op = new TermInfoParser(s).parseSingleOpCode();
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
      assertFeatureLine(info, Feature.create(Capability.acs_chars.name, new Sequence(tests[i + 1])));
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
        public void addStringFeature(String name, Sequence value) {
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

  @Test
  public void testSomeEval() throws Exception {
    Sequence cup = TermInfo.getDefault().getDevice("xterm-color").getFeature(Capability.cursor_address);
    Sequence smcup = TermInfo.getDefault().getDevice("xterm-color").getFeature(Capability.enter_ca_mode);
    assertSequenceEquals("\033[1;1H", cup.eval("0", "0"));
    assertSequenceEquals("\0337\033[?47h", smcup.eval());
    Sequence setb = TermInfo.getDefault().getDevice("xtermc").getFeature(Capability.set_background);
    assertSequenceEquals("\033[44m", setb.eval("1"));
    assertSequenceEquals("\033[42m", setb.eval("2"));
    assertSequenceEquals("\033[46m", setb.eval("3"));
    assertSequenceEquals("\033[41m", setb.eval("4"));
    assertSequenceEquals("\033[45m", setb.eval("5"));
    assertSequenceEquals("\033[43m", setb.eval("6"));
    assertSequenceEquals("\033[47m", setb.eval("7"));
    assertSequenceEquals("\033[412m", setb.eval("12"));
  }

  @Test
  public void testEvalPushParam() throws Exception {
    for (int i = 0;i < 9;i++) {
      OpCode.PushParam opcode = new OpCode.PushParam(1 + i);
      String[] parameters = new String[1 + i];
      parameters[i] = "10";
      EvalContext context = new EvalContext(parameters);
      opcode.eval(context);
      assertEquals(Arrays.asList("10"), context.stack);
      assertEquals("", context.result.toString());
    }
  }

  @Test
  public void testEvalPushParamInvalidIndex() throws Exception {
    for (int index : Arrays.asList(0, 10)) {
      try {
        new OpCode.PushParam(index);
        fail();
      } catch (IllegalArgumentException ignore) {
      }
    }
  }

  @Test
  public void testEvalPushParamNoParam() throws Exception {
    EvalContext context = new EvalContext();
    OpCode.PushParam opcode = new OpCode.PushParam(1);
    try {
      opcode.eval(context);
      fail();
    } catch (IllegalArgumentException ignore) {
      assertEquals(Arrays.<String>asList(), context.stack);
      assertEquals("", context.result.toString());
    }
  }

  @Test
  public void testEvalPushConstant() throws Exception {
    OpCode.PushConstant opcode = new OpCode.PushConstant(65, false);
    EvalContext context = new EvalContext();
    opcode.eval(context);
    assertEquals(Arrays.asList("A"), context.stack);
    assertEquals("", context.result.toString());
  }

  @Test
  public void testEvalLiteral() throws Exception {
    for (String test : Arrays.asList("a", "\r\n", "\033")) {
      OpCode.Literal opcode = new OpCode.Literal(test);
      EvalContext context = new EvalContext();
      opcode.eval(context);
      assertEquals(Arrays.<String>asList(), context.stack);
      assertEquals(test, context.result.toString());
    }
  }

  @Test
  public void testEvalLogicalEq() {
    EvalContext context = new EvalContext().push("0").push("0");
    OpCode.Logical.EQ.eval(context);
    assertEquals("1", context.pop());
    context = new EvalContext().push("0").push("1");
    OpCode.Logical.EQ.eval(context);
    assertEquals("0", context.pop());
    context = new EvalContext().push("1").push("1");
    OpCode.Logical.EQ.eval(context);
    assertEquals("1", context.pop());
    context = new EvalContext().push("150").push("150");
    OpCode.Logical.EQ.eval(context);
    assertEquals("1", context.pop());
    context = new EvalContext().push("150").push("4");
    OpCode.Logical.EQ.eval(context);
    assertEquals("0", context.pop());
  }

  @Test
  public void testEvalIfThenElse() throws Exception {
    String[][] tests = {
        { "%?%{0}%tA%;", "" },
        { "%?%{1}%tA%;", "A" },
        { "%?%{0}%tA%eB%;", "B" },
        { "%?%{1}%tA%eB%;", "A" },
        { "%?%{0}%tA%e%{1}%tB%;", "B" },
        { "%?%{1}%tA%e%{1}%tB%;", "A" },
        { "%?%{0}%tA%e%{0}%tB%;", "" },
        { "%?%{1}%tA%e%{0}%tB%;", "A" },
        { "%?%{0}%tA%e%{1}%tB%eC%;", "B" },
        { "%?%{1}%tA%e%{1}%tB%eC%;", "A" },
        { "%?%{0}%tA%e%{0}%tB%eC%;", "C" },
        { "%?%{1}%tA%e%{0}%tB%eC%;", "A" },
    };
    for (String[] test : tests) {
      StringBuilder result = new StringBuilder();
      OpCode opCode = new TermInfoParser(test[0]).parseSingleOpCode();
      opCode.eval(new EvalContext(new String[0], result)) ;
      assertEquals(test[1], result.toString());
    }
  }
}
