package io.termd.core.readline;

import io.termd.core.telnet.TestBase;
import io.termd.core.util.Dimension;
import io.termd.core.util.Helper;
import org.junit.Test;

import java.util.LinkedList;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReadlineTest extends TestBase {

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
    TestTerm term = new TestTerm(this);
    term.readline(event -> testComplete());
    term.read('\r');
    term.assertScreen("% ");
    term.assertAt(1, 0);
    await();
  }

  @Test
  public void testInsertChar() {
    TestTerm term = new TestTerm(this);
    term.readlineFail();
    term.read('A');
    term.assertScreen("% A");
    term.assertAt(0, 3);
  }

  @Test
  public void testInsertCharEnter() throws Exception {
    TestTerm term = new TestTerm(this);
    Supplier<String> line = term.readlineComplete();
    term.read('A');
    term.read('\r');
    term.assertScreen("% A");
    term.assertAt(1, 0);
    assertEquals("A", line.get());
  }

  @Test
  public void testEscapeCR() {
    TestTerm term = new TestTerm(this);
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
    TestTerm term = new TestTerm(this);
    term.readlineFail();
    term.read('A');
    term.read(BACKWARD_DELETE_CHAR);
    term.assertScreen("%  ");
    term.assertAt(0, 2);
  }

  @Test
  public void testBackwardDelete() {
    TestTerm term = new TestTerm(this);
    term.readlineFail();
    term.read(BACKWARD_DELETE_CHAR);
    term.assertScreen(
        "% "
    );
    term.assertAt(0, 2);
  }

  @Test
  public void testBackwardDeleteLastChar() {
    TestTerm term = new TestTerm(this);
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
    TestTerm term = new TestTerm(this);
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
    TestTerm term = new TestTerm(this);
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
    TestTerm term = new TestTerm(this);
    term.readlineFail();
    term.read(BACKWARD_CHAR);
    term.assertScreen("% ");
    term.assertAt(0, 2);
  }

  @Test
  public void testInsertCharBackwardChar() {
    TestTerm term = new TestTerm(this);
    term.readlineFail();
    term.read('A');
    term.read(BACKWARD_CHAR);
    term.assertScreen("% A");
    term.assertAt(0, 2);
  }

  @Test
  public void testForwardChar() {
    TestTerm term = new TestTerm(this);
    term.readlineFail();
    term.read(FORWARD_CHAR);
    term.assertScreen("% ");
    term.assertAt(0, 2);
  }

  @Test
  public void testInsertCharForwardChar() {
    TestTerm term = new TestTerm(this);
    term.readlineFail();
    term.read('A');
    term.read(BACKWARD_CHAR);
    term.read(FORWARD_CHAR);
    term.assertScreen("% A");
    term.assertAt(0, 3);
  }

  @Test
  public void testQuotedMultiline() {
    TestTerm term = new TestTerm(this);
    Supplier<String> a = term.readlineComplete();
    term.read('A');
    term.read('"');
    term.read('\r');
    assertNull(a.get());
    term.assertScreen(
        "% A\"",
        "> ");
    term.read('B');
    term.read('\r');
    term.assertScreen(
        "% A\"",
        "> B",
        "> ");
    assertNull(a.get());
    term.read('C');
    term.read('"');
    term.read('\r');
    term.assertScreen(
        "% A\"",
        "> B",
        "> C\"");
    term.assertAt(3, 0);
    assertEquals("A\"\nB\nC\"", a.get());
  }

/*
  @Test
  public void testPreserveOriginalHandlers() {
    TestTerm term = new TestTerm(this);
    Consumer<int[]> readHandler = buf -> {};
    Consumer<Dimension> sizeHandler = size -> {};
    term.readHandler = readHandler;
    term.sizeHandler = sizeHandler;
    term.readlineComplete();
    assertFalse(term.readHandler == readHandler);
    assertFalse(term.sizeHandler == sizeHandler);
    term.read('\r');
    assertEquals(term.readHandler, readHandler);
    assertEquals(term.sizeHandler, sizeHandler);
  }
*/

  @Test
  public void testBuffering1() {
    LinkedList<int[]> data = new LinkedList<>();
    TestTerm term = new TestTerm(this);
    term.readline.setReadHandler(data::add);
    term.read('h', 'e', 'l', 'l', 'o');
    assertEquals(1, data.size());
    assertEquals("hello", Helper.fromCodePoints(data.get(0)));
    term.assertScreen();
    term.assertAt(0, 0);
  }

  @Test
  public void testBuffering2() {
    LinkedList<int[]> data = new LinkedList<>();
    TestTerm term = new TestTerm(this);
    Supplier<String> line = term.readlineComplete();
    term.readline.setReadHandler(data::add);
    term.read('A', '\r', 'h', 'e', 'l', 'l', 'o');
    assertEquals(1, data.size());
    assertEquals("hello", Helper.fromCodePoints(data.get(0)));
    assertEquals("A", line.get());
  }

  @Test
  public void testBuffering3() {
    LinkedList<int[]> data = new LinkedList<>();
    TestTerm term = new TestTerm(this);
    term.read('A', '\r', 'h', 'e', 'l', 'l', 'o');
    term.assertScreen();
    term.assertAt(0, 0);
    Supplier<String> line = term.readlineComplete();
    term.readline.setReadHandler(data::add);
    term.readline.schedulePending();
    term.executeTasks();
    assertEquals(1, data.size());
    assertEquals("hello", Helper.fromCodePoints(data.get(0)));
    assertEquals("A", line.get());
  }

  @Test
  public void testResize() {
    TestTerm term = new TestTerm(this);
    term.readline.setSizeHandler(dim -> {
      assertEquals(new Dimension(3, 4), term.readline.size());
      assertEquals(new Dimension(3, 4), dim);
      testComplete();
    });
    term.sizeHandler.accept(new Dimension(3, 4));
    await();
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
