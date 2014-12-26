package io.modsh.core.writeline;

import io.modsh.core.Handler;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class EscapeFilter implements Handler<Integer> {

  private EscStatus status = EscStatus.NORMAL;
  private final Escaper escaper;

  public EscapeFilter(Escaper escaper) {
    this.escaper = escaper;
  }

  @Override
  public void handle(Integer code) {
    switch (status) {
      case NORMAL:
        switch ((int)code) {
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
            escaper.handle(code);
            break;
        }
        break;
      case IN_QUOTE:
        if (code == '\'') {
          escaper.endEscape('\'');
          status = EscStatus.NORMAL;
        } else {
          escaper.handle(code);
        }
        break;
      case IN_DOUBLE_QUOTE:
        if (code == '"') {
          escaper.endEscape('\"');
          status = EscStatus.NORMAL;
        } else {
          escaper.handle(code);
        }
        break;
      case IN_BACKSLASH:
        escaper.handle(code);
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
