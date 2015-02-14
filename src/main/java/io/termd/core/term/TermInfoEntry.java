package io.termd.core.term;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
*/
public class TermInfoEntry {

  final String name;
  final List<String> aliases;
  final Map<Capability<?>, Object> features;

  public TermInfoEntry(String name, List<String> aliases) {
    this.name = name;
    this.aliases = aliases;
    this.features = new HashMap<>();
  }

  public <T> T getFeature(Capability<T> capability) {
    return getFeature(capability, null);
  }

  public <T> T getFeature(Capability<T> capability, T def) {
    Object value = features.get(capability);
    if (value != null) {
      return capability.type.cast(value);
    }
    return def;
  }

  public void addFeature(Feature<?> feature) {
    features.put(feature.capability, feature);
  }

  public void addFeature(String name, Object value) {
    addFeature(Feature.create(name, value));
  }
}
