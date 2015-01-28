package io.termd.core.readline;

import io.termd.core.Handler;
import io.termd.core.Helper;
import io.termd.core.term.TermEvent;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class EventHandler implements Handler<TermEvent> {

  public final EventQueue eventQueue;
  final Executor scheduler;
  final Map<String, Function> functions = new HashMap<>();
  final Handler<int[]> output;
  final Handler<ReadlineRequest> handler;

  public EventHandler(Handler<int[]> output, Executor scheduler, Handler<ReadlineRequest> handler) {
    this(new EventQueue(), output, scheduler, handler);
  }

  public EventHandler(EventQueue eventQueue, Handler<int[]> output, Executor scheduler, Handler<ReadlineRequest> handler) {
    this.eventQueue = eventQueue;
    this.output = output;
    this.handler = handler;
    this.scheduler = scheduler;
  }

  public EventHandler addFunction(Function function) {
    functions.put(function.getName(), function);
    return this;
  }

  private final AtomicInteger handling = new AtomicInteger();
  private Handler<TermEvent> eventHandler;

  @Override
  public void handle(TermEvent event) {
    if (eventHandler != null) {
      eventHandler.handle(event);
    } else {
      if (event instanceof TermEvent.Read) {
        TermEvent.Read read = (TermEvent.Read) event;
        eventQueue.append(read.getData());
        scheduler.execute(task);
      } else if (event instanceof TermEvent.Size) {
        TermEvent.Size size = (TermEvent.Size) event;
        System.out.println("Window size changed width=" + size.getWidth() + " height=" + size.getHeight());
      }
    }
  }

  public void append(int[] data) {
    handle(new TermEvent.Read(data));
  }

  private class BlockingEventContext implements EventContext {
    final Event event;
    public BlockingEventContext(Event event) {
      this.event = event;
    }
    @Override
    public Event getEvent() {
      return event;
    }
    @Override
    public void end() {
      EventHandler.this.end();
    }
  }

  final Runnable task = new Runnable() {
    @Override
    public void run() {
      if (handling.compareAndSet(0, 2)) {
        if (eventQueue.hasNext()) {
          handle(new BlockingEventContext(eventQueue.next()));
          end(); // Should it be called ?
        } else {
          handling.set(0);
        }
      }
    }
  };

  /**
   * Initialize readline and send the init event.
   */
  public void init() {
    if (handling.compareAndSet(0, 2)) {
      BlockingEventContext context = new BlockingEventContext(new InitEvent());
      handler.handle(new ReadlineRequestImpl(context, 0, null));
      context.end();
    } else {
      throw new IllegalStateException("Invoked at the wrong time");
    }
  }

  private void end() {
    int value = handling.decrementAndGet();
    if (value == 0) {
      scheduler.execute(task);
    } else if (value < 0) {
      throw new AssertionError();
    }
  }

  enum LineStatus {
    LITERAL, ESCAPED, QUOTED
  }

  private final LinkedList<int[]> lines = new LinkedList<>();
  private final LineBuffer lineBuffer = new LineBuffer();
  private LinkedList<Integer> escaped = new LinkedList<>();
  private LineStatus lineStatus = LineStatus.LITERAL;
  private AtomicInteger count = new AtomicInteger();
  private EscapeFilter filter = new EscapeFilter(new Escaper() {
    @Override
    public void escaping() {
      lineStatus = LineStatus.ESCAPED;
    }
    @Override
    public void escaped(int ch) {
      if (ch != '\r') {
        escaped.add((int) '\\');
        escaped.add(ch);
      }
      lineStatus = LineStatus.LITERAL;
    }
    @Override
    public void beginQuotes(int delim) {
      escaped.add(delim);
      lineStatus = LineStatus.QUOTED;
    }
    @Override
    public void endQuotes(int delim) {
      escaped.add(delim);
      lineStatus = LineStatus.LITERAL;
    }
    @Override
    public void handle(Integer event) {
      escaped.add(event);
    }
  });

  private class ReadlineRequestImpl implements ReadlineRequest {

    final EventContext context;
    final int count;
    final String data;
    private boolean done;

    public ReadlineRequestImpl(EventContext context, int count, String data) {
      this.context = context;
      this.data = data;
      this.count = count;
    }

    @Override
    public int requestCount() {
      return count;
    }

    @Override
    public String getData() {
      return data;
    }

    @Override
    public synchronized void eventHandler(Handler<TermEvent> handler) {
      if (done) {
        throw new IllegalStateException("Already ended");
      }
      if (handler != null) {
        eventHandler = handler;
      } else {
        eventHandler = null;
      }
    }

    @Override
    public synchronized ReadlineRequest write(String s) {
      if (done) {
        throw new IllegalStateException("Already ended");
      }
      output.handle(Helper.toCodePoints(s));
      return this;
    }

    @Override
    public synchronized void end() {
      if (done) {
        throw new IllegalStateException("Already ended");
      }
      done = true;
      eventHandler = null;
      context.end();
    }
  }

  public void handle(final EventContext context) {
    LineBuffer copy = new LineBuffer(lineBuffer);
    Event event = context.getEvent();
    if (event instanceof KeyEvent) {
      KeyEvent key = (KeyEvent) event;
      if (key.length() == 1 && key.getAt(0) == '\r') {
        for (int j : lineBuffer) {
          filter.handle(j);
        }
        if (lineStatus == LineStatus.ESCAPED) {
          filter.handle((int) '\r'); // Correct status
          output.handle(new int[]{'\r', '\n', '>', ' '});
          lineBuffer.setSize(0);
          copy.setSize(0);
        } else {
          int[] l = new int[this.escaped.size()];
          for (int index = 0;index < l.length;index++) {
            l[index] = this.escaped.get(index);
          }
          escaped.clear();
          lines.add(l);
          if (lineStatus == LineStatus.QUOTED) {
            output.handle(new int[]{'\r', '\n', '>', ' '});
            lineBuffer.setSize(0);
            copy.setSize(0);
          } else {
            final StringBuilder raw = new StringBuilder();
            for (int index = 0;index < lines.size();index++) {
              int[] a = lines.get(index);
              if (index > 0) {
                raw.append('\n'); // Use \n for processing
              }
              for (int b : a) {
                raw.appendCodePoint(b);
              }
            }
            lines.clear();
            escaped.clear();
            output.handle(new int[]{'\r', '\n'});
            lineBuffer.setSize(0);
            handler.handle(new ReadlineRequestImpl(context, count.incrementAndGet(), raw.toString()));
            return;
          }
        }
      } else {
        for (int i = 0;i < key.length();i++) {
          int codePoint = key.getAt(i);
          lineBuffer.insert(codePoint);
        }
      }
    } else {
      FunctionEvent fname = (FunctionEvent) event;
      Function function = functions.get(fname.getName());
      if (function != null) {
        function.call(lineBuffer);
      } else {
        System.out.println("Unimplemented function " + fname.getName());
      }
    }
    LinkedList<Integer> a = copy.compute(lineBuffer);
    int[] t = new int[a.size()];
    for (int index = 0;index < a.size();index++) {
      t[index] = a.get(index);
    }
    output.handle(t);
    context.end();
  }
}
