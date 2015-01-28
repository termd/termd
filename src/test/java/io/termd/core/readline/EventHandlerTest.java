package io.termd.core.readline;

import io.termd.core.Handler;
import io.termd.core.readline.functions.BackwardChar;
import io.termd.core.readline.functions.BackwardDeleteChar;
import io.termd.core.readline.functions.ForwardChar;
import io.termd.core.telnet.TestBase;
import io.termd.core.term.TermEvent;
import io.termd.core.term.TermRequest;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class EventHandlerTest extends TestBase {

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
  
  static class EventContextImpl implements EventContext {

    int endCount = 0;
    final Event event;

    public EventContextImpl(Event event) {
      this.event = event;
    }

    @Override
    public Event getEvent() {
      return event;
    }

    @Override
    public void end() {
      endCount++;
    }
  }

  class Term {

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
      this(new Handler<TermRequest>() {
        @Override
        public void handle(TermRequest event) {
          event.write("% ");
          event.end();
        }
      });
    }

    public Term(Handler<TermRequest> requestHandler) {
      handler = new EventHandler(adapter, new Executor() {
        @Override
        public void execute(Runnable command) {
          command.run();
        }
      }, requestHandler).
          addFunction(new BackwardDeleteChar()).
          addFunction(new BackwardChar()).
          addFunction(new ForwardChar());
      handler.init();
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
    term.handler.handle(new EventContextImpl(Keys.CTRL_M));
    term.assertScreen(
        "% ",
        "% "
    );
    term.assertAt(1, 2);
  }

  @Test
  public void testInsertChar() {
    Term term = new Term();
    EventContextImpl context = new EventContextImpl(Keys.A);
    term.handler.handle(context);
    assertEquals(1, context.endCount);
    term.assertScreen("% A");
    term.assertAt(0, 3);
  }

  @Test
  public void testInsertCharEnter() {
    Term term = new Term();
    term.handler.handle(new EventContextImpl(Keys.A));
    term.handler.handle(new EventContextImpl(Keys.CTRL_M));
    term.assertScreen(
        "% A",
        "% ");
    term.assertAt(1, 2);
  }

  @Test
  public void testBS() {
    Term term = new Term();
    term.handler.handle(new EventContextImpl(Keys.A));
    term.handler.handle(new EventContextImpl(BACKWARD_DELETE_CHAR));
    term.assertScreen("%  ");
    term.assertAt(0, 2);
  }

  @Test
  public void testEscapeCR() {
    Term term = new Term();
    term.handler.handle(new EventContextImpl(Keys.BACKSLASH));
    term.assertScreen("% \\");
    EventContextImpl context = new EventContextImpl(Keys.CTRL_M);
    term.handler.handle(context);
    term.assertScreen(
        "% \\",
        "> "
    );
    term.assertAt(1, 2);
    assertEquals(1, context.endCount);
  }

  @Test
  public void testBackwardDelete() {
    Term term = new Term();
    term.handler.handle(new EventContextImpl(BACKWARD_DELETE_CHAR));
    term.assertScreen(
        "% "
    );
    term.assertAt(0, 2);
  }

  @Test
  public void testBackwardDeleteLastChar() {
    Term term = new Term();
    term.handler.handle(new EventContextImpl(Keys.A));
    term.handler.handle(new EventContextImpl(Keys.B));
    term.handler.handle(new EventContextImpl(BACKWARD_DELETE_CHAR));
    term.assertScreen(
        "% A "
    );
    term.assertAt(0, 3);
  }

  @Test
  public void testBackwardDeleteChar() {
    Term term = new Term();
    term.handler.handle(new EventContextImpl(Keys.A));
    term.handler.handle(new EventContextImpl(Keys.B));
    term.handler.handle(new EventContextImpl(BACKWARD_CHAR));
    term.handler.handle(new EventContextImpl(BACKWARD_DELETE_CHAR));
    term.assertScreen(
        "% B "
    );
    term.assertAt(0, 2);
  }

  @Test
  public void testBackwardDeleteEscape() {
    Term term = new Term();
    term.handler.handle(new EventContextImpl(Keys.BACKSLASH));
    term.assertScreen("% \\");
    term.handler.handle(new EventContextImpl(BACKWARD_DELETE_CHAR));
    term.assertScreen(
        "%  "
    );
    term.assertAt(0, 2);
  }

  @Test
  public void testBackwardChar() {
    Term term = new Term();
    term.handler.handle(new EventContextImpl(BACKWARD_CHAR));
    term.assertScreen("% ");
    term.assertAt(0, 2);
  }

  @Test
  public void testInsertCharBackwardChar() {
    Term term = new Term();
    term.handler.handle(new EventContextImpl(Keys.A));
    term.handler.handle(new EventContextImpl(BACKWARD_CHAR));
    term.assertScreen("% A");
    term.assertAt(0, 2);
  }

  @Test
  public void testForwardChar() {
    Term term = new Term();
    term.handler.handle(new EventContextImpl(FORWARD_CHAR));
    term.assertScreen("% ");
    term.assertAt(0, 2);
  }

  @Test
  public void testInsertCharForwardChar() {
    Term term = new Term();
    term.handler.handle(new EventContextImpl(Keys.A));
    term.handler.handle(new EventContextImpl(BACKWARD_CHAR));
    term.handler.handle(new EventContextImpl(FORWARD_CHAR));
    term.assertScreen("% A");
    term.assertAt(0, 3);
  }

  @Test
  public void testQuotedMultiline() {
    Term term = new Term();
    term.handler.handle(new EventContextImpl(Keys.A));
    term.handler.handle(new EventContextImpl(Keys.QUOTE));
    EventContextImpl context = new EventContextImpl(Keys.CTRL_M);
    term.handler.handle(context);
    term.assertScreen(
        "% A\"",
        "> ");
    assertEquals(1, context.endCount);
    term.handler.handle(new EventContextImpl(Keys.B));
    term.handler.handle(new EventContextImpl(Keys.CTRL_M));
    term.assertScreen(
        "% A\"",
        "> B",
        "> ");
    term.handler.handle(new EventContextImpl(Keys.C));
    term.handler.handle(new EventContextImpl(Keys.QUOTE));
    term.handler.handle(new EventContextImpl(Keys.CTRL_M));
    term.assertScreen(
        "% A\"",
        "> B",
        "> C\"",
        "% ");
  }

  @Test
  public void testCharsQueuing() {
    final AtomicReference<TermRequest> ctx = new AtomicReference<>();
    Term term = new Term(new Handler<TermRequest>() {
      @Override
      public void handle(TermRequest request) {
        switch (request.requestCount()) {
          case 0:
            request.write("% ").end();
            break;
          case 1:
            ctx.set(request);
            break;
          default:
            fail("was not expecting such request");
        }
      }
    });
    term.handler.append(new int[]{'\r'});
    assertNotNull(ctx);
    term.assertScreen("% ");
    term.assertAt(1, 0);
    term.handler.append(new int[]{'A'});
    term.assertScreen("% ");
    term.assertAt(1, 0);
    ctx.get().write("% ").end();
    term.assertScreen(
        "% ",
        "% A");
    term.assertAt(1, 3);
  }

  @Test
  public void testSetDataHandler() {
    final AtomicReference<TermRequest> ctx = new AtomicReference<>();
    final LinkedList<TermEvent> events = new LinkedList<>();
    Term term = new Term(new Handler<TermRequest>() {
      @Override
      public void handle(final TermRequest request) {
        switch (request.requestCount()) {
          case 0:
            request.write("% ").end();
            break;
          case 1:
            ctx.set(request);
            request.eventHandler(new Handler<TermEvent>() {
              @Override
              public void handle(TermEvent data) {
                events.add(data);
                if (events.size() == 1) {
                  request.eventHandler(null);
                }
              }
            });
            break;
          default:
            fail("was not expecting such request");
        }
      }
    });
    term.handler.append(new int[]{'\r'});
    term.handler.append(new int[]{'h','e','l','l','o'});
    assertEquals(1, events.size());
    assertTrue(Arrays.equals(new int[]{'h', 'e', 'l', 'l', 'o'}, ((TermEvent.Data) events.get(0)).getData()));
    term.assertScreen(
        "% "
    );
    term.assertAt(1, 0);
    term.handler.append(new int[]{'b', 'y', 'e'});
    assertEquals(1, events.size());
    ctx.get().write("% ").end();
    term.assertScreen(
        "% ",
        "% bye"
    );
    term.assertAt(1, 5);
  }

  @Test
  public void testResetDataHandlerAfterRequest() {
    final LinkedList<TermEvent> events = new LinkedList<>();
    final AtomicReference<TermRequest> ctx = new AtomicReference<>();
    Term term = new Term(new Handler<TermRequest>() {
      @Override
      public void handle(final TermRequest request) {
        switch (request.requestCount()) {
          case 0:
            request.write("% ").end();
            break;
          case 1:
            request.eventHandler(new Handler<TermEvent>() {
              @Override
              public void handle(TermEvent event) {
                events.add(event);
              }
            });
            ctx.set(request);
            break;
          case 2:
            ctx.set(request);
            break;
          default:
            fail("was not expecting such request");
        }
      }
    });
    term.handler.append(new int[]{'\r'});
    ctx.get().write("% ").end();
    term.handler.append(new int[]{'\r'});
    term.handler.append(new int[]{'b', 'y', 'e'});
    ctx.get().write("% ").end();
    assertEquals(0, events.size());
    term.assertScreen(
        "% ",
        "% ",
        "% bye"
    );
    term.assertAt(2, 5);
  }

  @Test
  public void testEndedTermRequest() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    Term term = new Term(new Handler<TermRequest>() {
      @Override
      public void handle(final TermRequest request) {
        request.write("foo");
        request.end();
        assertNull(request.getData());
        assertEquals(0, request.requestCount());
        try {
          request.eventHandler(new Handler<TermEvent>() {
            @Override
            public void handle(TermEvent event) {
            }
          });
          fail("was expecting an illegal state exception");
        } catch (IllegalStateException ignore) {
        }
        try {
          request.write("something");
          fail("was expecting an illegal state exception");
        } catch (IllegalStateException ignore) {
        }
        try {
          request.end();
          fail("was expecting an illegal state exception");
        } catch (IllegalStateException ignore) {
        }
        latch.countDown();
      }
    });
    awaitLatch(latch);
    term.assertScreen("foo");
    term.assertAt(0, 3);
  }
}
