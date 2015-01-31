package io.termd.core.readline;

import io.termd.core.readline.functions.BackwardChar;
import io.termd.core.readline.functions.BackwardDeleteChar;
import io.termd.core.readline.functions.ForwardChar;
import io.termd.core.telnet.TestBase;
import io.termd.core.tty.Signal;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Dimension;
import io.termd.core.util.Handler;
import io.termd.core.util.Provider;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReadlineTest extends TestBase {


  private static final int[] FORWARD_CHAR = { 27, '[', 'C' };
  private static final int[] BACKWARD_CHAR = { 27, '[', 'D' };
  private static final int[] BACKWARD_DELETE_CHAR = { 8 };

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
    Handler<int[]> writeHandler = new Handler<int[]>() {
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
    final Readline handler;

    private Handler<int[]> readHandler;

    public Term() {
      Keymap keymap = InputrcParser.create();
      handler = new Readline(keymap);
      handler.addFunction(new BackwardDeleteChar());
      handler.addFunction(new BackwardChar());
      handler.addFunction(new ForwardChar());
    }

    public void readlineFail() {
      readline(new Handler<String>() {
        @Override
        public void handle(String event) {
          fail("Was not accepting a call");
        }
      });
    }

    public Provider<String> readlineComplete() {
      final AtomicReference<String> queue = new AtomicReference<>();
      readline(new Handler<String>() {
        @Override
        public void handle(String event) {
          queue.compareAndSet(null, event);
        }
      });
      return new Provider<String>() {
        @Override
        public String provide() {
          return queue.get();
        }
      };
    }

    public void readline(Handler<String> readlineHandler) {
      handler.readline(new TtyConnection() {
        @Override
        public Handler<Dimension> getResizeHandler() {
          throw new UnsupportedOperationException();
        }
        @Override
        public void setResizeHandler(Handler<Dimension> handler) {
          throw new UnsupportedOperationException();
        }
        @Override
        public Handler<Signal> getSignalHandler() {
          throw new UnsupportedOperationException();
        }
        @Override
        public void setSignalHandler(Handler<Signal> handler) {
          throw new UnsupportedOperationException();
        }
        @Override
        public Handler<int[]> getReadHandler() {
          return readHandler;
        }
        @Override
        public void setReadHandler(Handler<int[]> handler) {
          readHandler = handler;
        }
        @Override
        public Handler<int[]> writeHandler() {
          return writeHandler;
        }
        @Override
        public void schedule(Runnable task) {
          throw new UnsupportedOperationException();
        }
      }, "% ", readlineHandler);
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

    void read(int... data) {
      readHandler.handle(data);
    }

  }

/*
  @Test
  public void testPrompt() {
    Term term = new Term();
    term.assertScreen("% ");
    term.assertAt(0, 2);
  }
*/

  @Test
  public void testEnter() {
    Term term = new Term();
    term.readline(new Handler<String>() {
      @Override
      public void handle(String event) {
        testComplete();
      }
    });
    term.read('\r');
    term.assertScreen("% ");
    term.assertAt(1, 0);
    await();
  }

  @Test
  public void testInsertChar() {
    Term term = new Term();
    term.readlineFail();
    term.read('A');
    term.assertScreen("% A");
    term.assertAt(0, 3);
  }

  @Test
  public void testInsertCharEnter() throws Exception {
    Term term = new Term();
    Provider<String> line = term.readlineComplete();
    term.read('A');
    term.read('\r');
    term.assertScreen("% A");
    term.assertAt(1, 0);
    assertEquals("A", line.provide());
  }

  @Test
  public void testEscapeCR() {
    Term term = new Term();
    term.readlineFail();
    term.read('\\');
    term.assertScreen("% \\");
    term.read('\r');
    term.assertScreen(
        "% \\",
        "> "
    );
    term.assertAt(1, 2);
  }

  @Test
  public void testBackwardDeleteChar() {
    Term term = new Term();
    term.readlineFail();
    term.read('A');
    term.read(BACKWARD_DELETE_CHAR);
    term.assertScreen("%  ");
    term.assertAt(0, 2);
  }

  @Test
  public void testBackwardDelete() {
    Term term = new Term();
    term.readlineFail();
    term.read(BACKWARD_DELETE_CHAR);
    term.assertScreen(
        "% "
    );
    term.assertAt(0, 2);
  }

  @Test
  public void testBackwardDeleteLastChar() {
    Term term = new Term();
    term.readlineFail();
    term.read('A');
    term.read('B');
    term.read(8);
    term.assertScreen(
        "% A "
    );
    term.assertAt(0, 3);
  }

  @Test
  public void testBackwardCharBackwardDeleteChar() {
    Term term = new Term();
    term.readlineFail();
    term.read('A');
    term.read('B');
    term.read(BACKWARD_CHAR);
    term.read(BACKWARD_DELETE_CHAR);
    term.assertScreen(
        "% B "
    );
    term.assertAt(0, 2);
  }

  @Test
  public void testBackwardDeleteEscape() {
    Term term = new Term();
    term.readlineFail();
    term.read('\\');
    term.assertScreen("% \\");
    term.read(BACKWARD_DELETE_CHAR);
    term.assertScreen(
        "%  "
    );
    term.assertAt(0, 2);
  }

  @Test
  public void testBackwardChar() {
    Term term = new Term();
    term.readlineFail();
    term.read(BACKWARD_CHAR);
    term.assertScreen("% ");
    term.assertAt(0, 2);
  }

  @Test
  public void testInsertCharBackwardChar() {
    Term term = new Term();
    term.readlineFail();
    term.read('A');
    term.read(BACKWARD_CHAR);
    term.assertScreen("% A");
    term.assertAt(0, 2);
  }

  @Test
  public void testForwardChar() {
    Term term = new Term();
    term.readlineFail();
    term.read(FORWARD_CHAR);
    term.assertScreen("% ");
    term.assertAt(0, 2);
  }

  @Test
  public void testInsertCharForwardChar() {
    Term term = new Term();
    term.readlineFail();
    term.read('A');
    term.read(BACKWARD_CHAR);
    term.read(FORWARD_CHAR);
    term.assertScreen("% A");
    term.assertAt(0, 3);
  }

  @Test
  public void testQuotedMultiline() {
    Term term = new Term();
    Provider<String> a = term.readlineComplete();
    term.read('A');
    term.read('"');
    term.read('\r');
    assertNull(a.provide());
    term.assertScreen(
        "% A\"",
        "> ");
    term.read('B');
    term.read('\r');
    term.assertScreen(
        "% A\"",
        "> B",
        "> ");
    assertNull(a.provide());
    term.read('C');
    term.read('"');
    term.read('\r');
    term.assertScreen(
        "% A\"",
        "> B",
        "> C\"");
    term.assertAt(3, 0);
    assertEquals("A\"\nB\nC\"", a.provide());
  }
/*

  @Test
  public void testCharsQueuing() {
    final AtomicReference<ReadlineRequest> ctx = new AtomicReference<>();
    Term term = new Term(new Handler<ReadlineRequest>() {
      @Override
      public void handle(ReadlineRequest request) {
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
    final AtomicReference<ReadlineRequest> ctx = new AtomicReference<>();
    final LinkedList<TermEvent> events = new LinkedList<>();
    Term term = new Term(new Handler<ReadlineRequest>() {
      @Override
      public void handle(final ReadlineRequest request) {
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
    assertTrue(Arrays.equals(new int[]{'h', 'e', 'l', 'l', 'o'}, ((TermEvent.Read) events.get(0)).getData()));
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
    final AtomicReference<ReadlineRequest> ctx = new AtomicReference<>();
    Term term = new Term(new Handler<ReadlineRequest>() {
      @Override
      public void handle(final ReadlineRequest request) {
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
    Term term = new Term(new Handler<ReadlineRequest>() {
      @Override
      public void handle(final ReadlineRequest request) {
        request.write("foo");
        request.end();
        assertNull(request.line());
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
*/
}
