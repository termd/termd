package io.termd.core.readline;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class KeymapTest {

  @Test
  public void bindFunction1() {
    Keymap keymap = new Keymap();
    keymap.bindFunction("\\C-j", "my-func");
    EventQueue eq = new EventQueue(keymap);
    eq.append('J' - 64);
    assertEquals("my-func", ((FunctionEvent) eq.next()).name());
    assertFalse(eq.hasNext());
  }

  @Test
  public void bindFunction2() {
    Keymap keymap = new Keymap();
    keymap.bindFunction(new int[]{'J' - 64}, "my-func");
    EventQueue eq = new EventQueue(keymap);
    eq.append('J' - 64);
    assertEquals("my-func", ((FunctionEvent) eq.next()).name());
    assertFalse(eq.hasNext());
  }
}
