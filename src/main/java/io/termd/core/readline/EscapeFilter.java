package io.termd.core.readline;

import io.termd.core.Handler;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
class EscapeFilter implements Handler<Integer> {

  private EscStatus status = EscStatus.NORMAL;
  private final Escaper escaper;

  EscapeFilter(Escaper escaper) {
    this.escaper = escaper;
  }

  @Override
  public void handle(Integer code) {
    switch (status) {
      case NORMAL:
        switch ((int)code) {
          case '\'':
            escaper.beginQuotes('\'');
            status = EscStatus.IN_QUOTE;
            break;
          case '"':
            escaper.beginQuotes('\"');
            status = EscStatus.IN_DOUBLE_QUOTE;
            break;
          case '\\':
            escaper.escaping();
            status = EscStatus.IN_BACKSLASH;
            break;
          default:
            escaper.handle(code);
            break;
        }
        break;
      case IN_QUOTE:
        if (code == '\'') {
          escaper.endQuotes('\'');
          status = EscStatus.NORMAL;
        } else {
          escaper.handle(code);
        }
        break;
      case IN_DOUBLE_QUOTE:
        if (code == '"') {
          escaper.endQuotes('\"');
          status = EscStatus.NORMAL;
        } else {
          escaper.handle(code);
        }
        break;
      case IN_BACKSLASH:
        escaper.escaped(code);
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
