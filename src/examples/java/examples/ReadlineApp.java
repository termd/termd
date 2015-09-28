package examples;

import io.termd.core.readline.Keymap;
import io.termd.core.readline.Readline;
import io.termd.core.tty.TtyConnection;
import io.termd.core.tty.TtyEvent;
import io.termd.core.util.Helper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReadlineApp implements Consumer<TtyConnection> {

  private static final Pattern splitter = Pattern.compile("\\w+");

  public void accept(final TtyConnection conn) {
    InputStream inputrc = Keymap.class.getResourceAsStream("inputrc");
    Keymap keymap = new Keymap(inputrc);
    Readline readline = new Readline(keymap);
    for (io.termd.core.readline.Function function : Helper.loadServices(Thread.currentThread().getContextClassLoader(), io.termd.core.readline.Function.class)) {
      readline.addFunction(function);
    }
    conn.write("Welcome sir\n\n");
    read(conn, readline);
  }

  class Task extends Thread implements BiConsumer<TtyEvent, Integer> {

    final TtyConnection conn;
    final Readline readline;
    final Command command;
    final List<String> args;
    volatile boolean sleeping;

    public Task(TtyConnection conn, Readline readline, Command command, List<String> args) {
      this.conn = conn;
      this.readline = readline;
      this.command = command;
      this.args = args;
    }

    @Override
    public void accept(TtyEvent event, Integer cp) {
      switch (event) {
        case INTR:
          if (sleeping) {
            interrupt();
          }
      }
    }

    @Override
    public void run() {
//      conn.write("Running " + line + "\n");
      conn.setEventHandler(this);
      sleeping = true;
      try {
        command.execute(conn, args);
      } catch (InterruptedException e) {
        // Interrupted
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        sleeping = false;
        conn.setEventHandler(null);
        read(conn, readline);
      }
    }

  }

  interface Command {
    void execute(TtyConnection conn, List<String> args) throws Exception;
  }

  private void sleep(TtyConnection conn, List<String> args) throws Exception {
    if (args.isEmpty()) {
      conn.write("usage: sleep seconds");
      return;
    }
    int time = -1;
    try {
      time = Integer.parseInt(args.get(0));
    } catch (NumberFormatException ignore) {
    }
    if (time > 0) {
      Thread.sleep(time * 1000);
    }
  }


  public void read(final TtyConnection conn, final Readline readline) {
    readline.readline(conn, "% ", line -> {

      // Ctrl-D
      if (line == null) {
        conn.write("logout\n").close();
        return;
      }

      Matcher matcher = splitter.matcher(line);
      if (matcher.find()) {
        String cmd = matcher.group();
        List<String> rest = new ArrayList<>();
        while (matcher.find()) {
          rest.add(matcher.group());
        }
        Command command = null;
        switch (cmd) {
          case "sleep":
            command  = this::sleep;
            break;
          default:
        }
        if (command != null) {
          new Task(conn, readline, command, rest).start();
          return;
        } else {
          conn.write(cmd + ": command not found\n");
        }
      }
      read(conn, readline);
    });
  }
}
