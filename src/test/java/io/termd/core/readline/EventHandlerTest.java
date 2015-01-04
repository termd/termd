package io.termd.core.readline;

import io.termd.core.Handler;
import io.termd.core.readline.functions.BackwardChar;
import io.termd.core.readline.functions.BackwardDeleteChar;
import io.termd.core.readline.functions.ForwardChar;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class EventHandlerTest {

  public static final FunctionEvent BACKWARD_DELETE_CHAR = new FunctionEvent() {
    @Override
    public String getName() {
      return "backward-delete-char";
    }
  };

  public static final FunctionEvent BACKWARD_CHAR = new FunctionEvent() {
    @Override
    public String getName() {
      return "backward-char";
    }
  };

  public static final FunctionEvent FORWARD_CHAR = new FunctionEvent() {
    @Override
    public String getName() {
      return "forward-char";
    }
  };

  static class Counter implements Handler<Void> {
    int value = 0;
    @Override
    public void handle(Void event) {
      value++;
    }
  }

  static class Term {

    private int[][] buffer = new int[10][];
    private int row;
    private int cursor;
    Handler<int[]> adapter = new Handler<int[]>() {
      @Override
      public void handle(int[] event) {
        for (int i : event) {
          if (buffer[row] == null) {
            buffer[row] = new int[100];
          }
          if (i >= 32) {
            buffer[row][cursor++] = i;
          } else {
            switch (i) {
              case '\r':
                cursor = 0;
                break;
              case '\n':
                row++;
                break;
              case '\b':
                if (cursor > 0) {
                  cursor--;
                } else {
                  throw new UnsupportedOperationException();
                }
                break;
            }
          }
        }
      }
    };
    final EventHandler handler;

    public Term() {
      this(new Handler<RequestContext>() {
        @Override
        public void handle(RequestContext event) {
          event.end();
        }
      });
    }

    public Term(Handler<RequestContext> requestHandler) {
      handler = new EventHandler(adapter, requestHandler).
          addFunction(new BackwardDeleteChar()).
          addFunction(new BackwardChar()).
          addFunction(new ForwardChar());
    }

    private List<String> render() {
      List<String> lines = new ArrayList<>();
      for (int[] row : buffer) {
        if (row == null) {
          break;
        }
        StringBuilder line = new StringBuilder();
        for (int codePoint : row) {
          if (codePoint < 32) {
            break;
          }
          line.appendCodePoint(codePoint);
        }
        lines.add(line.toString());
      }
      return lines;
    }

    void assertScreen(String... expected) {
      List<String> lines = render();
      assertEquals(Arrays.asList(expected), lines);
    }

    void assertAt(int row, int cursor) {
      assertEquals(row, this.row);
      assertEquals(cursor, this.cursor);
    }
  }

  @Test
  public void testPrompt() {
    Term term = new Term();
    term.assertScreen("% ");
    term.assertAt(0, 2);
  }

  @Test
  public void testEnter() {
    Term term = new Term();
    term.handler.handle(Keys.CTRL_M);
    term.assertScreen(
        "% ",
        "% "
    );
    term.assertAt(1, 2);
  }

  @Test
  public void testInsertChar() {
    Term term = new Term();
    Counter counter = new Counter();
    term.handler.handle(Keys.A, counter);
    assertEquals(1, counter.value);
    term.assertScreen("% A");
    term.assertAt(0, 3);
  }

  @Test
  public void testInsertCharEnter() {
    Term term = new Term();
    term.handler.handle(Keys.A);
    term.handler.handle(Keys.CTRL_M);
    term.assertScreen(
        "% A",
        "% ");
    term.assertAt(1, 2);
  }

  @Test
  public void testBS() {
    Term term = new Term();
    term.handler.handle(Keys.A);
    term.handler.handle(BACKWARD_DELETE_CHAR);
    term.assertScreen("%  ");
    term.assertAt(0, 2);
  }

  @Test
  public void testEscapeCR() {
    Term term = new Term();
    term.handler.handle(Keys.BACKSLASH);
    term.assertScreen("% \\");
    Counter counter = new Counter();
    term.handler.handle(Keys.CTRL_M, counter);
    term.assertScreen(
        "% \\",
        "> "
    );
    term.assertAt(1, 2);
    assertEquals(1, counter.value);
  }

  @Test
  public void testBackwardDelete() {
    Term term = new Term();
    term.handler.handle(BACKWARD_DELETE_CHAR);
    term.assertScreen(
        "% "
    );
    term.assertAt(0, 2);
  }

  @Test
  public void testBackwardDeleteLastChar() {
    Term term = new Term();
    term.handler.handle(Keys.A);
    term.handler.handle(Keys.B);
    term.handler.handle(BACKWARD_DELETE_CHAR);
    term.assertScreen(
        "% A "
    );
    term.assertAt(0, 3);
  }

  @Test
  public void testBackwardDeleteChar() {
    Term term = new Term();
    term.handler.handle(Keys.A);
    term.handler.handle(Keys.B);
    term.handler.handle(BACKWARD_CHAR);
    term.handler.handle(BACKWARD_DELETE_CHAR);
    term.assertScreen(
        "% B "
    );
    term.assertAt(0, 2);
  }

  @Test
  public void testBackwardDeleteEscape() {
    Term term = new Term();
    term.handler.handle(Keys.BACKSLASH);
    term.assertScreen("% \\");
    term.handler.handle(BACKWARD_DELETE_CHAR);
    term.assertScreen(
        "%  "
    );
    term.assertAt(0, 2);
  }

  @Test
  public void testBackwardChar() {
    Term term = new Term();
    term.handler.handle(BACKWARD_CHAR);
    term.assertScreen("% ");
    term.assertAt(0, 2);
  }

  @Test
  public void testInsertCharBackwardChar() {
    Term term = new Term();
    term.handler.handle(Keys.A);
    term.handler.handle(BACKWARD_CHAR);
    term.assertScreen("% A");
    term.assertAt(0, 2);
  }

  @Test
  public void testForwardChar() {
    Term term = new Term();
    term.handler.handle(FORWARD_CHAR);
    term.assertScreen("% ");
    term.assertAt(0, 2);
  }

  @Test
  public void testInsertCharForwardChar() {
    Term term = new Term();
    term.handler.handle(Keys.A);
    term.handler.handle(BACKWARD_CHAR);
    term.handler.handle(FORWARD_CHAR);
    term.assertScreen("% A");
    term.assertAt(0, 3);
  }

  @Test
  public void testQuotedMultiline() {
    Term term = new Term();
    term.handler.handle(Keys.A);
    term.handler.handle(Keys.QUOTE);
    Counter counter = new Counter();
    term.handler.handle(Keys.CTRL_M, counter);
    term.assertScreen(
        "% A\"",
        "> ");
    assertEquals(1, counter.value);
    term.handler.handle(Keys.B);
    term.handler.handle(Keys.CTRL_M);
    term.assertScreen(
        "% A\"",
        "> B",
        "> ");
    term.handler.handle(Keys.C);
    term.handler.handle(Keys.QUOTE);
    term.handler.handle(Keys.CTRL_M);
    term.assertScreen(
        "% A\"",
        "> B",
        "> C\"",
        "% ");
  }

  @Test
  public void testBlocking() {
    final AtomicReference<RequestContext> ctx = new AtomicReference<>();
    Term term = new Term(new Handler<RequestContext>() {
      @Override
      public void handle(RequestContext event) {
        ctx.set(event);
      }
    });
    Counter counter = new Counter();
    term.handler.handle(Keys.CTRL_M, counter);
    assertNotNull(ctx);
    term.assertScreen("% ");
    term.assertAt(1, 0);
    assertEquals(0, counter.value);
    try {
      term.handler.handle(Keys.A);
      fail();
    } catch (IllegalStateException ignore) {
    }
    ctx.get().end();
    term.assertScreen(
        "% ",
        "% ");
    term.assertAt(1, 2);
    assertEquals(1, counter.value);
    term.handler.handle(Keys.A);
    term.assertScreen(
        "% ",
        "% A");
    term.assertAt(1, 3);
  }
}
