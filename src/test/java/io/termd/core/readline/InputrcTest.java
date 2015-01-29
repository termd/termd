package io.termd.core.readline;

import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;

import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class InputrcTest {

  static class FailingParser extends InputrcParser {
    @Override public void bindMacro(String keyName, String macro) { throw new AssertionError(); }
    @Override public void bindFunction(String keyName, String functionName) { throw new AssertionError(); }
    @Override public void bindMacro(int[] keySequence, String macro) { throw new AssertionError(); }
    @Override public void bindFunction(int[] keySequence, String functionName) { throw new AssertionError(); }
  }

  static class AssertingParser extends InputrcParser {

    @Override public void bindMacro(String keyName, String macro) { nextParser().bindMacro(keyName, macro); }
    @Override public void bindFunction(String keyName, String functionName) { nextParser().bindFunction(keyName, functionName); }
    @Override public void bindMacro(int[] keySequence, String macro) { nextParser().bindMacro(keySequence, macro); }
    @Override public void bindFunction(int[] keySequence, String functionName) { nextParser().bindFunction(keySequence, functionName); }

    private FailingParser nextParser() {
      assertTrue(asserts.size() > 0);
      return asserts.removeFirst();
    }

    final LinkedList<FailingParser> asserts = new LinkedList<>();

    void parse(String s) throws UnsupportedEncodingException {
      InputrcParser.parse(s, this);
    }

    AssertingParser append(FailingParser next) {
      asserts.add(next);
      return this;
    }

    AssertingParser assertBindMacro(final String keyName, final String macro) {
      return append(new FailingParser() {
        @Override
        public void bindMacro(String k, String m) {
          Assert.assertEquals(k, keyName);
          Assert.assertEquals(m, macro);
        }
      });
    }

    AssertingParser assertBindMacro(final int[] keyName, final String macro) {
      return append(new FailingParser() {
        @Override
        public void bindMacro(int[] k, String m) {
          Assert.assertArrayEquals(k, keyName);
          Assert.assertEquals(m, macro);
        }
      });
    }

    AssertingParser assertBindFunction(final String keyName, final String macro) {
      return append (new FailingParser() {
        @Override
        public void bindFunction(String k, String m) {
          Assert.assertEquals(k, keyName);
          Assert.assertEquals(m, macro);
        }
      });
    }

    AssertingParser assertBindFunction(final int[] keySequence, final String macro) {
      return append(new FailingParser() {
        @Override
        public void bindFunction(int[] k, String m) {
          Assert.assertArrayEquals(k, keySequence);
          Assert.assertEquals(m, macro);
        }
      });
    }
  }

  @Test
  public void testFoo() throws UnsupportedEncodingException {

    new AssertingParser() {}.assertBindFunction("a", "b").parse("a:b");
    new AssertingParser() {}.assertBindMacro("a", "b").parse("a:\"b\"");
    new AssertingParser() {}.assertBindMacro("a", "b").parse("a:'b'");
    new AssertingParser() {}.assertBindFunction(new int[]{64}, "b").parse("\"@\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{0}, "b").parse("\"\\C-@\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{8}, "b").parse("\"\\C-H\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{8}, "b").parse("\"\\C-h\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{127}, "b").parse("\"\\C-?\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{27,0}, "b").parse("\"\\M-@\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{27,8}, "b").parse("\"\\M-H\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{27,8}, "b").parse("\"\\M-h\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{27,127}, "b").parse("\"\\M-?\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{27}, "b").parse("\"\\e\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{92}, "b").parse("\"\\\\\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{34}, "b").parse("\"\\\"\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{39}, "b").parse("\"\\'\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{7}, "b").parse("\"\\a\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{127}, "b").parse("\"\\d\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{12}, "b").parse("\"\\f\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{10}, "b").parse("\"\\n\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{13}, "b").parse("\"\\r\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{9}, "b").parse("\"\\t\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{11}, "b").parse("\"\\v\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{0}, "b").parse("\"\\0\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{0}, "b").parse("\"\\00\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{0}, "b").parse("\"\\000\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{255}, "b").parse("\"\\377\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{0}, "b").parse("\"\\x0\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{255}, "b").parse("\"\\xFF\":b");
    new AssertingParser() {}.assertBindFunction(new int[]{255}, "b").parse("\"\\xff\":b");

    // Bind \ to \
    new AssertingParser() {}.assertBindFunction(new int[]{27,'/'}, "\\").parse("\"\\e/\":\\");

/*
    InputrcParser.parse("#azefzef");
    InputrcParser.parse("$abc");
    InputrcParser.parse("set foo bar ");
    InputrcParser.parse("a:b");
    InputrcParser.parse("a: b");
    InputrcParser.parse("\"a\":b");
    InputrcParser.parse("\"a\": b");
    InputrcParser.parse("\"a\": 'b'");
    InputrcParser.parse("\"a\": \"b\"");
*/

  }

}
