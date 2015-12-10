package examples.readlinefunction;

import io.termd.core.readline.Function;
import io.termd.core.readline.LineBuffer;
import io.termd.core.readline.Readline;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReverseFunction implements Function {

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
}
