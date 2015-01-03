package io.termd.core.readline;

/**
 * Goal : translate a codepoint sequence into a symbol.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public enum Keys implements KeyEvent {

  CTRL_AROBASE("ctrl-@", 0),
  CTRL_A("Ctrl-A", 1),
  CTRL_B("Ctrl-B", 2),
  CTRL_C("Ctrl-C", 3),
  CTRL_D("Ctrl-D", 4),
  CTRL_E("Ctrl-E", 5),
  CTRL_F("Ctrl-F", 6),
  CTRL_G("Ctrl-G", 7),
  CTRL_H("Ctrl-H", 8),
  CTRL_I("Ctrl-I", 9),
  CTRL_J("Ctrl-J", 10),
  CTRL_K("Ctrl-K", 11),
  CTRL_L("Ctrl-L", 12),
  CTRL_M("Ctrl-M", 13),
  CTRL_N("Ctrl-N", 14),
  CTRL_O("Ctrl-O", 15),
  CTRL_P("Ctrl-P", 16),
  CTRL_Q("Ctrl-Q", 17),
  CTRL_R("Ctrl-R", 18),
  CTRL_S("Ctrl-S", 19),
  CTRL_T("Ctrl-T", 20),
  CTRL_U("Ctrl-U", 21),
  CTRL_V("Ctrl-V", 22),
  CTRL_W("Ctrl-W", 23),
  CTRL_X("Ctrl-X", 24),
  CTRL_Y("Ctrl-Y", 25),
  CTRL_Z("Ctrl-Z", 26),
  CTRL_LEFT_BRACE("Ctrl-[", 27),
  CTRL_ANTI_SLASH("Ctrl-\\", 28), // `
  CTRL_RIGHT_BRACE("Ctrl-]", 29),
  CTRL_CARRET("Ctrl-^", 30), // ^
  CTRL_UNDERSCORE("Ctrl-_", 31),

  A("A", 'A'), B("B", 'B'), C("C", 'C'), QUOTE("\"", '"'),
  BACKSLASH("\\", '\\'),

  UP("up", 27, '[', 'A'),
  DOWN("down", 27, '[', 'B'),
  RIGHT("right", 27, '[', 'C'),
  LEFT("left", 27, '[', 'D'),

  SHIFT_RIGHT("", 27, ']', '1', ';', '2', 'C'),
  SHIFT_LEFT("", 27, ']', '1', ';', '2', 'D');

  final String name;
  final int[] sequence;

  Keys(String name, int... sequence) {
    this.name = name;
    this.sequence = sequence;
  }


  @Override
  public int getAt(int index) {
    return sequence[index];
  }

  @Override
  public int length() {
    return sequence.length;
  }


  @Override
  public String toString() {
    return "key:" + name;
  }
}
