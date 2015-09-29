package examples.readlinefunction;

import examples.readline.ReadlineExample;
import io.termd.core.readline.Function;
import io.termd.core.readline.Keymap;
import io.termd.core.readline.LineBuffer;
import io.termd.core.readline.Readline;
import io.termd.core.tty.TtyConnection;

/**
 * Shows how to extend readline with custom functions.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReadlineFunctionExample {

  public static void handle(TtyConnection conn) {

    // The reverse function simply reverse the edit buffer
    Function reverseFunction = new Function() {
      @Override
      public String name() {
        return "reverse";
      }

      @Override
      public void apply(Readline.Interaction interaction) {
        int[] points = interaction.buffer().toArray();

        // Reverse the buffer
        for (int i = 0; i < points.length / 2; i++) {
          int temp = points[i];
          points[i] = points[points.length - 1 - i];
          points[points.length - 1 - i] = temp;
        }

        // Refresh buffer
        interaction.refresh(new LineBuffer().insert(points));

        // Resume readline
        interaction.resume();
      }
    };

    ReadlineExample.readline(
        // Bind reverse to Ctrl-g to the reverse function
        new Readline(Keymap.getDefault().bindFunction("\\C-g", "reverse")).
            addFunctions(Function.loadDefaults()).addFunction(reverseFunction),
        conn);
  }
}
