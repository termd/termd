package io.termd.core.tput;

import java.util.ArrayList;
import java.util.List;

/**
* @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
*/
public class TermInfoEntry {

  final String name;
  final List<String> aliases;
  final List<TermInfoFeature> features;

  public TermInfoEntry(String name, List<String> aliases) {
    this.name = name;
    this.aliases = aliases;
    this.features = new ArrayList<>();
  }

}
