package io.termd.core.term;

import io.termd.core.Handler;
import io.termd.core.Helper;
import io.termd.core.readline.Event;
import io.termd.core.readline.EventHandler;
import io.termd.core.readline.Reader;

import java.io.InputStream;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReadlineTerm {

  final TermConnection conn;

  public ReadlineTerm(TermConnection conn) {
    this.conn = conn;

    //
    InputStream inputrc = Reader.class.getResourceAsStream("inputrc");
    final Reader reader = new Reader(inputrc);
    final EventHandler handler = new EventHandler(conn.charsHandler());
    for (io.termd.core.readline.Function function : Helper.loadServices(Thread.currentThread().getContextClassLoader(), io.termd.core.readline.Function.class)) {
      handler.addFunction(function);
    }
    conn.charsHandler(new Handler<int[]>() {
      @Override
      public void handle(int[] chars) {
        reader.append(chars);
        while (true) {
          Event event = reader.reduceOnce().popEvent();
          if (event != null) {
            handler.handle(event);
          } else {
            break;
          }
        }
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
