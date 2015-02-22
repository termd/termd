package io.termd.core.term;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class OpCode {

  public String toString() {
    StringBuilder sb = new StringBuilder();
    toString(sb);
    return sb.toString();
  }

  protected abstract void toString(StringBuilder sb);

  public static class PushParam extends OpCode {

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

    @Override
    protected void toString(StringBuilder sb) {
      sb.append("%p").append(index);
    }
  }

  // %'c'
  public static class Constant extends OpCode {
    final String value;
    public Constant(String value) {
      this.value = value;
    }
    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Constant) {
        Constant that = (Constant) obj;
        return value.equals(that.value);
      }
      return false;
    }
    @Override
    protected void toString(StringBuilder sb) {
      sb.append(value);
    }
  }

  // %%
  public static class Esc extends OpCode {
    public static final Esc INSTANCE = new Esc();
    private Esc() {
    }

    @Override
    protected void toString(StringBuilder sb) {
      sb.append("%%");
    }
  }

  // %l
  public static class PushStrLen extends OpCode {
    public static final PushStrLen INSTANCE = new PushStrLen();
    private PushStrLen() {
    }
    @Override
    protected void toString(StringBuilder sb) {
      sb.append("%l");
    }
  }

  // %%
  public static class Add1ToParams extends OpCode {
    public static final Add1ToParams INSTANCE = new Add1ToParams();
    private Add1ToParams() {
    }
    @Override
    protected void toString(StringBuilder sb) {
      sb.append("%i");
    }
  }

  // %c
  // %s
  public abstract static class PrintPop extends OpCode {
    public static final PrintPop c = new PrintPop() {
      @Override
      protected void toString(StringBuilder sb) {
        sb.append("%c");
      }
    };
    public static final PrintPop s = new PrintPop() {
      @Override
      protected void toString(StringBuilder sb) {
        sb.append("%s");
      }
    };
    private PrintPop() {
    }
  }

  public static class SetPopVar extends OpCode {
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
    @Override
    protected void toString(StringBuilder sb) {
      sb.append("%P").append(value);
    }
  }

  public static class GetPushVar extends OpCode {
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
    @Override
    protected void toString(StringBuilder sb) {
      sb.append("%g").append(value);
    }
  }

  public static class Bit extends OpCode {

    public static final Bit OR = new Bit('|');
    public static final Bit AND = new Bit('&');
    public static final Bit XOR = new Bit('^');

    private final char value;

    private Bit(char value) {
      this.value = value;
    }

    @Override
    protected void toString(StringBuilder sb) {
      sb.append('%').append(value);
    }
  }

  public static class Logical extends OpCode {

    public static final Logical EQ = new Logical('=');
    public static final Logical GT = new Logical('>');
    public static final Logical LT = new Logical('<');
    public static final Logical AND = new Logical('A');
    public static final Logical OR = new Logical('O');
    public static final Logical NEG = new Logical('!');

    final char value;

    private Logical(char value) {
      this.value = value;
    }

    @Override
    protected void toString(StringBuilder sb) {
      sb.append('%').append(value);
    }
  }

  public static class Arithmetic extends OpCode {

    public static final Arithmetic PLUS = new Arithmetic('+');
    public static final Arithmetic MINUS = new Arithmetic('-');
    public static final Arithmetic MUL = new Arithmetic('*');
    public static final Arithmetic DIV = new Arithmetic('/');
    public static final Arithmetic MOD = new Arithmetic('m');

    final char value;

    private Arithmetic(char value) {
      this.value = value;
    }

    @Override
    protected void toString(StringBuilder sb) {
      sb.append('%').append(value);
    }
  }

  public static class IntegerConstant extends OpCode {

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
      if (obj instanceof OpCode.IntegerConstant) {
        OpCode.IntegerConstant that = (IntegerConstant) obj;
        return value == that.value;
      }
      return false;
    }

    @Override
    protected void toString(StringBuilder sb) {
      sb.append("%{").append(value).append("}");
    }
  }

  public static class Printf extends OpCode {

    private static final Pattern p = Pattern.compile("%:?([-+# ])?([0-9]+)?(?:\\.([0-9]+))?([doxXs])?");

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
      if (obj instanceof OpCode.Printf) {
        OpCode.Printf that = (Printf) obj;
        return
            (flag == null ? that.flag == null : flag.equals(that.flag)) &&
            (width == null ? that.width == null : width.equals(that.width)) &&
            (precision == null ? that.precision == null : precision.equals(that.precision)) &&
            (specifier == null ? that.specifier == null : specifier.equals(that.specifier));
      }
      return false;
    }

    @Override
    protected void toString(StringBuilder sb) {
      sb.append("%:");
      if (flag != null) {
        sb.append(flag);
      }
      if (width != null) {
        sb.append(width);
      }
      if (precision != null) {
        sb.append(".").append(precision);
      }
      if (specifier != null) {
        sb.append(specifier);
      }
    }
  }

  public static class If extends OpCode implements ElsePart {

    final List<OpCode> expr;
    final Then thenPart;

    public If(List<OpCode> expr, Then thenPart) {
      this.expr = expr;
      this.thenPart = thenPart;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof If) {
        If that = (If) obj;
        return expr.equals(that.expr) && thenPart.equals(that.thenPart);
      }
      return false;
    }

    @Override
    protected void toString(StringBuilder sb) {
      sb.append("%?");
      for (OpCode op : expr) {
        op.toString(sb);
      }
      thenPart.toString(sb);
      sb.append("%;");
    }
  }

  public static class Else implements ElsePart {

    final List<OpCode> expr;

    public Else(List<OpCode> expr) {
      this.expr = expr;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Else) {
        Else that = (Else) obj;
        return expr.equals(that.expr);
      }
      return false;
    }

    protected void toString(StringBuilder sb) {
      sb.append("%e");
      for (OpCode op : expr) {
        op.toString(sb);
      }
    }
  }

  public static class Then {

    final List<OpCode> expr;
    final ElsePart elsePart;

    public Then(List<OpCode> expr, ElsePart elsePart) {
      this.expr = expr;
      this.elsePart = elsePart;
    }

    public Then(List<OpCode> expr) {
      this.expr = expr;
      this.elsePart = null;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Then) {
        Then that = (Then) obj;
        return expr.equals(that.expr) && (elsePart == null ? that.elsePart == null : elsePart.equals(that.elsePart));
      }
      return false;
    }

    protected void toString(StringBuilder sb) {
      sb.append("%t");
      for (OpCode op : expr) {
        op.toString(sb);
      }
      if (elsePart instanceof Else) {
        ((Else) elsePart).toString(sb);
      } else if (elsePart instanceof If) {
        ((If) elsePart).toString(sb);
      }
    }
  }
}
