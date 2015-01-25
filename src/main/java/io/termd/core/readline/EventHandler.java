package io.termd.core.readline;

import io.termd.core.Handler;
import io.termd.core.Helper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class EventHandler implements Handler<EventContext> {

  public final EventQueue eventQueue;
  final Executor scheduler;
  final Map<String, Function> functions = new HashMap<>();
  final Handler<int[]> output;
  final Handler<RequestContext> handler;

  public EventHandler(Handler<int[]> output, Executor scheduler, Handler<RequestContext> handler) {
    this(new EventQueue(), output, scheduler, handler);
  }

  public EventHandler(EventQueue eventQueue, Handler<int[]> output, Executor scheduler, Handler<RequestContext> handler) {
    output.handle(new int[]{'%', ' '});
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

  public void append(int[] chars) {
    eventQueue.append(chars);
    scheduler.execute(task);
  }

  final Runnable task = new Runnable() {
    @Override
    public void run() {
      if (handling.compareAndSet(0, 2)) {
        if (eventQueue.hasNext()) {
          final Event event = eventQueue.next();
          final Runnable self = this;
          EventContext context = new EventContext() {
            @Override
            public Event getEvent() {
              return event;
            }
            @Override
            public void end() {
              EventHandler.this.end();
            }
          };
          handle(context);
          if (handling.decrementAndGet() == 0) {
            scheduler.execute(self);
          }
        } else {
          handling.set(0);
        }
      }

    }
  };

  private void end() {
    if (handling.decrementAndGet() == 0) {
      scheduler.execute(task);
    }
  }

  enum LineStatus {
    LITERAL, ESCAPED, QUOTED
  }

  private final LinkedList<int[]> lines = new LinkedList<>();
  private final LineBuffer lineBuffer = new LineBuffer();
  private LinkedList<Integer> escaped = new LinkedList<>();
  private LineStatus lineStatus = LineStatus.LITERAL;
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
            handler.handle(new RequestContext() {

              @Override
              public String getRaw() {
                return raw.toString();
              }

              @Override
              public RequestContext write(String s) {
                output.handle(Helper.toCodePoints(s));
                return this;
              }

              @Override
              public void end() {
                output.handle(new int[]{'%', ' '});
                context.end();
              }
            });
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
