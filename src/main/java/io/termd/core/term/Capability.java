/*
 * Copyright 2015 Julien Viet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.termd.core.term;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Capability<T> {

  private static final Map<String, Capability<?>> values = new HashMap<>();

  private static class Enumerated<T> extends Capability<T> {
    public Enumerated(Class<T> type, String variable, String name, String cap, String description) {
      super(type, variable, name, cap, description);
      values.put(name, this);
    }
  }

  public static Capability<?> getCapability(String name) {
    return values.get(name);
  }

  public static <T> Capability<T> getCapability(String name, Class<T> type) {
    Capability<?> capability = values.get(name);
    if (capability != null && capability.type.equals(type)) {
      return (Capability<T>) capability;
    }
    return null;
  }

  public static Capability<Boolean> auto_left_margin = new Enumerated<>(Boolean.class, "auto_left_margin", "bw", "bw", "cub1 wraps from column 0 to last column");
  public static Capability<Boolean> auto_right_margin = new Enumerated<>(Boolean.class, "auto_right_margin", "am", "am", "Terminal has automatic margins");
  public static Capability<Boolean> back_color_erase = new Enumerated<>(Boolean.class, "back_color_erase", "bce", "ut", "Screen erased with background colour");
  public static Capability<Boolean> can_change = new Enumerated<>(Boolean.class, "can_change", "ccc", "cc", "Terminal can re-define existing colour");
  public static Capability<Boolean> ceol_standout_glitch = new Enumerated<>(Boolean.class, "ceol_standout_glitch", "xhp", "xs", "Standout not erased by overwriting (hp)");
  public static Capability<Boolean> col_addr_glitch = new Enumerated<>(Boolean.class, "col_addr_glitch", "xhpa", "YA", "Only positive motion for hpa/mhpa caps");
  public static Capability<Boolean> cpi_changes_res = new Enumerated<>(Boolean.class, "cpi_changes_res", "cpix", "YF", "Changing character pitch changes resolution");
  public static Capability<Boolean> cr_cancels_micro_mode = new Enumerated<>(Boolean.class, "cr_cancels_micro_mode", "crxm", "YB", "Using cr turns off micro mode");
  public static Capability<Boolean> dest_tabs_magic_smso = new Enumerated<>(Boolean.class, "dest_tabs_magic_smso", "xt", "xt", "\"Destructive tabs");
  public static Capability<Boolean> eat_newline_glitch = new Enumerated<>(Boolean.class, "eat_newline_glitch", "xenl", "xn", "Newline ignored after 80 columns (Concept)");
  public static Capability<Boolean> erase_overstrike = new Enumerated<>(Boolean.class, "erase_overstrike", "eo", "eo", "Can erase overstrikes with a blank");
  public static Capability<Boolean> generic_type = new Enumerated<>(Boolean.class, "generic_type", "gn", "gn", "\"Generic line type (e.g.");
  public static Capability<Boolean> hard_copy = new Enumerated<>(Boolean.class, "hard_copy", "hc", "hc", "Hardcopy terminal");
  public static Capability<Boolean> hard_cursor = new Enumerated<>(Boolean.class, "hard_cursor", "chts", "HC", "Cursor is hard to see");
  public static Capability<Boolean> has_meta_key = new Enumerated<>(Boolean.class, "has_meta_key", "km", "km", "\"Has a meta key (shift");
  public static Capability<Boolean> has_print_wheel = new Enumerated<>(Boolean.class, "has_print_wheel", "daisy", "YC", "Printer needs operator to change character set");
  public static Capability<Boolean> has_status_line = new Enumerated<>(Boolean.class, "has_status_line", "hs", "hs", "\"Has extra \"\"status line\"\"\"");
  public static Capability<Boolean> hue_lightness_saturation = new Enumerated<>(Boolean.class, "hue_lightness_saturation", "hls", "hl", "Terminal uses only HLS colour notation (Tektronix)");
  public static Capability<Boolean> insert_null_glitch = new Enumerated<>(Boolean.class, "insert_null_glitch", "in", "in", "Insert mode distinguishes nulls");
  public static Capability<Boolean> lpi_changes_res = new Enumerated<>(Boolean.class, "lpi_changes_res", "lpix", "YG", "Changing line pitch changes resolution");
  public static Capability<Boolean> memory_above = new Enumerated<>(Boolean.class, "memory_above", "da", "da", "Display may be retained above the screen");
  public static Capability<Boolean> memory_below = new Enumerated<>(Boolean.class, "memory_below", "db", "db", "Display may be retained below the screen");
  public static Capability<Boolean> move_insert_mode = new Enumerated<>(Boolean.class, "move_insert_mode", "mir", "mi", "Safe to move while in insert mode");
  public static Capability<Boolean> move_standout_mode = new Enumerated<>(Boolean.class, "move_standout_mode", "msgr", "ms", "Safe to move in standout modes");
  public static Capability<Boolean> needs_xon_xoff = new Enumerated<>(Boolean.class, "needs_xon_xoff", "nxon", "nx", "\"Padding won't work");
  public static Capability<Boolean> no_esc_ctlc = new Enumerated<>(Boolean.class, "no_esc_ctlc", "xsb", "xb", "\"Beehive (f1=escape");
  public static Capability<Boolean> no_pad_char = new Enumerated<>(Boolean.class, "no_pad_char", "npc", "NP", "Pad character doesn't exist");
  public static Capability<Boolean> non_dest_scroll_region = new Enumerated<>(Boolean.class, "non_dest_scroll_region", "ndscr", "ND", "Scrolling region is nondestructive");
  public static Capability<Boolean> non_rev_rmcup = new Enumerated<>(Boolean.class, "non_rev_rmcup", "nrrmc", "NR", "smcup does not reverse rmcup");
  public static Capability<Boolean> over_strike = new Enumerated<>(Boolean.class, "over_strike", "os", "os", "Terminal overstrikes on hard-copy terminal");
  public static Capability<Boolean> prtr_silent = new Enumerated<>(Boolean.class, "prtr_silent", "mc5i", "5i", "Printer won't echo on screen");
  public static Capability<Boolean> row_addr_glitch = new Enumerated<>(Boolean.class, "row_addr_glitch", "xvpa", "YD", "Only positive motion for vpa/mvpa caps");
  public static Capability<Boolean> semi_auto_right_margin = new Enumerated<>(Boolean.class, "semi_auto_right_margin", "sam", "YE", "Printing in last column causes cr");
  public static Capability<Boolean> status_line_esc_ok = new Enumerated<>(Boolean.class, "status_line_esc_ok", "eslok", "es", "Escape can be used on the status line");
  public static Capability<Boolean> tilde_glitch = new Enumerated<>(Boolean.class, "tilde_glitch", "hz", "hz", "Hazeltine; can't print tilde (~)");
  public static Capability<Boolean> transparent_underline = new Enumerated<>(Boolean.class, "transparent_underline", "ul", "ul", "Underline character overstrikes");
  public static Capability<Boolean> xon_xoff = new Enumerated<>(Boolean.class, "xon_xoff", "xon", "xo", "Terminal uses xon/xoff handshaking");

  public static Capability<Integer> bit_image_entwining = new Enumerated<>(Integer.class, "bit_image_entwining", "bitwin", "Yo", "Number of passes for each bit-map row");
  public static Capability<Integer> bit_image_type = new Enumerated<>(Integer.class, "bit_image_type", "bitype", "Yp", "Type of bit image device");
  public static Capability<Integer> buffer_capacity = new Enumerated<>(Integer.class, "buffer_capacity", "bufsz", "Ya", "Number of bytes buffered before printing");
  public static Capability<Integer> buttons = new Enumerated<>(Integer.class, "buttons", "btns", "BT", "Number of buttons on the mouse");
  public static Capability<Integer> columns = new Enumerated<>(Integer.class, "columns", "cols", "co", "Number of columns in a line");
  public static Capability<Integer> dot_horz_spacing = new Enumerated<>(Integer.class, "dot_horz_spacing", "spinh", "Yc", "Spacing of dots horizontally in dots per inch");
  public static Capability<Integer> dot_vert_spacing = new Enumerated<>(Integer.class, "dot_vert_spacing", "spinv", "Yb", "Spacing of pins vertically in pins per inch");
  public static Capability<Integer> init_tabs = new Enumerated<>(Integer.class, "init_tabs", "it", "it", "Tabs initially every # spaces");
  public static Capability<Integer> label_height = new Enumerated<>(Integer.class, "label_height", "lh", "lh", "Number of rows in each label");
  public static Capability<Integer> label_width = new Enumerated<>(Integer.class, "label_width", "lw", "lw", "Number of columns in each label");
  public static Capability<Integer> lines = new Enumerated<>(Integer.class, "lines", "lines", "li", "Number of lines on a screen or a page");
  public static Capability<Integer> lines_of_memory = new Enumerated<>(Integer.class, "lines_of_memory", "lm", "lm", "Lines of memory if > lines; 0 means varies");
  public static Capability<Integer> max_attributes = new Enumerated<>(Integer.class, "max_attributes", "ma", "ma", "Maximum combined video attributes terminal can display");
  public static Capability<Integer> magic_cookie_glitch = new Enumerated<>(Integer.class, "magic_cookie_glitch", "xmc", "sg", "Number of blank characters left by smso or rmso");
  public static Capability<Integer> max_colors = new Enumerated<>(Integer.class, "max_colors", "colors", "Co", "Maximum number of colours on the screen");
  public static Capability<Integer> max_micro_address = new Enumerated<>(Integer.class, "max_micro_address", "maddr", "Yd", "Maximum value in micro_..._address");
  public static Capability<Integer> max_micro_jump = new Enumerated<>(Integer.class, "max_micro_jump", "mjump", "Ye", "Maximum value in parm_..._micro");
  public static Capability<Integer> max_pairs = new Enumerated<>(Integer.class, "max_pairs", "pairs", "pa", "Maximum number of colour-pairs on the screen");
  public static Capability<Integer> maximum_windows = new Enumerated<>(Integer.class, "maximum_windows", "wnum", "MW", "Maximum number of definable windows");
  public static Capability<Integer> micro_col_size = new Enumerated<>(Integer.class, "micro_col_size", "mcs", "Yf", "Character step size when in micro mode");
  public static Capability<Integer> micro_line_size = new Enumerated<>(Integer.class, "micro_line_size", "mls", "Yg", "Line step size when in micro mode");
  public static Capability<Integer> no_color_video = new Enumerated<>(Integer.class, "no_color_video", "ncv", "NC", "Video attributes that can't be used with colours");
  public static Capability<Integer> num_labels = new Enumerated<>(Integer.class, "num_labels", "nlab", "Nl", "Number of labels on screen (start at 1)");
  public static Capability<Integer> number_of_pins = new Enumerated<>(Integer.class, "number_of_pins", "npins", "Yh", "Number of pins in print-head");
  public static Capability<Integer> output_res_char = new Enumerated<>(Integer.class, "output_res_char", "orc", "Yi", "Horizontal resolution in units per character");
  public static Capability<Integer> output_res_line = new Enumerated<>(Integer.class, "output_res_line", "orl", "Yj", "Vertical resolution in units per line");
  public static Capability<Integer> output_res_horz_inch = new Enumerated<>(Integer.class, "output_res_horz_inch", "orhi", "Yk", "Horizontal resolution in units per inch");
  public static Capability<Integer> output_res_vert_inch = new Enumerated<>(Integer.class, "output_res_vert_inch", "orvi", "Yl", "Vertical resolution in units per inch");
  public static Capability<Integer> padding_baud_rate = new Enumerated<>(Integer.class, "padding_baud_rate", "pb", "pb", "Lowest baud rate where padding needed");
  public static Capability<Integer> print_rate = new Enumerated<>(Integer.class, "print_rate", "cps", "Ym", "Print rate in characters per second");
  public static Capability<Integer> virtual_terminal = new Enumerated<>(Integer.class, "virtual_terminal", "vt", "vt", "Virtual terminal number");
  public static Capability<Integer> wide_char_size = new Enumerated<>(Integer.class, "wide_char_size", "widcs", "Yn", "Character step size when in double-wide mode");
  public static Capability<Integer> width_status_line = new Enumerated<>(Integer.class, "width_status_line", "wsl", "ws", "Number of columns in status line");

  public static Capability<Sequence> acs_chars = new Enumerated<>(Sequence.class, "acs_chars", "acsc", "ac", "Graphic charset pairs aAbBcC");
  public static Capability<Sequence> alt_scancode_esc = new Enumerated<>(Sequence.class, "alt_scancode_esc", "scesa", "S8", "Alternate escape for scancode emulation (default is for VT100)");
  public static Capability<Sequence> back_tab = new Enumerated<>(Sequence.class, "back_tab", "cbt", "bt", "Back tab");
  public static Capability<Sequence> bell = new Enumerated<>(Sequence.class, "bell", "bel", "bl", "Audible signal (bell)");
  public static Capability<Sequence> bit_image_carriage_return = new Enumerated<>(Sequence.class, "bit_image_carriage_return", "bicr", "Yv", "Move to beginning of same row");
  public static Capability<Sequence> bit_image_newline = new Enumerated<>(Sequence.class, "bit_image_newline", "binel", "Zz", "Move to next row of the bit image");
  public static Capability<Sequence> bit_image_repeat = new Enumerated<>(Sequence.class, "bit_image_repeat", "birep", "Xy", "Repeat bit-image cell #1 #2 times");
  public static Capability<Sequence> carriage_return = new Enumerated<>(Sequence.class, "carriage_return", "cr", "cr", "Carriage return");
  public static Capability<Sequence> change_char_pitch = new Enumerated<>(Sequence.class, "change_char_pitch", "cpi", "ZA", "Change number of characters per inch");
  public static Capability<Sequence> change_line_pitch = new Enumerated<>(Sequence.class, "change_line_pitch", "lpi", "ZB", "Change number of lines per inch");
  public static Capability<Sequence> change_res_horz = new Enumerated<>(Sequence.class, "change_res_horz", "chr", "ZC", "Change horizontal resolution");
  public static Capability<Sequence> change_res_vert = new Enumerated<>(Sequence.class, "change_res_vert", "cvr", "ZD", "Change vertical resolution");
  public static Capability<Sequence> change_scroll_region = new Enumerated<>(Sequence.class, "change_scroll_region", "csr", "cs", "Change to lines #1 through #2 (VT100)");
  public static Capability<Sequence> char_padding = new Enumerated<>(Sequence.class, "char_padding", "rmp", "rP", "Like ip but when in replace mode");
  public static Capability<Sequence> char_set_names = new Enumerated<>(Sequence.class, "char_set_names", "csnm", "Zy", "Returns a list of character set names");
  public static Capability<Sequence> clear_all_tabs = new Enumerated<>(Sequence.class, "clear_all_tabs", "tbc", "ct", "Clear all tab stops");
  public static Capability<Sequence> clear_margins = new Enumerated<>(Sequence.class, "clear_margins", "mgc", "MC", "\"Clear all margins (top");
  public static Capability<Sequence> clear_screen = new Enumerated<>(Sequence.class, "clear_screen", "clear", "cl", "Clear screen and home cursor");
  public static Capability<Sequence> clr_bol = new Enumerated<>(Sequence.class, "clr_bol", "el1", "cb", "\"Clear to beginning of line");
  public static Capability<Sequence> clr_eol = new Enumerated<>(Sequence.class, "clr_eol", "el", "ce", "Clear to end of line");
  public static Capability<Sequence> clr_eos = new Enumerated<>(Sequence.class, "clr_eos", "ed", "cd", "Clear to end of display");
  public static Capability<Sequence> code_set_init = new Enumerated<>(Sequence.class, "code_set_init", "csin", "ci", "Init sequence for multiple codesets");
  public static Capability<Sequence> color_names = new Enumerated<>(Sequence.class, "color_names", "colornm", "Yw", "Give name for colour #1");
  public static Capability<Sequence> column_address = new Enumerated<>(Sequence.class, "column_address", "hpa", "ch", "Set horizontal position to absolute #1");
  public static Capability<Sequence> command_character = new Enumerated<>(Sequence.class, "command_character", "cmdch", "CC", "Terminal settable cmd character in prototype");
  public static Capability<Sequence> create_window = new Enumerated<>(Sequence.class, "create_window", "cwin", "", "\"Define win #1 to go from #2");
  public static Capability<Sequence> cursor_address = new Enumerated<>(Sequence.class, "cursor_address", "cup", "cm", "Move to row #1 col #2");
  public static Capability<Sequence> cursor_down = new Enumerated<>(Sequence.class, "cursor_down", "cud1", "do", "Down one line");
  public static Capability<Sequence> cursor_home = new Enumerated<>(Sequence.class, "cursor_home", "home", "ho", "Home cursor (if no cup)");
  public static Capability<Sequence> cursor_invisible = new Enumerated<>(Sequence.class, "cursor_invisible", "civis", "vi", "Make cursor invisible");
  public static Capability<Sequence> cursor_left = new Enumerated<>(Sequence.class, "cursor_left", "cub1", "le", "Move left one space.");
  public static Capability<Sequence> cursor_mem_address = new Enumerated<>(Sequence.class, "cursor_mem_address", "mrcup", "CM", "Memory relative cursor addressing");
  public static Capability<Sequence> cursor_normal = new Enumerated<>(Sequence.class, "cursor_normal", "cnorm", "ve", "Make cursor appear normal (undo vs/vi)");
  public static Capability<Sequence> cursor_right = new Enumerated<>(Sequence.class, "cursor_right", "cuf1", "nd", "Non-destructive space (cursor or carriage right)");
  public static Capability<Sequence> cursor_to_ll = new Enumerated<>(Sequence.class, "cursor_to_ll", "ll", "ll", "\"Last line");
  public static Capability<Sequence> cursor_up = new Enumerated<>(Sequence.class, "cursor_up", "cuu1", "up", "Upline (cursor up)");
  public static Capability<Sequence> cursor_visible = new Enumerated<>(Sequence.class, "cursor_visible", "cvvis", "vs", "Make cursor very visible");
  public static Capability<Sequence> define_bit_image_region = new Enumerated<>(Sequence.class, "define_bit_image_region", "defbi", "Yx", "Define rectangular bit-image region");
  public static Capability<Sequence> define_char = new Enumerated<>(Sequence.class, "define_char", "defc", "ZE", "Define a character in a character set");
  public static Capability<Sequence> delete_character = new Enumerated<>(Sequence.class, "delete_character", "dch1", "dc", "Delete character");
  public static Capability<Sequence> delete_line = new Enumerated<>(Sequence.class, "delete_line", "dl1", "dl", "Delete line");
  public static Capability<Sequence> device_type = new Enumerated<>(Sequence.class, "device_type", "devt", "dv", "Indicate language/codeset support");
  public static Capability<Sequence> dial_phone = new Enumerated<>(Sequence.class, "dial_phone", "dial", "DI", "Dial phone number #1");
  public static Capability<Sequence> dis_status_line = new Enumerated<>(Sequence.class, "dis_status_line", "dsl", "ds", "Disable status line");
  public static Capability<Sequence> display_clock = new Enumerated<>(Sequence.class, "display_clock", "dclk", "DK", "Display time-of-day clock");
  public static Capability<Sequence> display_pc_char = new Enumerated<>(Sequence.class, "display_pc_char", "dispc", "S1", "Display PC character");
  public static Capability<Sequence> down_half_line = new Enumerated<>(Sequence.class, "down_half_line", "hd", "hd", "Half-line down (forward 1/2 linefeed)");
  public static Capability<Sequence> ena_acs = new Enumerated<>(Sequence.class, "ena_acs", "enacs", "eA", "Enable alternate character set");
  public static Capability<Sequence> end_bit_image_region = new Enumerated<>(Sequence.class, "end_bit_image_region", "endbi", "Yy", "End a bit-image region");
  public static Capability<Sequence> enter_alt_charset_mode = new Enumerated<>(Sequence.class, "enter_alt_charset_mode", "smacs", "as", "Start alternate character set");
  public static Capability<Sequence> enter_am_mode = new Enumerated<>(Sequence.class, "enter_am_mode", "smam", "SA", "Turn on automatic margins");
  public static Capability<Sequence> enter_blink_mode = new Enumerated<>(Sequence.class, "enter_blink_mode", "blink", "mb", "Turn on blinking");
  public static Capability<Sequence> enter_bold_mode = new Enumerated<>(Sequence.class, "enter_bold_mode", "bold", "md", "Turn on bold (extra bright) mode");
  public static Capability<Sequence> enter_ca_mode = new Enumerated<>(Sequence.class, "enter_ca_mode", "smcup", "ti", "String to begin programs that use cup");
  public static Capability<Sequence> enter_delete_mode = new Enumerated<>(Sequence.class, "enter_delete_mode", "smdc", "dm", "Delete mode (enter)");
  public static Capability<Sequence> enter_dim_mode = new Enumerated<>(Sequence.class, "enter_dim_mode", "dim", "mh", "Turn on half-bright mode");
  public static Capability<Sequence> enter_doublewide_mode = new Enumerated<>(Sequence.class, "enter_doublewide_mode", "swidm", "ZF", "Enable double wide printing");
  public static Capability<Sequence> enter_draft_quality = new Enumerated<>(Sequence.class, "enter_draft_quality", "sdrfq", "ZG", "Set draft quality print");
  public static Capability<Sequence> enter_horizontal_hl_mode = new Enumerated<>(Sequence.class, "enter_horizontal_hl_mode", "ehhlm", "", "Turn on horizontal highlight mode");
  public static Capability<Sequence> enter_insert_mode = new Enumerated<>(Sequence.class, "enter_insert_mode", "smir", "im", "Insert mode (enter)");
  public static Capability<Sequence> enter_italics_mode = new Enumerated<>(Sequence.class, "enter_italics_mode", "sitm", "ZH", "Enable italics");
  public static Capability<Sequence> enter_left_hl_mode = new Enumerated<>(Sequence.class, "enter_left_hl_mode", "elhlm", "", "Turn on left highlight mode");
  public static Capability<Sequence> enter_leftward_mode = new Enumerated<>(Sequence.class, "enter_leftward_mode", "slm", "ZI", "Enable leftward carriage motion");
  public static Capability<Sequence> enter_low_hl_mode = new Enumerated<>(Sequence.class, "enter_low_hl_mode", "elohlm", "", "Turn on low highlight mode");
  public static Capability<Sequence> enter_micro_mode = new Enumerated<>(Sequence.class, "enter_micro_mode", "smicm", "ZJ", "Enable micro motion capabilities");
  public static Capability<Sequence> enter_near_letter_quality = new Enumerated<>(Sequence.class, "enter_near_letter_quality", "snlq", "ZK", "Set near-letter quality print");
  public static Capability<Sequence> enter_normal_quality = new Enumerated<>(Sequence.class, "enter_normal_quality", "snrmq", "ZL", "Set normal quality print");
  public static Capability<Sequence> enter_pc_charset_mode = new Enumerated<>(Sequence.class, "enter_pc_charset_mode", "smpch", "S2", "Enter PC character display mode");
  public static Capability<Sequence> enter_protected_mode = new Enumerated<>(Sequence.class, "enter_protected_mode", "prot", "mp", "Turn on protected mode");
  public static Capability<Sequence> enter_reverse_mode = new Enumerated<>(Sequence.class, "enter_reverse_mode", "rev", "mr", "Turn on reverse video mode");
  public static Capability<Sequence> enter_right_hl_mode = new Enumerated<>(Sequence.class, "enter_right_hl_mode", "erhlm", "", "Turn on right highlight mode");
  public static Capability<Sequence> enter_scancode_mode = new Enumerated<>(Sequence.class, "enter_scancode_mode", "smsc", "S4", "Enter PC scancode mode");
  public static Capability<Sequence> enter_secure_mode = new Enumerated<>(Sequence.class, "enter_secure_mode", "invis", "mk", "Turn on blank mode (characters invisible)");
  public static Capability<Sequence> enter_shadow_mode = new Enumerated<>(Sequence.class, "enter_shadow_mode", "sshm", "ZM", "Enable shadow printing");
  public static Capability<Sequence> enter_standout_mode = new Enumerated<>(Sequence.class, "enter_standout_mode", "smso", "so", "Begin standout mode");
  public static Capability<Sequence> enter_subscript_mode = new Enumerated<>(Sequence.class, "enter_subscript_mode", "ssubm", "ZN", "Enable subscript printing");
  public static Capability<Sequence> enter_superscript_mode = new Enumerated<>(Sequence.class, "enter_superscript_mode", "ssupm", "ZO", "Enable superscript printing");
  public static Capability<Sequence> enter_top_hl_mode = new Enumerated<>(Sequence.class, "enter_top_hl_mode", "ethlm", "", "Turn on top highlight mode");
  public static Capability<Sequence> enter_underline_mode = new Enumerated<>(Sequence.class, "enter_underline_mode", "smul", "us", "Start underscore mode");
  public static Capability<Sequence> enter_upward_mode = new Enumerated<>(Sequence.class, "enter_upward_mode", "sum", "ZP", "Enable upward carriage motion");
  public static Capability<Sequence> enter_vertical_hl_mode = new Enumerated<>(Sequence.class, "enter_vertical_hl_mode", "evhlm", "", "Turn on vertical highlight mode");
  public static Capability<Sequence> enter_xon_mode = new Enumerated<>(Sequence.class, "enter_xon_mode", "smxon", "SX", "Turn on xon/xoff handshaking");
  public static Capability<Sequence> erase_chars = new Enumerated<>(Sequence.class, "erase_chars", "ech", "ec", "Erase #1 characters");
  public static Capability<Sequence> exit_alt_charset_mode = new Enumerated<>(Sequence.class, "exit_alt_charset_mode", "rmacs", "ae", "End alternate character set");
  public static Capability<Sequence> exit_am_mode = new Enumerated<>(Sequence.class, "exit_am_mode", "rmam", "RA", "Turn off automatic margins");
  public static Capability<Sequence> exit_attribute_mode = new Enumerated<>(Sequence.class, "exit_attribute_mode", "sgr0", "me", "Turn off all attributes");
  public static Capability<Sequence> exit_ca_mode = new Enumerated<>(Sequence.class, "exit_ca_mode", "rmcup", "te", "String to end programs that use cup");
  public static Capability<Sequence> exit_delete_mode = new Enumerated<>(Sequence.class, "exit_delete_mode", "rmdc", "ed", "End delete mode");
  public static Capability<Sequence> exit_doublewide_mode = new Enumerated<>(Sequence.class, "exit_doublewide_mode", "rwidm", "ZQ", "Disable double wide printing");
  public static Capability<Sequence> exit_insert_mode = new Enumerated<>(Sequence.class, "exit_insert_mode", "rmir", "ei", "End insert mode");
  public static Capability<Sequence> exit_italics_mode = new Enumerated<>(Sequence.class, "exit_italics_mode", "ritm", "ZR", "Disable italics");
  public static Capability<Sequence> exit_leftward_mode = new Enumerated<>(Sequence.class, "exit_leftward_mode", "rlm", "ZS", "Enable rightward (normal) carriage motion");
  public static Capability<Sequence> exit_micro_mode = new Enumerated<>(Sequence.class, "exit_micro_mode", "rmicm", "ZT", "Disable micro motion capabilities");
  public static Capability<Sequence> exit_pc_charset_mode = new Enumerated<>(Sequence.class, "exit_pc_charset_mode", "rmpch", "S3", "Disable PC character display mode");
  public static Capability<Sequence> exit_scancode_mode = new Enumerated<>(Sequence.class, "exit_scancode_mode", "rmsc", "S5", "Disable PC scancode mode");
  public static Capability<Sequence> exit_shadow_mode = new Enumerated<>(Sequence.class, "exit_shadow_mode", "rshm", "ZU", "Disable shadow printing");
  public static Capability<Sequence> exit_standout_mode = new Enumerated<>(Sequence.class, "exit_standout_mode", "rmso", "se", "End standout mode");
  public static Capability<Sequence> exit_subscript_mode = new Enumerated<>(Sequence.class, "exit_subscript_mode", "rsubm", "ZV", "Disable subscript printing");
  public static Capability<Sequence> exit_superscript_mode = new Enumerated<>(Sequence.class, "exit_superscript_mode", "rsupm", "ZW", "Disable superscript printing");
  public static Capability<Sequence> exit_underline_mode = new Enumerated<>(Sequence.class, "exit_underline_mode", "rmul", "ue", "End underscore mode");
  public static Capability<Sequence> exit_upward_mode = new Enumerated<>(Sequence.class, "exit_upward_mode", "rum", "ZX", "Enable downward (normal) carriage motion");
  public static Capability<Sequence> exit_xon_mode = new Enumerated<>(Sequence.class, "exit_xon_mode", "rmxon", "RX", "Turn off xon/xoff handshaking");
  public static Capability<Sequence> fixed_pause = new Enumerated<>(Sequence.class, "fixed_pause", "pause", "PA", "Pause for 2-3 seconds");
  public static Capability<Sequence> flash_hook = new Enumerated<>(Sequence.class, "flash_hook", "hook", "fh", "Flash the switch hook");
  public static Capability<Sequence> flash_screen = new Enumerated<>(Sequence.class, "flash_screen", "flash", "vb", "Visible bell (may move cursor)");
  public static Capability<Sequence> form_feed = new Enumerated<>(Sequence.class, "form_feed", "ff", "ff", "Hardcopy terminal page eject");
  public static Capability<Sequence> from_status_line = new Enumerated<>(Sequence.class, "from_status_line", "fsl", "fs", "Return from status line");
  public static Capability<Sequence> get_mouse = new Enumerated<>(Sequence.class, "get_mouse", "getm", "Gm", "Curses should get button events");
  public static Capability<Sequence> goto_window = new Enumerated<>(Sequence.class, "goto_window", "wingo", "WG", "Go to window #1");
  public static Capability<Sequence> hangup = new Enumerated<>(Sequence.class, "hangup", "hup", "HU", "Hang-up phone");
  public static Capability<Sequence> init_1string = new Enumerated<>(Sequence.class, "init_1string", "is1", "i1", "Terminal or printer initialisation string");
  public static Capability<Sequence> init_2string = new Enumerated<>(Sequence.class, "init_2string", "is2", "is", "Terminal or printer initialisation string");
  public static Capability<Sequence> init_3string = new Enumerated<>(Sequence.class, "init_3string", "is3", "i3", "Terminal or printer initialisation string");
  public static Capability<Sequence> init_file = new Enumerated<>(Sequence.class, "init_file", "if", "if", "Name of initialisation file");
  public static Capability<Sequence> init_prog = new Enumerated<>(Sequence.class, "init_prog", "iprog", "iP", "Path name of program for initialisation");
  public static Capability<Sequence> initialize_color = new Enumerated<>(Sequence.class, "initialize_color", "initc", "IC", "\"Set colour #1 to RGB #2");
  public static Capability<Sequence> initialize_pair = new Enumerated<>(Sequence.class, "initialize_pair", "initp", "Ip", "\"Set colour-pair #1 to fg #2");
  public static Capability<Sequence> insert_character = new Enumerated<>(Sequence.class, "insert_character", "ich1", "ic", "Insert character");
  public static Capability<Sequence> insert_line = new Enumerated<>(Sequence.class, "insert_line", "il1", "al", "Add new blank line");
  public static Capability<Sequence> insert_padding = new Enumerated<>(Sequence.class, "insert_padding", "ip", "ip", "Insert pad after character inserted");
  public static Capability<Sequence> key_a1 = new Enumerated<>(Sequence.class, "key_a1", "ka1", "K1", "upper left of keypad");
  public static Capability<Sequence> key_a3 = new Enumerated<>(Sequence.class, "key_a3", "ka3", "K3", "upper right of keypad");
  public static Capability<Sequence> key_b2 = new Enumerated<>(Sequence.class, "key_b2", "kb2", "K2", "center of keypad");
  public static Capability<Sequence> key_backspace = new Enumerated<>(Sequence.class, "key_backspace", "kbs", "kb", "sent by backspace key");
  public static Capability<Sequence> key_beg = new Enumerated<>(Sequence.class, "key_beg", "kbeg", "", "1");
  public static Capability<Sequence> key_btab = new Enumerated<>(Sequence.class, "key_btab", "kcbt", "kB", "sent by back-tab key");
  public static Capability<Sequence> key_c1 = new Enumerated<>(Sequence.class, "key_c1", "kc1", "K4", "lower left of keypad");
  public static Capability<Sequence> key_c3 = new Enumerated<>(Sequence.class, "key_c3", "kc3", "K5", "lower right of keypad");
  public static Capability<Sequence> key_cancel = new Enumerated<>(Sequence.class, "key_cancel", "kcan", "", "2");
  public static Capability<Sequence> key_catab = new Enumerated<>(Sequence.class, "key_catab", "ktbc", "ka", "sent by clear-all-tabs key");
  public static Capability<Sequence> key_clear = new Enumerated<>(Sequence.class, "key_clear", "kclr", "kC", "sent by clear-screen or erase key");
  public static Capability<Sequence> key_close = new Enumerated<>(Sequence.class, "key_close", "kclo", "", "3");
  public static Capability<Sequence> key_command = new Enumerated<>(Sequence.class, "key_command", "kcmd", "", "4");
  public static Capability<Sequence> key_copy = new Enumerated<>(Sequence.class, "key_copy", "kcpy", "", "5");
  public static Capability<Sequence> key_create = new Enumerated<>(Sequence.class, "key_create", "kcrt", "", "6");
  public static Capability<Sequence> key_ctab = new Enumerated<>(Sequence.class, "key_ctab", "kctab", "kt", "sent by clear-tab key");
  public static Capability<Sequence> key_dc = new Enumerated<>(Sequence.class, "key_dc", "kdch1", "kD", "sent by delete-character key");
  public static Capability<Sequence> key_dl = new Enumerated<>(Sequence.class, "key_dl", "kdl1", "kL", "sent by delete-line key");
  public static Capability<Sequence> key_down = new Enumerated<>(Sequence.class, "key_down", "kcud1", "kd", "sent by terminal down-arrow key");
  public static Capability<Sequence> key_eic = new Enumerated<>(Sequence.class, "key_eic", "krmir", "kM", "sent by rmir or smir in insert mode");
  public static Capability<Sequence> key_end = new Enumerated<>(Sequence.class, "key_end", "kend", "", "7");
  public static Capability<Sequence> key_enter = new Enumerated<>(Sequence.class, "key_enter", "kent", "", "8");
  public static Capability<Sequence> key_eol = new Enumerated<>(Sequence.class, "key_eol", "kel", "kE", "sent by clear-to-end-of-line key");
  public static Capability<Sequence> key_eos = new Enumerated<>(Sequence.class, "key_eos", "ked", "kS", "sent by clear-to-end-of-screen key");
  public static Capability<Sequence> key_exit = new Enumerated<>(Sequence.class, "key_exit", "kext", "", "9");
  public static Capability<Sequence> key_f0 = new Enumerated<>(Sequence.class, "key_f0", "kf0", "k0", "sent by function key f0");
  public static Capability<Sequence> key_f1 = new Enumerated<>(Sequence.class, "key_f1", "kf1", "k1", "sent by function key f1");
  public static Capability<Sequence> key_f2 = new Enumerated<>(Sequence.class, "key_f2", "kf2", "k2", "sent by function key f2");
  public static Capability<Sequence> key_f3 = new Enumerated<>(Sequence.class, "key_f3", "kf3", "k3", "sent by function key f3");
  public static Capability<Sequence> key_f4 = new Enumerated<>(Sequence.class, "key_f4", "kf4", "k4", "sent by function key f4");
  public static Capability<Sequence> key_f5 = new Enumerated<>(Sequence.class, "key_f5", "kf5", "k5", "sent by function key f5");
  public static Capability<Sequence> key_f6 = new Enumerated<>(Sequence.class, "key_f6", "kf6", "k6", "sent by function key f6");
  public static Capability<Sequence> key_f7 = new Enumerated<>(Sequence.class, "key_f7", "kf7", "k7", "sent by function key f7");
  public static Capability<Sequence> key_f8 = new Enumerated<>(Sequence.class, "key_f8", "kf8", "k8", "sent by function key f8");
  public static Capability<Sequence> key_f9 = new Enumerated<>(Sequence.class, "key_f9", "kf9", "k9", "sent by function key f9");
  public static Capability<Sequence> key_f10 = new Enumerated<>(Sequence.class, "key_f10", "kf10", "k10", "sent by function key f10");
  public static Capability<Sequence> key_f11 = new Enumerated<>(Sequence.class, "key_f11", "kf11", "k11", "sent by function key f11");
  public static Capability<Sequence> key_f12 = new Enumerated<>(Sequence.class, "key_f12", "kf12", "k12", "sent by function key f12");
  public static Capability<Sequence> key_f13 = new Enumerated<>(Sequence.class, "key_f13", "kf13", "k13", "sent by function key f13");
  public static Capability<Sequence> key_f14 = new Enumerated<>(Sequence.class, "key_f14", "kf14", "k14", "sent by function key f14");
  public static Capability<Sequence> key_f15 = new Enumerated<>(Sequence.class, "key_f15", "kf15", "k15", "sent by function key f15");
  public static Capability<Sequence> key_f16 = new Enumerated<>(Sequence.class, "key_f16", "kf16", "k16", "sent by function key f16");
  public static Capability<Sequence> key_f17 = new Enumerated<>(Sequence.class, "key_f17", "kf17", "k17", "sent by function key f17");
  public static Capability<Sequence> key_f18 = new Enumerated<>(Sequence.class, "key_f18", "kf18", "k18", "sent by function key f18");
  public static Capability<Sequence> key_f19 = new Enumerated<>(Sequence.class, "key_f19", "kf19", "k19", "sent by function key f19");
  public static Capability<Sequence> key_f20 = new Enumerated<>(Sequence.class, "key_f20", "kf20", "k20", "sent by function key f20");
  public static Capability<Sequence> key_f21 = new Enumerated<>(Sequence.class, "key_f21", "kf21", "k21", "sent by function key f21");
  public static Capability<Sequence> key_f22 = new Enumerated<>(Sequence.class, "key_f22", "kf22", "k22", "sent by function key f22");
  public static Capability<Sequence> key_f23 = new Enumerated<>(Sequence.class, "key_f23", "kf23", "k23", "sent by function key f23");
  public static Capability<Sequence> key_f24 = new Enumerated<>(Sequence.class, "key_f24", "kf24", "k24", "sent by function key f24");
  public static Capability<Sequence> key_f25 = new Enumerated<>(Sequence.class, "key_f25", "kf25", "k25", "sent by function key f25");
  public static Capability<Sequence> key_f26 = new Enumerated<>(Sequence.class, "key_f26", "kf26", "k26", "sent by function key f26");
  public static Capability<Sequence> key_f27 = new Enumerated<>(Sequence.class, "key_f27", "kf27", "k27", "sent by function key f27");
  public static Capability<Sequence> key_f28 = new Enumerated<>(Sequence.class, "key_f28", "kf28", "k28", "sent by function key f28");
  public static Capability<Sequence> key_f29 = new Enumerated<>(Sequence.class, "key_f29", "kf29", "k29", "sent by function key f29");
  public static Capability<Sequence> key_f30 = new Enumerated<>(Sequence.class, "key_f30", "kf30", "k30", "sent by function key f30");
  public static Capability<Sequence> key_f31 = new Enumerated<>(Sequence.class, "key_f31", "kf31", "k31", "sent by function key f31");
  public static Capability<Sequence> key_f32 = new Enumerated<>(Sequence.class, "key_f32", "kf32", "k33", "sent by function key f32");
  public static Capability<Sequence> key_f33 = new Enumerated<>(Sequence.class, "key_f33", "kf33", "k33", "sent by function key f33");
  public static Capability<Sequence> key_f34 = new Enumerated<>(Sequence.class, "key_f34", "kf34", "k34", "sent by function key f34");
  public static Capability<Sequence> key_f35 = new Enumerated<>(Sequence.class, "key_f35", "kf35", "k35", "sent by function key f35");
  public static Capability<Sequence> key_f36 = new Enumerated<>(Sequence.class, "key_f36", "kf36", "k36", "sent by function key f36");
  public static Capability<Sequence> key_f37 = new Enumerated<>(Sequence.class, "key_f37", "kf37", "k37", "sent by function key f37");
  public static Capability<Sequence> key_f38 = new Enumerated<>(Sequence.class, "key_f38", "kf38", "k38", "sent by function key f38");
  public static Capability<Sequence> key_f39 = new Enumerated<>(Sequence.class, "key_f39", "kf39", "k39", "sent by function key f39");
  public static Capability<Sequence> key_f40 = new Enumerated<>(Sequence.class, "key_f40", "kf40", "k40", "sent by function key f40");
  public static Capability<Sequence> key_f41 = new Enumerated<>(Sequence.class, "key_f41", "kf41", "k41", "sent by function key f41");
  public static Capability<Sequence> key_f42 = new Enumerated<>(Sequence.class, "key_f42", "kf42", "k42", "sent by function key f42");
  public static Capability<Sequence> key_f43 = new Enumerated<>(Sequence.class, "key_f43", "kf43", "k43", "sent by function key f43");
  public static Capability<Sequence> key_f44 = new Enumerated<>(Sequence.class, "key_f44", "kf44", "k44", "sent by function key f44");
  public static Capability<Sequence> key_f45 = new Enumerated<>(Sequence.class, "key_f45", "kf45", "k45", "sent by function key f45");
  public static Capability<Sequence> key_f46 = new Enumerated<>(Sequence.class, "key_f46", "kf46", "k46", "sent by function key f46");
  public static Capability<Sequence> key_f47 = new Enumerated<>(Sequence.class, "key_f47", "kf47", "k47", "sent by function key f47");
  public static Capability<Sequence> key_f48 = new Enumerated<>(Sequence.class, "key_f48", "kf48", "k48", "sent by function key f48");
  public static Capability<Sequence> key_f49 = new Enumerated<>(Sequence.class, "key_f49", "kf49", "k49", "sent by function key f49");
  public static Capability<Sequence> key_f50 = new Enumerated<>(Sequence.class, "key_f50", "kf50", "k50", "sent by function key f50");
  public static Capability<Sequence> key_f51 = new Enumerated<>(Sequence.class, "key_f51", "kf51", "k51", "sent by function key f51");
  public static Capability<Sequence> key_f52 = new Enumerated<>(Sequence.class, "key_f52", "kf52", "k52", "sent by function key f52");
  public static Capability<Sequence> key_f53 = new Enumerated<>(Sequence.class, "key_f53", "kf53", "k53", "sent by function key f53");
  public static Capability<Sequence> key_f54 = new Enumerated<>(Sequence.class, "key_f54", "kf54", "k54", "sent by function key f54");
  public static Capability<Sequence> key_f55 = new Enumerated<>(Sequence.class, "key_f55", "kf55", "k55", "sent by function key f55");
  public static Capability<Sequence> key_f56 = new Enumerated<>(Sequence.class, "key_f56", "kf56", "k56", "sent by function key f56");
  public static Capability<Sequence> key_f57 = new Enumerated<>(Sequence.class, "key_f57", "kf57", "k57", "sent by function key f57");
  public static Capability<Sequence> key_f58 = new Enumerated<>(Sequence.class, "key_f58", "kf58", "k58", "sent by function key f58");
  public static Capability<Sequence> key_f59 = new Enumerated<>(Sequence.class, "key_f59", "kf59", "k59", "sent by function key f59");
  public static Capability<Sequence> key_f60 = new Enumerated<>(Sequence.class, "key_f60", "kf60", "k60", "sent by function key f60");
  public static Capability<Sequence> key_f61 = new Enumerated<>(Sequence.class, "key_f61", "kf61", "k61", "sent by function key f61");
  public static Capability<Sequence> key_f62 = new Enumerated<>(Sequence.class, "key_f62", "kf62", "k62", "sent by function key f62");
  public static Capability<Sequence> key_f63 = new Enumerated<>(Sequence.class, "key_f63", "kf63", "k63", "sent by function key f63");
  public static Capability<Sequence> key_find = new Enumerated<>(Sequence.class, "key_find", "kfnd", "", "0");
  public static Capability<Sequence> key_help = new Enumerated<>(Sequence.class, "key_help", "khlp", "%1", "sent by help key");
  public static Capability<Sequence> key_home = new Enumerated<>(Sequence.class, "key_home", "khome", "kh", "sent by home key");
  public static Capability<Sequence> key_ic = new Enumerated<>(Sequence.class, "key_ic", "kich1", "kI", "sent by ins-char/enter ins-mode key");
  public static Capability<Sequence> key_il = new Enumerated<>(Sequence.class, "key_il", "kil1", "kA", "sent by insert-line key");
  public static Capability<Sequence> key_left = new Enumerated<>(Sequence.class, "key_left", "kcub1", "kl", "sent by terminal left-arrow key");
  public static Capability<Sequence> key_ll = new Enumerated<>(Sequence.class, "key_ll", "kll", "kH", "sent by home-down key");
  public static Capability<Sequence> key_mark = new Enumerated<>(Sequence.class, "key_mark", "kmrk", "%2", "sent by mark key");
  public static Capability<Sequence> key_message = new Enumerated<>(Sequence.class, "key_message", "kmsg", "%3", "sent by message key");
  public static Capability<Sequence> key_mouse = new Enumerated<>(Sequence.class, "key_mouse", "kmous", "Km", "\"0631");
  public static Capability<Sequence> key_move = new Enumerated<>(Sequence.class, "key_move", "kmov", "%4", "sent by move key");
  public static Capability<Sequence> key_next = new Enumerated<>(Sequence.class, "key_next", "knxt", "%5", "sent by next-object key");
  public static Capability<Sequence> key_npage = new Enumerated<>(Sequence.class, "key_npage", "knp", "kN", "sent by next-page key");
  public static Capability<Sequence> key_open = new Enumerated<>(Sequence.class, "key_open", "kopn", "%6", "sent by open key");
  public static Capability<Sequence> key_options = new Enumerated<>(Sequence.class, "key_options", "kopt", "%7", "sent by options key");
  public static Capability<Sequence> key_ppage = new Enumerated<>(Sequence.class, "key_ppage", "kpp", "kP", "sent by previous-page key");
  public static Capability<Sequence> key_previous = new Enumerated<>(Sequence.class, "key_previous", "kprv", "%8", "sent by previous-object key");
  public static Capability<Sequence> key_print = new Enumerated<>(Sequence.class, "key_print", "kprt", "%9", "sent by print or copy key");
  public static Capability<Sequence> key_redo = new Enumerated<>(Sequence.class, "key_redo", "krdo", "%0", "sent by redo key");
  public static Capability<Sequence> key_reference = new Enumerated<>(Sequence.class, "key_reference", "kref", "&1", "sent by ref(erence) key");
  public static Capability<Sequence> key_refresh = new Enumerated<>(Sequence.class, "key_refresh", "krfr", "&2", "sent by refresh key");
  public static Capability<Sequence> key_replace = new Enumerated<>(Sequence.class, "key_replace", "krpl", "&3", "sent by replace key");
  public static Capability<Sequence> key_restart = new Enumerated<>(Sequence.class, "key_restart", "krst", "&4", "sent by restart key");
  public static Capability<Sequence> key_resume = new Enumerated<>(Sequence.class, "key_resume", "kres", "&5", "sent by resume key");
  public static Capability<Sequence> key_right = new Enumerated<>(Sequence.class, "key_right", "kcuf1", "kr", "sent by terminal right-arrow key");
  public static Capability<Sequence> key_save = new Enumerated<>(Sequence.class, "key_save", "ksav", "&6", "sent by save key");
  public static Capability<Sequence> key_sbeg = new Enumerated<>(Sequence.class, "key_sbeg", "kBEG", "&9", "sent by shifted beginning key");
  public static Capability<Sequence> key_scancel = new Enumerated<>(Sequence.class, "key_scancel", "kCAN", "&0", "sent by shifted cancel key");
  public static Capability<Sequence> key_scommand = new Enumerated<>(Sequence.class, "key_scommand", "kCMD", "*1", "sent by shifted command key");
  public static Capability<Sequence> key_scopy = new Enumerated<>(Sequence.class, "key_scopy", "kCPY", "*2", "sent by shifted copy key");
  public static Capability<Sequence> key_screate = new Enumerated<>(Sequence.class, "key_screate", "kCRT", "*3", "sent by shifted create key");
  public static Capability<Sequence> key_sdc = new Enumerated<>(Sequence.class, "key_sdc", "kDC", "*4", "sent by shifted delete-char key");
  public static Capability<Sequence> key_sdl = new Enumerated<>(Sequence.class, "key_sdl", "kDL", "*5", "sent by shifted delete-line key");
  public static Capability<Sequence> key_select = new Enumerated<>(Sequence.class, "key_select", "kslt", "*6", "sent by select key");
  public static Capability<Sequence> key_send = new Enumerated<>(Sequence.class, "key_send", "kEND", "*7", "sent by shifted end key");
  public static Capability<Sequence> key_seol = new Enumerated<>(Sequence.class, "key_seol", "kEOL", "*8", "sent by shifted clear-line key");
  public static Capability<Sequence> key_sexit = new Enumerated<>(Sequence.class, "key_sexit", "kEXT", "*9", "sent by shifted exit key");
  public static Capability<Sequence> key_sf = new Enumerated<>(Sequence.class, "key_sf", "kind", "kF", "sent by scroll-forward/down key");
  public static Capability<Sequence> key_sfind = new Enumerated<>(Sequence.class, "key_sfind", "kFND", "*0", "sent by shifted find key");
  public static Capability<Sequence> key_shelp = new Enumerated<>(Sequence.class, "key_shelp", "kHLP", "#1", "sent by shifted help key");
  public static Capability<Sequence> key_shome = new Enumerated<>(Sequence.class, "key_shome", "kHOM", "#2", "sent by shifted home key");
  public static Capability<Sequence> key_sic = new Enumerated<>(Sequence.class, "key_sic", "kIC", "#3", "sent by shifted input key");
  public static Capability<Sequence> key_sleft = new Enumerated<>(Sequence.class, "key_sleft", "kLFT", "#4", "sent by shifted left-arrow key");
  public static Capability<Sequence> key_smessage = new Enumerated<>(Sequence.class, "key_smessage", "kMSG", "%a", "sent by shifted message key");
  public static Capability<Sequence> key_smove = new Enumerated<>(Sequence.class, "key_smove", "kMOV", "%b", "sent by shifted move key");
  public static Capability<Sequence> key_snext = new Enumerated<>(Sequence.class, "key_snext", "kNXT", "%c", "sent by shifted next key");
  public static Capability<Sequence> key_soptions = new Enumerated<>(Sequence.class, "key_soptions", "kOPT", "%d", "sent by shifted options key");
  public static Capability<Sequence> key_sprevious = new Enumerated<>(Sequence.class, "key_sprevious", "kPRV", "%e", "sent by shifted prev key");
  public static Capability<Sequence> key_sprint = new Enumerated<>(Sequence.class, "key_sprint", "kPRT", "%f", "sent by shifted print key");
  public static Capability<Sequence> key_sr = new Enumerated<>(Sequence.class, "key_sr", "kri", "kR", "sent by scroll-backward/up key");
  public static Capability<Sequence> key_sredo = new Enumerated<>(Sequence.class, "key_sredo", "kRDO", "%g", "sent by shifted redo key");
  public static Capability<Sequence> key_sreplace = new Enumerated<>(Sequence.class, "key_sreplace", "kRPL", "%h", "sent by shifted replace key");
  public static Capability<Sequence> key_sright = new Enumerated<>(Sequence.class, "key_sright", "kRIT", "%i", "sent by shifted right-arrow key");
  public static Capability<Sequence> key_srsume = new Enumerated<>(Sequence.class, "key_srsume", "kRES", "%j", "sent by shifted resume key");
  public static Capability<Sequence> key_ssave = new Enumerated<>(Sequence.class, "key_ssave", "kSAV", "!1", "sent by shifted save key");
  public static Capability<Sequence> key_ssuspend = new Enumerated<>(Sequence.class, "key_ssuspend", "kSPD", "!2", "sent by shifted suspend key");
  public static Capability<Sequence> key_stab = new Enumerated<>(Sequence.class, "key_stab", "khts", "kT", "sent by set-tab key");
  public static Capability<Sequence> key_sundo = new Enumerated<>(Sequence.class, "key_sundo", "kUND", "!3", "sent by shifted undo key");
  public static Capability<Sequence> key_suspend = new Enumerated<>(Sequence.class, "key_suspend", "kspd", "&7", "sent by suspend key");
  public static Capability<Sequence> key_undo = new Enumerated<>(Sequence.class, "key_undo", "kund", "&8", "sent by undo key");
  public static Capability<Sequence> key_up = new Enumerated<>(Sequence.class, "key_up", "kcuu1", "ku", "sent by terminal up-arrow key");
  public static Capability<Sequence> keypad_local = new Enumerated<>(Sequence.class, "keypad_local", "rmkx", "ke", "\"Out of \"\"keypad-transmit\"\" mode\"");
  public static Capability<Sequence> keypad_xmit = new Enumerated<>(Sequence.class, "keypad_xmit", "smkx", "ks", "\"Put terminal in \"\"keypad-transmit\"\" mode\"");
  public static Capability<Sequence> lab_f0 = new Enumerated<>(Sequence.class, "lab_f0", "lf0", "l0", "Labels on function key f0 if not f0");
  public static Capability<Sequence> lab_f1 = new Enumerated<>(Sequence.class, "lab_f1", "lf1", "l1", "Labels on function key f1 if not f1");
  public static Capability<Sequence> lab_f2 = new Enumerated<>(Sequence.class, "lab_f2", "lf2", "l2", "Labels on function key f2 if not f2");
  public static Capability<Sequence> lab_f3 = new Enumerated<>(Sequence.class, "lab_f3", "lf3", "l3", "Labels on function key f3 if not f3");
  public static Capability<Sequence> lab_f4 = new Enumerated<>(Sequence.class, "lab_f4", "lf4", "l4", "Labels on function key f4 if not f4");
  public static Capability<Sequence> lab_f5 = new Enumerated<>(Sequence.class, "lab_f5", "lf5", "l5", "Labels on function key f5 if not f5");
  public static Capability<Sequence> lab_f6 = new Enumerated<>(Sequence.class, "lab_f6", "lf6", "l6", "Labels on function key f6 if not f6");
  public static Capability<Sequence> lab_f7 = new Enumerated<>(Sequence.class, "lab_f7", "lf7", "l7", "Labels on function key f7 if not f7");
  public static Capability<Sequence> lab_f8 = new Enumerated<>(Sequence.class, "lab_f8", "lf8", "l8", "Labels on function key f8 if not f8");
  public static Capability<Sequence> lab_f9 = new Enumerated<>(Sequence.class, "lab_f9", "lf9", "l9", "Labels on function key f9 if not f9");
  public static Capability<Sequence> lab_f10 = new Enumerated<>(Sequence.class, "lab_f10", "lf10", "la", "Labels on function key f10 if not f10");
  public static Capability<Sequence> label_format = new Enumerated<>(Sequence.class, "label_format", "fln", "Lf", "Label format");
  public static Capability<Sequence> label_off = new Enumerated<>(Sequence.class, "label_off", "rmln", "LF", "Turn off soft labels");
  public static Capability<Sequence> label_on = new Enumerated<>(Sequence.class, "label_on", "smln", "LO", "Turn on soft labels");
  public static Capability<Sequence> meta_off = new Enumerated<>(Sequence.class, "meta_off", "rmm", "mo", "\"Turn off \"\"meta mode\"\"\"");
  public static Capability<Sequence> meta_on = new Enumerated<>(Sequence.class, "meta_on", "smm", "mm", "\"Turn on \"\"meta mode\"\" (8th bit)\"");
  public static Capability<Sequence> micro_column_address = new Enumerated<>(Sequence.class, "micro_column_address", "mhpa", "ZY", "Like column_address for micro adjustment");
  public static Capability<Sequence> micro_down = new Enumerated<>(Sequence.class, "micro_down", "mcud1", "ZZ", "Like cursor_down for micro adjustment");
  public static Capability<Sequence> micro_left = new Enumerated<>(Sequence.class, "micro_left", "mcub1", "Za", "Like cursor_left for micro adjustment");
  public static Capability<Sequence> micro_right = new Enumerated<>(Sequence.class, "micro_right", "mcuf1", "Zb", "Like cursor_right for micro adjustment");
  public static Capability<Sequence> micro_row_address = new Enumerated<>(Sequence.class, "micro_row_address", "mvpa", "Zc", "Like row_address for micro adjustment");
  public static Capability<Sequence> micro_up = new Enumerated<>(Sequence.class, "micro_up", "mcuu1", "Zd", "Like cursor_up for micro adjustment");
  public static Capability<Sequence> mouse_info = new Enumerated<>(Sequence.class, "mouse_info", "minfo", "Mi", "Mouse status information");
  public static Capability<Sequence> newline = new Enumerated<>(Sequence.class, "newline", "nel", "nw", "Newline (behaves like cr followed by lf)");
  public static Capability<Sequence> order_of_pins = new Enumerated<>(Sequence.class, "order_of_pins", "porder", "Ze", "Matches software bits to print-head pins");
  public static Capability<Sequence> orig_colors = new Enumerated<>(Sequence.class, "orig_colors", "oc", "oc", "Set all colour(-pair)s to the original ones");
  public static Capability<Sequence> orig_pair = new Enumerated<>(Sequence.class, "orig_pair", "op", "op", "Set default colour-pair to the original one");
  public static Capability<Sequence> pad_char = new Enumerated<>(Sequence.class, "pad_char", "pad", "pc", "Pad character (rather than null)");
  public static Capability<Sequence> parm_dch = new Enumerated<>(Sequence.class, "parm_dch", "dch", "DC", "Delete #1 chars");
  public static Capability<Sequence> parm_delete_line = new Enumerated<>(Sequence.class, "parm_delete_line", "dl", "DL", "Delete #1 lines");
  public static Capability<Sequence> parm_down_cursor = new Enumerated<>(Sequence.class, "parm_down_cursor", "cud", "DO", "Move down #1 lines.");
  public static Capability<Sequence> parm_down_micro = new Enumerated<>(Sequence.class, "parm_down_micro", "mcud", "Zf", "Like parm_down_cursor for micro adjust.");
  public static Capability<Sequence> parm_ich = new Enumerated<>(Sequence.class, "parm_ich", "ich", "IC", "Insert #1 blank chars");
  public static Capability<Sequence> parm_index = new Enumerated<>(Sequence.class, "parm_index", "indn", "SF", "Scroll forward #1 lines.");
  public static Capability<Sequence> parm_insert_line = new Enumerated<>(Sequence.class, "parm_insert_line", "il", "AL", "Add #1 new blank lines");
  public static Capability<Sequence> parm_left_cursor = new Enumerated<>(Sequence.class, "parm_left_cursor", "cub", "LE", "Move cursor left #1 spaces");
  public static Capability<Sequence> parm_left_micro = new Enumerated<>(Sequence.class, "parm_left_micro", "mcub", "Zg", "Like parm_left_cursor for micro adjust.");
  public static Capability<Sequence> parm_right_cursor = new Enumerated<>(Sequence.class, "parm_right_cursor", "cuf", "RI", "Move right #1 spaces.");
  public static Capability<Sequence> parm_right_micro = new Enumerated<>(Sequence.class, "parm_right_micro", "mcuf", "Zh", "Like parm_right_cursor for micro adjust.");
  public static Capability<Sequence> parm_rindex = new Enumerated<>(Sequence.class, "parm_rindex", "rin", "SR", "Scroll backward #1 lines.");
  public static Capability<Sequence> parm_up_cursor = new Enumerated<>(Sequence.class, "parm_up_cursor", "cuu", "UP", "Move cursor up #1 lines.");
  public static Capability<Sequence> parm_up_micro = new Enumerated<>(Sequence.class, "parm_up_micro", "mcuu", "Zi", "Like parm_up_cursor for micro adjust.");
  public static Capability<Sequence> pc_term_options = new Enumerated<>(Sequence.class, "pc_term_options", "pctrm", "S6", "PC terminal options");
  public static Capability<Sequence> pkey_key = new Enumerated<>(Sequence.class, "pkey_key", "pfkey", "pk", "Prog funct key #1 to type string #2");
  public static Capability<Sequence> pkey_local = new Enumerated<>(Sequence.class, "pkey_local", "pfloc", "pl", "Prog funct key #1 to execute string #2");
  public static Capability<Sequence> pkey_plab = new Enumerated<>(Sequence.class, "pkey_plab", "pfxl", "xl", "Prog key #1 to xmit string #2 and show string #3");
  public static Capability<Sequence> pkey_xmit = new Enumerated<>(Sequence.class, "pkey_xmit", "pfx", "px", "Prog funct key #1 to xmit string #2");
  public static Capability<Sequence> plab_norm = new Enumerated<>(Sequence.class, "plab_norm", "pln", "pn", "Prog label #1 to show string #2");
  public static Capability<Sequence> print_screen = new Enumerated<>(Sequence.class, "print_screen", "mc0", "ps", "Print contents of the screen");
  public static Capability<Sequence> prtr_non = new Enumerated<>(Sequence.class, "prtr_non", "mc5p", "pO", "Turn on the printer for #1 bytes");
  public static Capability<Sequence> prtr_off = new Enumerated<>(Sequence.class, "prtr_off", "mc4", "pf", "Turn off the printer");
  public static Capability<Sequence> prtr_on = new Enumerated<>(Sequence.class, "prtr_on", "mc5", "po", "Turn on the printer");
  public static Capability<Sequence> pulse = new Enumerated<>(Sequence.class, "pulse", "pulse", "PU", "Select pulse dialing");
  public static Capability<Sequence> quick_dial = new Enumerated<>(Sequence.class, "quick_dial", "qdial", "QD", "\"Dial phone number #1");
  public static Capability<Sequence> remove_clock = new Enumerated<>(Sequence.class, "remove_clock", "rmclk", "RC", "Remove time-of-day clock");
  public static Capability<Sequence> repeat_char = new Enumerated<>(Sequence.class, "repeat_char", "rep", "rp", "Repeat char #1 #2 times");
  public static Capability<Sequence> req_for_input = new Enumerated<>(Sequence.class, "req_for_input", "rfi", "RF", "Send next input char (for ptys)");
  public static Capability<Sequence> req_mouse_pos = new Enumerated<>(Sequence.class, "req_mouse_pos", "reqmp", "RQ", "Request mouse position report");
  public static Capability<Sequence> reset_1string = new Enumerated<>(Sequence.class, "reset_1string", "rs1", "r1", "Reset terminal completely to sane modes");
  public static Capability<Sequence> reset_2string = new Enumerated<>(Sequence.class, "reset_2string", "rs2", "r2", "Reset terminal completely to sane modes");
  public static Capability<Sequence> reset_3string = new Enumerated<>(Sequence.class, "reset_3string", "rs3", "r3", "Reset terminal completely to sane modes");
  public static Capability<Sequence> reset_file = new Enumerated<>(Sequence.class, "reset_file", "rf", "rf", "Name of file containing reset string");
  public static Capability<Sequence> restore_cursor = new Enumerated<>(Sequence.class, "restore_cursor", "rc", "rc", "Restore cursor to position of last sc");
  public static Capability<Sequence> row_address = new Enumerated<>(Sequence.class, "row_address", "vpa", "cv", "Set vertical position to absolute #1");
  public static Capability<Sequence> save_cursor = new Enumerated<>(Sequence.class, "save_cursor", "sc", "sc", "Save cursor position");
  public static Capability<Sequence> scancode_escape = new Enumerated<>(Sequence.class, "scancode_escape", "scesc", "S7", "Escape for scancode emulation");
  public static Capability<Sequence> scroll_forward = new Enumerated<>(Sequence.class, "scroll_forward", "ind", "sf", "Scroll text up");
  public static Capability<Sequence> scroll_reverse = new Enumerated<>(Sequence.class, "scroll_reverse", "ri", "sr", "Scroll text down");
  public static Capability<Sequence> select_char_set = new Enumerated<>(Sequence.class, "select_char_set", "scs", "Zj", "Select character set");
  public static Capability<Sequence> set0_des_seq = new Enumerated<>(Sequence.class, "set0_des_seq", "s0ds", "s0", "\"Shift into codeset 0 (EUC set 0");
  public static Capability<Sequence> set1_des_seq = new Enumerated<>(Sequence.class, "set1_des_seq", "s1ds", "s1", "Shift into codeset 1");
  public static Capability<Sequence> set2_des_seq = new Enumerated<>(Sequence.class, "set2_des_seq", "s2ds", "s2", "Shift into codeset 2");
  public static Capability<Sequence> set3_des_seq = new Enumerated<>(Sequence.class, "set3_des_seq", "s3ds", "s3", "Shift into codeset 3");
  public static Capability<Sequence> set_a_attributes = new Enumerated<>(Sequence.class, "set_a_attributes", "sgr1", "", "Define second set of video attributes #1-#6");
  public static Capability<Sequence> set_a_background = new Enumerated<>(Sequence.class, "set_a_background", "setab", "AB", "Set background colour to #1 using ANSI escape");
  public static Capability<Sequence> set_a_foreground = new Enumerated<>(Sequence.class, "set_a_foreground", "setaf", "AF", "Set foreground colour to #1 using ANSI escape");
  public static Capability<Sequence> set_attributes = new Enumerated<>(Sequence.class, "set_attributes", "sgr", "sa", "Define first set of video attributes #1-#9");
  public static Capability<Sequence> set_background = new Enumerated<>(Sequence.class, "set_background", "setb", "Sb", "Set background colour to #1");
  public static Capability<Sequence> set_bottom_margin = new Enumerated<>(Sequence.class, "set_bottom_margin", "smgb", "Zk", "Set bottom margin at current line");
  public static Capability<Sequence> set_bottom_margin_parm = new Enumerated<>(Sequence.class, "set_bottom_margin_parm", "smgbp", "Zl", "Set bottom margin at line #1 or #2 lines from bottom");
  public static Capability<Sequence> set_clock = new Enumerated<>(Sequence.class, "set_clock", "sclk", "SC", "\"Set clock to hours (#1)");
  public static Capability<Sequence> set_color_band = new Enumerated<>(Sequence.class, "set_color_band", "setcolor", "Yz", "Change to ribbon colour #1");
  public static Capability<Sequence> set_color_pair = new Enumerated<>(Sequence.class, "set_color_pair", "scp", "sp", "Set current colour pair to #1");
  public static Capability<Sequence> set_foreground = new Enumerated<>(Sequence.class, "set_foreground", "setf", "Sf", "Set foreground colour to #1");
  public static Capability<Sequence> set_left_margin = new Enumerated<>(Sequence.class, "set_left_margin", "smgl", "ML", "Set left margin at current column");
  public static Capability<Sequence> set_left_margin_parm = new Enumerated<>(Sequence.class, "set_left_margin_parm", "smglp", "Zm", "Set left (right) margin at column #1 (#2)");
  public static Capability<Sequence> set_lr_margin = new Enumerated<>(Sequence.class, "set_lr_margin", "smglr", "ML", "Sets both left and right margins");
  public static Capability<Sequence> set_page_length = new Enumerated<>(Sequence.class, "set_page_length", "slines", "YZ", "Set page length to #1 lines");
  public static Capability<Sequence> set_pglen_inch = new Enumerated<>(Sequence.class, "set_pglen_inch", "slength", "YI", "Set page length to #1 hundredth of an inch");
  public static Capability<Sequence> set_right_margin = new Enumerated<>(Sequence.class, "set_right_margin", "smgr", "MR", "Set right margin at current column");
  public static Capability<Sequence> set_right_margin_parm = new Enumerated<>(Sequence.class, "set_right_margin_parm", "smgrp", "Zn", "Set right margin at column #1");
  public static Capability<Sequence> set_tab = new Enumerated<>(Sequence.class, "set_tab", "hts", "st", "\"Set a tab in all rows");
  public static Capability<Sequence> set_tb_margin = new Enumerated<>(Sequence.class, "set_tb_margin", "smgtb", "MT", "Sets both top and bottom margins");
  public static Capability<Sequence> set_top_margin = new Enumerated<>(Sequence.class, "set_top_margin", "smgt", "Zo", "Set top margin at current line");
  public static Capability<Sequence> set_top_margin_parm = new Enumerated<>(Sequence.class, "set_top_margin_parm", "smgtp", "Zp", "Set top (bottom) margin at line #1 (#2)");
  public static Capability<Sequence> set_window = new Enumerated<>(Sequence.class, "set_window", "wind", "wi", "Current window is lines #1-#2 cols #3-#4");
  public static Capability<Sequence> start_bit_image = new Enumerated<>(Sequence.class, "start_bit_image", "sbim", "Zq", "Start printing bit image graphics");
  public static Capability<Sequence> start_char_set_def = new Enumerated<>(Sequence.class, "start_char_set_def", "scsd", "Zr", "Start definition of a character set");
  public static Capability<Sequence> stop_bit_image = new Enumerated<>(Sequence.class, "stop_bit_image", "rbim", "Zs", "End printing bit image graphics");
  public static Capability<Sequence> stop_char_set_def = new Enumerated<>(Sequence.class, "stop_char_set_def", "rcsd", "Zt", "End definition of a character set");
  public static Capability<Sequence> subscript_characters = new Enumerated<>(Sequence.class, "subscript_characters", "subcs", "Zu", "\"List of \"\"subscript-able\"\" characters\"");
  public static Capability<Sequence> superscript_characters = new Enumerated<>(Sequence.class, "superscript_characters", "supcs", "Zv", "\"List of \"\"superscript-able\"\" characters\"");
  public static Capability<Sequence> tab = new Enumerated<>(Sequence.class, "tab", "ht", "ta", "Tab to next 8-space hardware tab stop");
  public static Capability<Sequence> these_cause_cr = new Enumerated<>(Sequence.class, "these_cause_cr", "docr", "Zw", "Printing any of these chars causes cr");
  public static Capability<Sequence> to_status_line = new Enumerated<>(Sequence.class, "to_status_line", "tsl", "ts", "\"Go to status line");
  public static Capability<Sequence> tone = new Enumerated<>(Sequence.class, "tone", "tone", "TO", "Select touch tone dialing");
  public static Capability<Sequence> user0 = new Enumerated<>(Sequence.class, "user0", "u0", "u0", "User string 0");
  public static Capability<Sequence> user1 = new Enumerated<>(Sequence.class, "user1", "u1", "u1", "User string 1");
  public static Capability<Sequence> user2 = new Enumerated<>(Sequence.class, "user2", "u2", "u2", "User string 2");
  public static Capability<Sequence> user3 = new Enumerated<>(Sequence.class, "user3", "u3", "u3", "User string 3");
  public static Capability<Sequence> user4 = new Enumerated<>(Sequence.class, "user4", "u4", "u4", "User string 4");
  public static Capability<Sequence> user5 = new Enumerated<>(Sequence.class, "user5", "u5", "u5", "User string 5");
  public static Capability<Sequence> user6 = new Enumerated<>(Sequence.class, "user6", "u6", "u6", "User string 6");
  public static Capability<Sequence> user7 = new Enumerated<>(Sequence.class, "user7", "u7", "u7", "User string 7");
  public static Capability<Sequence> user8 = new Enumerated<>(Sequence.class, "user8", "u8", "u8", "User string 8");
  public static Capability<Sequence> user9 = new Enumerated<>(Sequence.class, "user9", "u9", "u9", "User string 9");
  public static Capability<Sequence> underline_char = new Enumerated<>(Sequence.class, "underline_char", "uc", "uc", "Underscore one char and move past it");
  public static Capability<Sequence> up_half_line = new Enumerated<>(Sequence.class, "up_half_line", "hu", "hu", "Half-line up (reverse 1/2 linefeed)");
  public static Capability<Sequence> wait_tone = new Enumerated<>(Sequence.class, "wait_tone", "wait", "WA", "Wait for dial tone");
  public static Capability<Sequence> xoff_character = new Enumerated<>(Sequence.class, "xoff_character", "xoffc", "XF", "X-off character");
  public static Capability<Sequence> xon_character = new Enumerated<>(Sequence.class, "xon_character", "xonc", "XN", "X-on character");
  public static Capability<Sequence> zero_motion = new Enumerated<>(Sequence.class, "zero_motion", "zerom", "Zx", "No motion for the subsequent character");

  public final Class<T> type;
  public final String variable;
  public final String name;
  public final String cap;
  public final String description;

  public Capability(Class<T> type, String variable, String name, String cap, String description) {
    if (type == null) {
      throw new NullPointerException("No null capability type accepted");
    }
    if (name == null) {
      throw new NullPointerException("No null capability name accepted");
    }
    this.type = type;
    this.variable = variable;
    this.name = name;
    this.cap = cap;
    this.description = description;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof Capability<?>) {
      Capability<?> that = (Capability<?>) obj;
      return type.equals(that.type) && name.equals(that.name);
    }
    return false;
  }
}
