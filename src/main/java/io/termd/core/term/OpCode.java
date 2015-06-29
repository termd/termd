/*
 * Copyright 2015 Julien Viet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

  public void eval(EvalContext context) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + " operation not implemented");
  }

  protected abstract void toString(StringBuilder sb);

  public static class PushParam extends OpCode {

    private final int index;

    public PushParam(int index) {
      if (index < 1 || index > 9) {
        throw new IllegalArgumentException("Parameter index must be between 1 and 9");
      }
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

    @Override
    public void eval(EvalContext context) {
      int ptr = index - 1;
      if (ptr >= context.getParametersLength()) {
        throw new IllegalArgumentException("Not enough parameters");
      }
      context.push(context.getParameter(ptr));
    }
  }

  // Synthetic opcode for string literal
  public static class Literal extends OpCode {
    final String value;
    public Literal(String value) {
      this.value = value;
    }
    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Literal) {
        Literal that = (Literal) obj;
        return value.equals(that.value);
      }
      return false;
    }
    @Override
    protected void toString(StringBuilder sb) {
      sb.append(value);
    }

    @Override
    public void eval(EvalContext context) {
      context.writeString(value);
    }
  }

  // %'A' or %{65}
  public static class PushConstant extends OpCode {

    private final int value;
    private final boolean literal;

    public PushConstant(int value, boolean literal) {
      this.value = value;
      this.literal = literal;
    }

    public int getValue() {
      return value;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof OpCode.PushConstant) {
        OpCode.PushConstant that = (PushConstant) obj;
        return value == that.value && literal == that.literal;
      }
      return false;
    }

    @Override
    protected void toString(StringBuilder sb) {
      if (literal) {
        sb.append("%{").append(value).append("}");
      } else {
        sb.append("%'").append((char)value).append("'");
      }
    }

    @Override
    public void eval(EvalContext context) {
      StringBuilder sb = new StringBuilder();
      if (literal) {
        sb.append(value);
      } else {
        sb.appendCodePoint(value);
      }
      context.push(sb.toString());
    }
  }

  // %%
  public static class Percent extends OpCode {
    public static final Percent INSTANCE = new Percent();
    private Percent() {
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

    @Override
    public void eval(EvalContext context) {
      if (context.getParametersLength() < 2) {
        throw new IllegalArgumentException("Missing parameters");
      }
      for (int i = 0;i < 2; i++) {
        context.setParameter(i, Integer.toString((Integer.parseInt(context.getParameter(i)) + 1)));
      }
    }
  }

  // %c
  // %s
  public static class PrintChar extends OpCode {
    public static final PrintChar INSTANCE = new PrintChar();
    private PrintChar() {
    }
    @Override
    protected void toString(StringBuilder sb) {
      sb.append("%c");
    }
    @Override
    public void eval(EvalContext context) {
      String s = context.pop();
      int codePoint = Integer.parseInt(s);
      context.writeCodePoint(codePoint);
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
    public static final Bit NEG = new Bit('~');

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

    public static final Logical EQ = new Logical('=') {
      @Override
      public void eval(EvalContext context) {
        int op1 = Integer.parseInt(context.pop());
        int op2 = Integer.parseInt(context.pop());
        context.push(op1 == op2 ? "1" : "0");
      }
    };
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

    public static final Arithmetic PLUS = new Arithmetic('+') {
      @Override
      public void eval(EvalContext context) {
        int op1 = Integer.parseInt(context.pop());
        int op2 = Integer.parseInt(context.pop());
        context.push(Integer.toString(op1 + op2));
      }
    };
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

    @Override
    public void eval(EvalContext context) {
      if (flag != null || width != null || precision != null) {
        super.eval(context);
      }
      StringBuilder format = new StringBuilder();
      if (specifier != null) {
        switch (specifier) {
          case 'd': {
            int value = Integer.parseInt(context.pop());
            context.writeNumber(value);
            break;
          }
          case 's': {
            String s = context.pop();
            context.writeString(s);
            break;
          }
          default:
            super.eval(context);
        }
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

    @Override
    public void eval(EvalContext context) {
      for (OpCode opCode : expr) {
        opCode.eval(context);
      }
      thenPart.eval(context);
    }
  }

  public static class Else extends OpCode implements ElsePart {

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

    @Override
    public void eval(EvalContext context) {
      for (OpCode opCode : expr) {
        opCode.eval(context);
      }
    }
  }

  public static class Then extends OpCode {

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

    @Override
    public void eval(EvalContext context) {
      int cond = Integer.parseInt(context.pop());
      if (cond != 0) {
        for (OpCode opCode : expr) {
          opCode.eval(context);
        }
      } else {
        if (elsePart instanceof If) {
          ((If) elsePart).eval(context);
        } else if (elsePart instanceof Else) {
          ((Else) elsePart).eval(context);
        }
      }
    }
  }
}
