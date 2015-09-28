package examples.snake;

import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Vector;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * The snake game implementation, fully non blocking, one thread to handle all players : massive scalability
 */
public class SnakeGame implements Consumer<TtyConnection> {

  @Override
  public void accept(TtyConnection conn) {
    if (conn.size() != null) {
      new Game(conn).execute();
    } else {
      conn.setSizeHandler(size -> new Game(conn).execute());
    }
  }

  enum Direction {
    LEFT, RIGHT, UP, DOWN
  }

  /**
   * The game automaton state
   */
  class GameState {

    final int width, height;
    HashSet<Vector> tiles;
    LinkedList<Vector> snake = new LinkedList<>();
    Direction direction;

    GameState(int width, int height, int size) {
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

    /**
     * Update the state with one game iteration
     *
     * @throws Exception when user lose
     */
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

  /**
   * The game itself.
   */
  class Game {

    final TtyConnection conn;
    GameState game;
    boolean interrupted;

    public Game(TtyConnection conn) {
      this.conn = conn;

      // When user resize the screen : launch a new game
      conn.setSizeHandler(this::reset);

      // Ctrl-C ends the game
      conn.setEventHandler((event, ch) -> {
        switch (event) {
          case INTR:
            interrupted = true;
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
      reset(conn.size());
    }

    /**
     * Execute one iteration of the game, at the end schedule the next iteration until user lose or hits Ctrl-C
     */
    void execute() {
      if (interrupted) {
        return;
      }
      GameState game = this.game;

      // Compute the ANSI magic string that draws the game
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

      // Update screen
      conn.write(buf.toString());

      // Now update game and handle losing the game
      try {
        game.update();
      } catch (Exception e) {
        conn.write("YOU LOST");
        conn.close();
        return;
      }

      // Schedule a new execution of the game
      conn.schedule(this::execute, 500, TimeUnit.MILLISECONDS);
    }

    private void reset(Vector size) {
      // Fill factory area / 25
      game = new GameState(size.x(), size.y(), (size.x() * size.y()) / 10);
    }
  }
}
