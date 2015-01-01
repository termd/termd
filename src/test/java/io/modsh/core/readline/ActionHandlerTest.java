package io.modsh.core.readline;

import io.modsh.core.Handler;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ActionHandlerTest {

  public static final FunctionAction BACKWARD_DELETE_CHAR = new FunctionAction() {
    @Override
    public String getName() {
      return "backward-delete-char";
    }

    @Override
    public int getAt(int index) throws IndexOutOfBoundsException {
      throw new IndexOutOfBoundsException();
    }

    @Override
    public int length() {
      return 0;
    }
  };

  public static final FunctionAction BACKWARD_CHAR = new FunctionAction() {
    @Override
    public String getName() {
      return "backward-char";
    }

    @Override
    public int getAt(int index) throws IndexOutOfBoundsException {
      throw new IndexOutOfBoundsException();
    }

    @Override
    public int length() {
      return 0;
    }
  };

  public static final FunctionAction FORWARD_CHAR = new FunctionAction() {
    @Override
    public String getName() {
      return "forward-char";
    }

    @Override
    public int getAt(int index) throws IndexOutOfBoundsException {
      throw new IndexOutOfBoundsException();
    }

    @Override
    public int length() {
      return 0;
    }
  };

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
    final ActionHandler handler = new ActionHandler(adapter);

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
    term.handler.handle(Keys.A);
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
    term.handler.handle(Keys.CTRL_M);
    term.assertScreen(
        "% \\",
        "> "
    );
    term.assertAt(1, 2);
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
}
