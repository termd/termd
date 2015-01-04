package io.termd.core.term;

import io.termd.core.Handler;
import io.termd.core.Helper;
import io.termd.core.readline.Event;
import io.termd.core.readline.EventHandler;
import io.termd.core.readline.EventMapper;
import io.termd.core.readline.RequestContext;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReadlineTerm {

  private static final int READY = 0;
  private static final int HANDLING = 1;
  private static final int ASYNC = 2;

  final TermConnection conn;

  public ReadlineTerm(TermConnection conn) {
    this(conn, new Handler<RequestContext>() {
      @Override
      public void handle(final RequestContext event) {
        System.out.println("Line " + event.getRaw());
        new Thread() {
          @Override
          public void run() {
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            event.end();
          }
        }.start();
      }
    });
  }

  public ReadlineTerm(TermConnection conn, Handler<RequestContext> requestHandler) {
    this.conn = conn;

    //
    InputStream inputrc = EventMapper.class.getResourceAsStream("inputrc");
    final EventMapper eventMapper = new EventMapper(inputrc);
    final EventHandler handler = new EventHandler(conn.charsHandler(), requestHandler);
    for (io.termd.core.readline.Function function : Helper.loadServices(Thread.currentThread().getContextClassLoader(), io.termd.core.readline.Function.class)) {
      handler.addFunction(function);
    }

    //
    final AtomicInteger status = new AtomicInteger(READY);

    //
    final Runnable task = new Runnable() {
      @Override
      public void run() {
        final Runnable self = this;
        while (true) {
          if (status.compareAndSet(READY, HANDLING)) {
            Event event = eventMapper.reduceOnce().popEvent();
            if (event != null) {
              handler.handle(event, new Handler<Void>() {
                @Override
                public void handle(Void event) {
                  if (status.compareAndSet(HANDLING, READY)) {
                  } else {
                    status.set(READY);
                    ReadlineTerm.this.conn.schedule(self);
                  }
                }
              });
              if (status.compareAndSet(HANDLING, ASYNC)) {
                break;
              }
            } else {
              status.set(READY);
              break;
            }
          }

        }
      }
    };

    //
    conn.charsHandler(new Handler<int[]>() {
      @Override
      public void handle(int[] chars) {
        eventMapper.append(chars);
        task.run();
      }
    });

    conn.sizeHandler(new Handler<Map.Entry<Integer, Integer>>() {
      @Override
      public void handle(Map.Entry<Integer, Integer> event) {
        System.out.println("Window size changed width=" + event.getKey() + " height=" + event.getValue());
      }
    });
  }
}
