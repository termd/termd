package examples.snake;

import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Vector;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Snake implements Consumer<TtyConnection> {

  enum Direction {
    LEFT, RIGHT, UP, DOWN
  }

  class Game {

    final int width, height;
    HashSet<Vector> tiles;
    LinkedList<Vector> snake = new LinkedList<>();
    Direction direction;

    Game(int width, int height, int size) {
      this.width = width;
      this.height = height;
      tiles = new HashSet<>();
      while (size > 0) {
        int x = new Random().nextInt(width);
        int y = new Random().nextInt(height);
        Vector tile = new Vector(x, y);
        if (tiles.add(tile)) {
          size--;
        }
      }
      snake.addFirst(new Vector(0, 0));
      snake.addFirst(new Vector(1, 0));
      snake.addFirst(new Vector(2, 0));
      snake.addFirst(new Vector(3, 0));
      direction = Direction.RIGHT;
    }

    void update() throws Exception {
      Vector curr = snake.peekFirst();
      Vector next = null;
      switch (direction) {
        case RIGHT:
          next = new Vector(curr.x() + 1, curr.y());
          break;
        case LEFT:
          next = new Vector(curr.x() - 1, curr.y());
          break;
        case UP:
          next = new Vector(curr.x(), curr.y() - 1);
          break;
        case DOWN:
          next = new Vector(curr.x(), curr.y() + 1);
          break;
      }
      if (next.x() < 0 || next.x() >= width || next.y() < 0 || next.y() >= height || snake.contains(next)) {
        throw new Exception("lost");
      }
      if (!tiles.remove(next)) {
        // Eat a tile : grow
        snake.removeLast();
      }
      snake.addFirst(next);
    }
  }

  @Override
  public void accept(TtyConnection conn) {
    if (conn.size() != null) {
      start(conn);
    } else {
      conn.setSizeHandler(size -> {
        start(conn);
      });
    }
  }

  private void start(TtyConnection conn) {
    new Thread() {

      Game game;

      private void newGame(Vector size) {
        game = new Game(size.x(), size.y(), (size.x() * size.y()) / 25);
      }

      @Override
      public void run() {

        Thread current = Thread.currentThread();

        conn.setSizeHandler(size -> {
          newGame(size);
        });

        conn.setEventHandler((event, ch) -> {
          switch (event) {
            case INTR:
              current.interrupt();
              conn.close();
              break;
          }
        });

        // Keyboard handling
        conn.setStdinHandler(keys -> {
          if (keys.length == 3) {
            if (keys[0] == 27 && keys[1] == '[') {
              switch (keys[2]) {
                case 'A':
                  game.direction = Direction.UP;
                  break;
                case 'B':
                  game.direction = Direction.DOWN;
                  break;
                case 'C':
                  game.direction = Direction.RIGHT;
                  break;
                case 'D':
                  game.direction = Direction.LEFT;
                  break;
              }
            }
          }
        });

        // Init current game
        newGame(conn.size());

        // Now play
        while (true) {
          Game game = this.game;
          StringBuilder buf = new StringBuilder();
          for (int y = 0;y < game.height;y++) {
            buf.append("\033[").append(y + 1).append(";1H\033[K");
          }
          for (Vector tile : game.tiles) {
            buf.append("\033[").append(tile.y() + 1).append(";").append(tile.x() + 1).append("H").append("X");
          }
          for (Vector tile : game.snake) {
            buf.append("\033[").append(tile.y() + 1).append(";").append(tile.x() + 1).append("H").append('0');
          }
          buf.append("\033[").append(game.height).append(";").append(game.width).append("H\033[K");
          conn.write(buf.toString());
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            break;
          }
          try {
            game.update();
          } catch (Exception e) {
            conn.write("YOU LOST");
            conn.close();
            break;
          }
        }
      }
    }.start();
  }
}
