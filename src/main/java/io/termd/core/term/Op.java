package io.termd.core.term;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Op {

  public static class PushParam extends Op {

    private final int index;

    public PushParam(int index) {
      this.index = index;
    }

    public int getIndex() {
      return index;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof PushParam) {
        PushParam that = (PushParam) obj;
        return index == that.index;
      }
      return false;
    }
  }

  // %'c'
  public static class CharConstant extends Op {
    private final char value;
    public CharConstant(char value) {
      this.value = value;
    }
    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof CharConstant) {
        CharConstant that = (CharConstant) obj;
        return value == that.value;
      }
      return false;
    }
  }

  // %%
  public static class Esc extends Op {
    public static final Esc INSTANCE = new Esc();
    private Esc() {
    }
  }

  // %%
  public static class StrLen extends Op {
    public static final StrLen INSTANCE = new StrLen();
    private StrLen() {
    }
  }

  // %c
  // %s
  public static class PrintPop extends Op {
    public static final PrintPop c = new PrintPop();
    public static final PrintPop s = new PrintPop();
    private PrintPop() {
    }
  }

  public static class SetPopVar extends Op {
    private final char value;
    public SetPopVar(char value) {
      this.value = value;
    }
    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof SetPopVar) {
        SetPopVar that = (SetPopVar) obj;
        return value == that.value;
      }
      return false;
    }
  }

  public static class GetPushVar extends Op {
    private final char value;
    public GetPushVar(char value) {
      this.value = value;
    }
    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof GetPushVar) {
        GetPushVar that = (GetPushVar) obj;
        return value == that.value;
      }
      return false;
    }
  }

  public static class Expr extends Op {

    public static final Expr IF = new Expr("IF");
    public static final Expr THEN = new Expr("EXPR");
    public static final Expr ELSE = new Expr("ELSE");
    public static final Expr FI = new Expr("FI");

    final String name;

    private Expr(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return "Op.Expr[" + name + "]";
    }
  }

  public static class Bit extends Op {

    public static final Bit OR = new Bit("OR");
    public static final Bit AND = new Bit("AND");
    public static final Bit XOR = new Bit("XOR");

    final String name;

    private Bit(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return "Op.Bit[" + name + "]";
    }
  }

  public static class Logical extends Op {

    public static final Logical EQ = new Logical("EQ");
    public static final Logical GT = new Logical("GT");
    public static final Logical LT = new Logical("LT");
    public static final Logical AND = new Logical("AND");
    public static final Logical OR = new Logical("OR");
    public static final Logical NEG = new Logical("NEG");

    final String name;

    private Logical(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return "Op.Logical[" + name + "]";
    }
  }

  public static class Arithmetic extends Op {

    public static final Arithmetic PLUS = new Arithmetic("PLUS");
    public static final Arithmetic MINUS = new Arithmetic("MINUS");
    public static final Arithmetic MUL = new Arithmetic("MUL");
    public static final Arithmetic DIV = new Arithmetic("DIV");
    public static final Arithmetic MOD = new Arithmetic("MOD");

    final String name;

    private Arithmetic(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return "Op.Arithmetic[" + name + "]";
    }
  }

  public static class IntegerConstant extends Op {

    private final int value;

    public IntegerConstant(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Op.IntegerConstant) {
        Op.IntegerConstant that = (IntegerConstant) obj;
        return value == that.value;
      }
      return false;
    }
  }

  public static class Printf extends Op {

    private static final Pattern p = Pattern.compile(":?([-+# ])?([0-9]+)?(?:\\.([0-9]+))?([doxXs])?");

    public static Printf parse(String s) {
      Matcher m = p.matcher(s);
      if (!m.matches()) {
        throw new IllegalArgumentException("Invalid printf pattern " + s);
      }
      String flag = m.group(1);
      String width = m.group(2);
      String precision = m.group(3);
      String specifier = m.group(4);
      return new Printf(flag != null ? flag.charAt(0) : null, width, precision, specifier != null ? specifier.charAt(0) : null);
    }

    private final Character flag;
    private final String width;
    private final String precision;
    private final Character specifier;

    public Printf(Character flag, String width, String precision, Character specifier) {
      this.flag = flag;
      this.width = width;
      this.precision = precision;
      this.specifier = specifier;
    }

    public Character getFlag() {
      return flag;
    }

    public String getWidth() {
      return width;
    }

    public String getPrecision() {
      return precision;
    }

    public Character getSpecifier() {
      return specifier;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Op.Printf) {
        Op.Printf that = (Printf) obj;
        return
            (flag == null ? that.flag == null : flag.equals(that.flag)) &&
            (width == null ? that.width == null : width.equals(that.width)) &&
            (precision == null ? that.precision == null : precision.equals(that.precision)) &&
            (specifier == null ? that.specifier == null : specifier.equals(that.specifier));
      }
      return false;
    }

    @Override
    public String toString() {
      return "Op.Printf[flag=" + flag + ",width=" + width + ",precision=" + precision + ",specified=" + specifier + "]";
    }
  }

}
