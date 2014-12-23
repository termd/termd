package io.modsh.core.writeline;

import java.util.function.IntConsumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class EscapeFilter implements IntConsumer {

  private EscStatus status = EscStatus.NORMAL;
  private final Escaper escaper;

  public EscapeFilter(Escaper escaper) {
    this.escaper = escaper;
  }

  @Override
  public void accept(int code) {
    switch (status) {
      case NORMAL:
        switch (code) {
          case '\'':
            escaper.beginEscape('\'');
            status = EscStatus.IN_QUOTE;
            break;
          case '"':
            escaper.beginEscape('\"');
            status = EscStatus.IN_DOUBLE_QUOTE;
            break;
          case '\\':
            escaper.beginEscape('\\');
            status = EscStatus.IN_BACKSLASH;
            break;
          default:
            escaper.accept(code);
            break;
        }
        break;
      case IN_QUOTE:
        if (code == '\'') {
          escaper.endEscape('\'');
          status = EscStatus.NORMAL;
        } else {
          escaper.accept(code);
        }
        break;
      case IN_DOUBLE_QUOTE:
        if (code == '"') {
          escaper.endEscape('\"');
          status = EscStatus.NORMAL;
        } else {
          escaper.accept(code);
        }
        break;
      case IN_BACKSLASH:
        escaper.accept(code);
        escaper.endEscape('\\');
        status = EscStatus.NORMAL;
        break;
      default:
        break;
    }
  }

  public static enum EscStatus {

    NORMAL, IN_QUOTE, IN_DOUBLE_QUOTE, IN_BACKSLASH

  }
}
